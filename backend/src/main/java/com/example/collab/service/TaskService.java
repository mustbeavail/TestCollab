package com.example.collab.service;

import com.example.collab.domain.*;
import com.example.collab.dto.task.TaskCreateRequest;
import com.example.collab.dto.task.TaskResponse;
import com.example.collab.dto.task.TaskUpdateRequest;
import com.example.collab.exception.CollabException;
import com.example.collab.exception.ErrorCode;
import com.example.collab.repository.ProjectMemberRepository;
import com.example.collab.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Objects;

/**
 * 작업 CRUD와 목록 조회(검색·상태 필터·페이징)의 비즈니스 로직을 담는 계층.
 *
 * 작업 수정·삭제는 담당자 본인이거나 그 프로젝트의 OWNER·ADMIN만 가능하므로,
 * 작업이 속한 프로젝트에서 요청자의 역할을 조회해 판정한다.
 * 그래서 TaskRepository와 함께 ProjectMemberRepository를 주입받는다.
 *
 * 동시 수정 충돌은 두 겹으로 막는다.
 * - 첫째: 클라이언트가 보낸 version과 DB의 현재 version을 여기서 직접 비교한다.
 *         요청과 요청 사이에 벌어진 변경(사용자가 화면을 보고 있던 동안의 변경)을 잡는다.
 * - 둘째: Task의 @Version이 커밋 시점에 검사한다.
 *         두 요청의 트랜잭션이 실제로 겹칠 때를 잡는다. 이때 나는 예외는
 *         잡지 않고 올려보내면 GlobalExceptionHandler가 409로 바꾼다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

	private final TaskRepository taskRepository;
	private final ProjectMemberRepository projectMemberRepository;

	// ================================================================
	// 권한 판정
	//
	// ProjectService에도 같은 이름의 메서드가 있다. 중복을 공통 클래스로
	// 빼지 않는 이유는, 서비스가 서비스를 부르는 구조가 되거나 계층이
	// 하나 더 생기는데 그 대가가 메서드 두 개를 아끼는 것뿐이기 때문이다.
	// ================================================================

	/**
	 * 요청자가 그 프로젝트의 멤버인지 확인하고 멤버 정보를 돌려준다.
	 * 프로젝트가 없는 경우도 같은 예외로 응답해 프로젝트의 존재를 드러내지 않는다.
	 */
	private ProjectMember requireMember(Long projectId, Long userId) {
		return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
				.orElseThrow(() -> new CollabException(ErrorCode.NOT_PROJECT_MEMBER));
	}

	/** 멤버의 역할이 허용 목록에 드는지 확인한다. 반드시 requireMember 뒤에 호출한다. */
	private void requireRole(ProjectMember member, Role... allowed) {
		if (!Arrays.asList(allowed).contains(member.getRole())) {
			throw new CollabException(ErrorCode.NO_PERMISSION);
		}
	}

	/**
	 * 작업을 수정·삭제할 수 있는지 판정한다.
	 *
	 * 통과 조건은 둘 중 하나다.
	 * - 요청자가 그 프로젝트의 OWNER 또는 ADMIN이다.
	 * - 요청자가 그 작업의 담당자 본인이다.
	 *
	 * 담당자가 지정되지 않은 작업도 있으므로 null 검사를 먼저 한다.
	 *
	 * @throws CollabException 둘 다 아닌 경우
	 */
	private void requireEditable(ProjectMember member, Task task) {
		if (member.getRole() == Role.OWNER || member.getRole() == Role.ADMIN) {
			return;
		}
		boolean isAssignee = task.getAssignee() != null
				&& Objects.equals(task.getAssignee().getId(), member.getUser().getId());

		if (!isAssignee) {
			throw new CollabException(ErrorCode.NO_PERMISSION);
		}
	}

	/**
	 * 담당자로 지정하려는 사용자를 찾아 돌려준다.
	 *
	 * 요청으로 들어오는 것은 사용자 id 값 하나뿐이고, 그 사람이 이 프로젝트의
	 * 멤버인지는 알 수 없다. 외래키가 대신 막아주지도 못한다.
	 * task.assignee_id는 project_member가 아니라 users를 가리키므로
	 * 아무 사용자 id나 넣어도 외래키 제약은 통과한다.
	 * 그래서 여기서 project_member를 조회해 확인한다.
	 *
	 * 사용자 조회를 따로 하지 않는 이유:
	 * 멤버 행에 이미 User가 매달려 있어, 멤버 확인과 사용자 획득이 한 번에 끝난다.
	 *
	 * (요청자가 멤버인지 보는 requireMember와 대상이 다르다.
	 *  이쪽은 담당자로 지목된 사람을 본다. 그래서 조회가 두 번 나간다)
	 *
	 * @throws CollabException 그 사용자가 이 프로젝트의 멤버가 아닌 경우
	 */
	private User resolveAssignee(Long projectId, Long assigneeId) {
		return projectMemberRepository.findByProjectIdAndUserId(projectId, assigneeId)
				.orElseThrow(() -> new CollabException(ErrorCode.ASSIGNEE_NOT_MEMBER))
				.getUser();
	}

	// ================================================================
	// 작업
	// ================================================================

	/**
	 * 작업을 만든다. 그 프로젝트의 멤버라면 역할과 무관하게 누구나 만들 수 있다.
	 *
	 * 상태를 인자로 받지 않는 이유:
	 * 새 작업은 항상 TODO로 시작한다. Task.create()가 그 규칙을 강제한다.
	 *
	 * @param userId    요청자
	 * @param projectId 작업을 만들 프로젝트
	 * @param request   제목·설명·담당자(선택)
	 */
	@Transactional
	public TaskResponse create(Long userId, Long projectId, TaskCreateRequest request) {
		ProjectMember member = requireMember(projectId, userId);

		User assignee = request.assigneeId() == null
				? null
				: resolveAssignee(projectId, request.assigneeId());

		Task task = taskRepository.save(Task.create(
				member.getProject(), request.title(), request.description(), assignee));

		return TaskResponse.from(task);
	}

	/**
	 * 작업 목록을 검색·필터·페이징해서 돌려준다. 그 프로젝트의 멤버만 볼 수 있다.
	 *
	 * 세 조건(keyword, status, assigneeId)은 모두 선택 사항이라 null이면 무시된다.
	 *
	 * Page.map을 쓰는 이유:
	 * 전체 건수·페이지 번호 같은 페이징 정보를 유지한 채 내용만 DTO로 바꾼다.
	 * 리스트로 꺼내 변환하면 그 정보가 사라진다.
	 */
	public Page<TaskResponse> search(Long userId, Long projectId, String keyword,
	                                 TaskStatus status, Long assigneeId, Pageable pageable) {
		requireMember(projectId, userId);

		return taskRepository.search(projectId, keyword, status, assigneeId, stableSort(pageable))
				.map(TaskResponse::from);
	}

	/**
	 * 정렬 기준 맨 뒤에 id 내림차순을 덧붙여, 순서가 항상 하나로 정해지게 만든다.
	 *
	 * 왜 필요한가:
	 * 기본 정렬인 createdAt은 값이 같은 작업이 얼마든지 생긴다(한 번에 여러 건을 만들면
	 * 밀리초까지 같아진다). 정렬 기준이 그것뿐이면 동률인 행들의 순서를 DB가 마음대로 정하고,
	 * 그 순서는 조회할 때마다 달라질 수 있다.
	 * 페이징은 "정렬된 결과의 n번째부터 m개"를 잘라 오는 방식이라,
	 * 1페이지와 2페이지 사이에 순서가 바뀌면 같은 작업이 두 페이지에 겹쳐 나오거나
	 * 아예 어느 페이지에도 안 나오는 일이 생긴다.
	 * id는 중복되지 않으므로, 마지막 기준으로 두면 동률이 남지 않는다.
	 *
	 * 이미 id로 정렬하고 있으면(클라이언트가 sort=id로 요청한 경우) 그대로 둔다.
	 * 그때는 덧붙이지 않아도 순서가 하나로 정해진다.
	 *
	 * @param pageable 클라이언트가 요청한(또는 기본값이 채워진) 페이지 정보
	 * @return id 정렬이 보장된 페이지 정보
	 */
	private Pageable stableSort(Pageable pageable) {
		Sort sort = pageable.getSort();
		if (sort.getOrderFor("id") != null) return pageable;

		return PageRequest.of(
				pageable.getPageNumber(),
				pageable.getPageSize(),
				sort.and(Sort.by(Sort.Direction.DESC, "id")));
	}

	/** 작업 한 건을 돌려준다. 그 프로젝트의 멤버만 볼 수 있다. */
	public TaskResponse get(Long userId, Long projectId, Long taskId) {
		requireMember(projectId, userId);
		return TaskResponse.from(findTask(projectId, taskId));
	}

	/**
	 * 작업을 수정한다. 담당자 본인이거나 OWNER·ADMIN만 할 수 있다.
	 *
	 * 처리 순서:
	 * 1. 요청자가 멤버인지 확인한다.
	 * 2. 작업을 찾는다(프로젝트 조건을 함께 걸어 남의 작업이 나오지 않게 한다).
	 * 3. 수정할 수 있는 사람인지 판정한다.
	 * 4. 클라이언트가 보낸 version이 현재 값과 같은지 확인한다.
	 * 5. 담당자를 지정했다면 그 사람이 이 프로젝트의 멤버인지 확인한다.
	 * 6. 값을 바꾼다.
	 * 7. flush로 UPDATE를 지금 내보내 version과 updatedAt을 확정한다.
	 *
	 * 3번(권한)이 4번(버전)보다 먼저인 이유:
	 * 권한이 없는 사람에게는 버전이 맞는지 여부조차 알려줄 이유가 없다.
	 *
	 * 7번이 필요한 이유:
	 * version은 하이버네이트가 UPDATE를 내보낼 때 올리는 값이라, flush 전에
	 * DTO를 만들면 수정 전 version이 응답에 담긴다. 클라이언트가 그 값으로
	 * 다시 수정하면 곧바로 409가 나서 정상적인 수정이 막힌다.
	 *
	 * @throws CollabException 권한이 없거나, 작업이 없거나, 그 사이 다른 사용자가 먼저 수정한 경우
	 */
	@Transactional
	public TaskResponse update(Long userId, Long projectId, Long taskId, TaskUpdateRequest request) {
		ProjectMember member = requireMember(projectId, userId);
		Task task = findTask(projectId, taskId);
		requireEditable(member, task);

		if (!Objects.equals(task.getVersion(), request.version())) {
			throw new CollabException(ErrorCode.TASK_VERSION_CONFLICT);
		}

		User assignee = request.assigneeId() == null
				? null
				: resolveAssignee(projectId, request.assigneeId());

		task.update(request.title(), request.description(), request.status(), assignee);
		taskRepository.flush();

		return TaskResponse.from(task);
	}

	/**
	 * 작업을 삭제한다. 담당자 본인이거나 OWNER·ADMIN만 할 수 있다.
	 *
	 * 버전을 비교하지 않는 이유:
	 * 지우는 쪽은 "내용이 그 사이 바뀌었는가"가 결과에 영향을 주지 않는다.
	 * 어느 버전이든 결국 사라지므로 덮어쓰기 사고가 성립하지 않는다.
	 */
	@Transactional
	public void delete(Long userId, Long projectId, Long taskId) {
		ProjectMember member = requireMember(projectId, userId);
		Task task = findTask(projectId, taskId);
		requireEditable(member, task);

		taskRepository.delete(task);
	}

	/**
	 * 작업을 id와 프로젝트로 함께 찾는다.
	 *
	 * id만으로 찾지 않는 이유는 TaskRepository.findByIdAndProjectId 설명 참고.
	 * 다른 프로젝트의 작업 id를 넣으면 "없음"으로 응답한다.
	 */
	private Task findTask(Long projectId, Long taskId) {
		return taskRepository.findByIdAndProjectId(taskId, projectId)
				.orElseThrow(() -> new CollabException(ErrorCode.TASK_NOT_FOUND));
	}
}
