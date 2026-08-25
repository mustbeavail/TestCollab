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

import java.util.Objects;

/**
 * 작업 CRUD와 목록 조회. 권한이 프로젝트 기준이라 ProjectMemberRepository를 함께 주입받는다.
 *
 * 동시 수정 충돌은 두 겹으로 막는다.
 * - 서비스가 클라이언트의 version과 DB 값을 비교한다(화면을 보고 있던 동안의 변경).
 * - Task의 @Version이 커밋 시점에 검사한다(두 트랜잭션이 실제로 겹칠 때).
 *   이때 나는 예외는 GlobalExceptionHandler가 409로 바꾼다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

	private final TaskRepository taskRepository;
	private final ProjectMemberRepository projectMemberRepository;

	// ================================================================
	// 권한 판정
	// (ProjectService에도 같은 메서드가 있다. 공통 클래스로 빼면 계층이 하나 늘거나
	//  서비스가 서비스를 부르게 되는데, 그 대가가 메서드 두 개를 아끼는 것뿐이다)
	// ================================================================

	/** 프로젝트가 없는 경우도 같은 예외를 던져 프로젝트의 존재를 드러내지 않는다. */
	private ProjectMember requireMember(Long projectId, Long userId) {
		return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
				.orElseThrow(() -> new CollabException(ErrorCode.NOT_PROJECT_MEMBER));
	}

	/** 수정·삭제 조건: OWNER·ADMIN이거나 그 작업의 담당자 본인. 담당자는 없을 수 있다. */
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
	 * 담당자로 지목된 사용자를 멤버 조회로 찾는다(요청자를 보는 requireMember와 대상이 다르다).
	 * assignee_id는 users를 가리켜 아무 사용자 id나 넣어도 외래키는 통과하므로 여기서 막아야 한다.
	 */
	private User resolveAssignee(Long projectId, Long assigneeId) {
		return projectMemberRepository.findByProjectIdAndUserId(projectId, assigneeId)
				.orElseThrow(() -> new CollabException(ErrorCode.ASSIGNEE_NOT_MEMBER))
				.getUser();
	}

	// ================================================================
	// 작업
	// ================================================================

	/** 그 프로젝트의 멤버라면 역할과 무관하게 만들 수 있다. 상태는 Task.create()가 TODO로 고정한다. */
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

	/** 세 조건은 모두 선택 사항이라 null이면 무시된다. Page.map으로 페이징 정보를 유지한 채 DTO로 바꾼다. */
	public Page<TaskResponse> search(Long userId, Long projectId, String keyword, TaskStatus status,
	                                 Long assigneeId, boolean unassignedOnly, Pageable pageable) {
		requireMember(projectId, userId);

		return taskRepository
				.search(projectId, keyword, status, assigneeId, unassignedOnly, stableSort(pageable))
				.map(TaskResponse::from);
	}

	/**
	 * 정렬 맨 뒤에 id를 덧붙여 순서를 하나로 고정한다.
	 * 기본 정렬인 createdAt은 값이 같은 작업이 생기는데, 동률의 순서가 조회마다 달라지면
	 * 같은 작업이 두 페이지에 겹치거나 어느 페이지에도 안 나올 수 있다.
	 */
	private Pageable stableSort(Pageable pageable) {
		Sort sort = pageable.getSort();
		if (sort.getOrderFor("id") != null) return pageable;

		return PageRequest.of(
				pageable.getPageNumber(),
				pageable.getPageSize(),
				sort.and(Sort.by(Sort.Direction.DESC, "id")));
	}

	public TaskResponse get(Long userId, Long projectId, Long taskId) {
		requireMember(projectId, userId);
		return TaskResponse.from(findTask(projectId, taskId));
	}

	/**
	 * 작업을 수정한다.
	 * 권한 검사가 버전 비교보다 먼저다. 권한 없는 사람에게 버전이 맞는지 알려줄 이유가 없다.
	 * flush()가 필요한 이유: version은 UPDATE가 나갈 때 올라가므로, 그냥 두면 응답에 옛 version이
	 * 담기고 클라이언트가 그 값으로 다시 수정하면 곧바로 409가 난다.
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

	/** 버전을 비교하지 않는다. 어느 버전이든 결국 사라지므로 덮어쓰기 사고가 성립하지 않는다. */
	@Transactional
	public void delete(Long userId, Long projectId, Long taskId) {
		ProjectMember member = requireMember(projectId, userId);
		Task task = findTask(projectId, taskId);
		requireEditable(member, task);

		taskRepository.delete(task);
	}

	/** 다른 프로젝트의 작업 id를 넣으면 "없음"으로 응답한다. */
	private Task findTask(Long projectId, Long taskId) {
		return taskRepository.findByIdAndProjectId(taskId, projectId)
				.orElseThrow(() -> new CollabException(ErrorCode.TASK_NOT_FOUND));
	}
}
