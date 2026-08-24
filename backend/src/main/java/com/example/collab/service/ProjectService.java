package com.example.collab.service;

import com.example.collab.domain.Project;
import com.example.collab.domain.ProjectMember;
import com.example.collab.domain.Role;
import com.example.collab.domain.User;
import com.example.collab.dto.project.*;
import com.example.collab.exception.CollabException;
import com.example.collab.exception.ErrorCode;
import com.example.collab.repository.ProjectMemberRepository;
import com.example.collab.repository.ProjectRepository;
import com.example.collab.repository.TaskRepository;
import com.example.collab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 프로젝트 CRUD와 멤버 관리의 비즈니스 로직을 담는 계층.
 *
 * 권한 판정이 여기서 이뤄진다. 컨트롤러는 요청자 식별자를 넘겨주기만 하고,
 * "이 사람이 이 프로젝트에서 무엇을 할 수 있는가"는 이 계층이
 * ProjectMemberRepository로 역할을 조회해 결정한다.
 *
 * 지켜야 할 규칙 세 가지:
 * - 프로젝트에는 항상 최소 1명의 OWNER가 남아야 한다.
 *   (마지막 OWNER의 역할 변경이나 제거를 막아야 한다)
 * - 프로젝트 생성자는 자동으로 OWNER 멤버가 된다.
 * - OWNER를 임명하거나 해임하는 일은 OWNER만 할 수 있다.
 *   ADMIN은 MEMBER와 ADMIN 사이만 다룬다. 이 제한이 없으면 ADMIN이
 *   자기 자신을 OWNER로 바꿀 수 있어 두 역할을 나눈 의미가 사라진다.
 *   (명세에 없는 제약이라 README에 근거를 남긴다)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;

	/** 프로젝트 생성 시 생성자를, 멤버 추가 시 대상자를 조회하는 데 쓴다. */
	private final UserRepository userRepository;

	/**
	 * 멤버를 제거할 때 그 사람이 담당하던 작업의 담당자를 비우는 데 쓴다.
	 *
	 * 프로젝트 삭제에는 쓰지 않는다. 딸린 작업은 @OnDelete(CASCADE)로
	 * DB가 지우므로 서비스가 관여할 일이 없다.
	 */
	private final TaskRepository taskRepository;

	// ================================================================
	// 권한 판정 — 아래 모든 메서드가 맨 처음 호출하는 두 가지
	// ================================================================

	/**
	 * 요청자가 그 프로젝트의 멤버인지 확인하고, 멤버 정보를 돌려준다.
	 *
	 * 돌려주는 ProjectMember에는 역할과 프로젝트가 함께 들어 있어,
	 * 호출한 쪽이 역할 검사와 프로젝트 접근에 그대로 쓴다.
	 * (역할을 알기 위해, 또 프로젝트를 얻기 위해 조회를 반복하지 않는다)
	 *
	 * 프로젝트가 존재하지 않는 경우도 같은 예외를 던진다. 404와 403을 구분하면
	 * 남의 프로젝트 id를 넣어보는 것만으로 그 존재를 알아낼 수 있기 때문이다.
	 *
	 * @throws CollabException 멤버가 아니거나 프로젝트가 없는 경우
	 */
	private ProjectMember requireMember(Long projectId, Long userId) {
		return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
				.orElseThrow(() -> new CollabException(ErrorCode.NOT_PROJECT_MEMBER));
	}

	/**
	 * 멤버의 역할이 허용 목록에 드는지 확인한다.
	 *
	 * 반드시 requireMember를 먼저 호출한 뒤에 쓴다. 순서가 뒤바뀌면
	 * 비멤버에게 "권한이 없다"는 응답이 나가면서 프로젝트의 존재가 드러난다.
	 *
	 * @param member  요청자의 멤버 정보
	 * @param allowed 허용할 역할들
	 * @throws CollabException 역할이 모자란 경우
	 */
	private void requireRole(ProjectMember member, Role... allowed) {
		if (!Arrays.asList(allowed).contains(member.getRole())) {
			throw new CollabException(ErrorCode.NO_PERMISSION);
		}
	}

	// ================================================================
	// 프로젝트
	// ================================================================

	/**
	 * 프로젝트를 만든다. 권한 검사가 없는 유일한 프로젝트 API다(누구나 만들 수 있다).
	 *
	 * 처리 순서:
	 * 1. 만든 사람이 실제로 존재하는 사용자인지 확인한다.
	 * 2. 프로젝트를 저장한다.
	 * 3. 만든 사람을 OWNER 멤버로 저장한다. "생성자가 OWNER가 된다"는 규칙이 여기서 구현된다.
	 *
	 * 2와 3이 한 트랜잭션이어야 하는 이유:
	 * 3이 실패하면 OWNER가 없는 프로젝트가 남는다. 아무도 수정·삭제·멤버 추가를
	 * 할 수 없는 상태라 되살릴 방법이 없다.
	 *
	 * @param userId  만드는 사람
	 * @param request 이름과 설명
	 * @return 만들어진 프로젝트 정보(myRole은 OWNER)
	 */
	@Transactional
	public ProjectResponse create(Long userId, ProjectCreateRequest request) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new CollabException(ErrorCode.USER_NOT_FOUND));

		Project project = projectRepository.save(
				Project.create(request.name(), request.description()));
		projectMemberRepository.save(ProjectMember.create(project, user, Role.OWNER));

		return ProjectResponse.from(project, Role.OWNER);
	}

	/**
	 * 내가 속한 프로젝트 목록을 돌려준다.
	 *
	 * 프로젝트 테이블을 훑고 걸러내는 방식이 아니라, 내 멤버 행에서 출발한다.
	 * 그래서 내가 속하지 않은 프로젝트는 애초에 결과에 들어올 수 없다.
	 *
	 * 각 프로젝트마다 내 역할을 함께 담아 돌려준다. 프론트가 수정·삭제 버튼을
	 * 보여줄지 판단하는 데 쓴다.
	 */
	public List<ProjectResponse> findMyProjects(Long userId) {
		return projectMemberRepository.findByUserIdWithProject(userId).stream()
				.map(member -> ProjectResponse.from(member.getProject(), member.getRole()))
				.toList();
	}

	/** 프로젝트 상세를 돌려준다. 그 프로젝트의 멤버라면 누구나 볼 수 있다. */
	public ProjectResponse get(Long userId, Long projectId) {
		ProjectMember member = requireMember(projectId, userId);
		return ProjectResponse.from(member.getProject(), member.getRole());
	}

	/**
	 * 프로젝트의 이름과 설명을 바꾼다. OWNER와 ADMIN만 할 수 있다.
	 *
	 * save()를 부르지 않는 이유:
	 * 트랜잭션 안에서 조회한 엔티티는 영속 상태라, 값을 바꾸면 커밋 시점에
	 * JPA가 변경을 감지해 UPDATE를 자동으로 내보낸다.
	 *
	 * 대신 flush()를 부르는 이유:
	 * updatedAt은 @UpdateTimestamp가 붙어 있어 애플리케이션이 채우는 값이 아니라
	 * 하이버네이트가 UPDATE 문을 내보내는 시점에 채우는 값이다. 그 시점은 기본적으로
	 * 트랜잭션이 끝날 때인데, 응답 DTO는 그보다 앞서 이 메서드 안에서 만들어진다.
	 * 그냥 두면 방금 수정했는데도 응답의 updatedAt이 수정 전 값으로 나간다.
	 * (DB에는 제대로 저장되므로 다음 조회부터는 맞다. 응답만 어긋난다)
	 * flush()로 UPDATE를 지금 내보내 값을 확정한 뒤 DTO를 만든다.
	 */
	@Transactional
	public ProjectResponse update(Long userId, Long projectId, ProjectUpdateRequest request) {
		ProjectMember member = requireMember(projectId, userId);
		requireRole(member, Role.OWNER, Role.ADMIN);

		Project project = member.getProject();
		project.update(request.name(), request.description());
		projectRepository.flush();

		return ProjectResponse.from(project, member.getRole());
	}

	/**
	 * 프로젝트를 삭제한다. OWNER만 할 수 있다(ADMIN은 불가).
	 *
	 * 딸린 작업과 멤버를 여기서 지우지 않는 이유:
	 * Task.project와 ProjectMember.project에 @OnDelete(CASCADE)를 붙여
	 * DB가 함께 지우도록 했다. 자식을 메모리로 읽어와 한 건씩 지우는 것보다
	 * DELETE 한 문장으로 끝나는 편이 낫다.
	 */
	@Transactional
	public void delete(Long userId, Long projectId) {
		ProjectMember member = requireMember(projectId, userId);
		requireRole(member, Role.OWNER);

		projectRepository.delete(member.getProject());
	}

	// ================================================================
	// 프로젝트 멤버
	// ================================================================

	/** 프로젝트의 멤버 목록을 돌려준다. 그 프로젝트의 멤버라면 누구나 볼 수 있다. */
	public List<MemberResponse> findMembers(Long userId, Long projectId) {
		requireMember(projectId, userId);

		return projectMemberRepository.findByProjectIdWithUser(projectId).stream()
				.map(MemberResponse::from)
				.toList();
	}

	/**
	 * 사용자를 프로젝트 멤버로 추가한다.
	 *
	 * 처리 순서:
	 * 1. 요청자가 멤버인지 확인한다.
	 * 2. 부여하려는 역할이 OWNER면 요청자도 OWNER여야 한다. 아니면 ADMIN까지 허용한다.
	 * 3. 추가할 사용자가 존재하는지 확인한다.
	 * 4. 이미 그 프로젝트의 멤버가 아닌지 확인한다.
	 * 5. 멤버로 저장한다.
	 *
	 * 4번을 통과해도 (project_id, user_id) UNIQUE 제약이 마지막 방어선으로 남는다.
	 *
	 * @throws CollabException 권한이 없거나, 사용자가 없거나, 이미 멤버인 경우
	 */
	@Transactional
	public MemberResponse addMember(Long userId, Long projectId, MemberAddRequest request) {
		ProjectMember member = requireMember(projectId, userId);

		if (request.role() == Role.OWNER) {
			requireRole(member, Role.OWNER);
		} else {
			requireRole(member, Role.OWNER, Role.ADMIN);
		}

		User target = userRepository.findById(request.userId())
				.orElseThrow(() -> new CollabException(ErrorCode.USER_NOT_FOUND));

		if (projectMemberRepository.existsByProjectIdAndUserId(projectId, request.userId())) {
			throw new CollabException(ErrorCode.ALREADY_MEMBER);
		}

		ProjectMember added = projectMemberRepository.save(
				ProjectMember.create(member.getProject(), target, request.role()));

		return MemberResponse.from(added);
	}

	/**
	 * 멤버의 역할을 바꾼다.
	 *
	 * 처리 순서:
	 * 1. 요청자가 멤버인지 확인한다.
	 * 2. 대상 멤버를 찾는다.
	 * 3. OWNER를 임명하거나 해임하는 변경이면 요청자가 OWNER여야 한다.
	 * 4. 마지막 OWNER를 다른 역할로 낮추려는 것이면 막는다.
	 * 5. 역할을 바꾼다.
	 *
	 * 2번이 3번보다 앞에 오는 이유:
	 * 대상의 현재 역할을 알아야 "OWNER 해임에 해당하는가"를 판단할 수 있다.
	 * 비멤버 차단(1번)은 그보다 앞에 있으므로 순서 원칙은 지켜진다.
	 *
	 * 4번에서 자기 자신인지 따로 보지 않는 이유:
	 * 마지막 OWNER가 스스로 역할을 낮추는 경우도 같은 검사에 걸린다.
	 *
	 * @throws CollabException 권한이 없거나, 대상이 멤버가 아니거나, 마지막 OWNER인 경우
	 */
	@Transactional
	public MemberResponse changeRole(Long userId, Long projectId, Long targetUserId,
	                                 MemberRoleUpdateRequest request) {
		ProjectMember member = requireMember(projectId, userId);
		ProjectMember target = requireMember(projectId, targetUserId);

		boolean ownerInvolved = request.role() == Role.OWNER || target.getRole() == Role.OWNER;
		if (ownerInvolved) {
			requireRole(member, Role.OWNER);
		} else {
			requireRole(member, Role.OWNER, Role.ADMIN);
		}

		if (target.getRole() == Role.OWNER && request.role() != Role.OWNER) {
			requireOtherOwnerExists(projectId);
		}

		target.changeRole(request.role());
		return MemberResponse.from(target);
	}

	/**
	 * 멤버를 프로젝트에서 제거한다.
	 *
	 * 처리 순서:
	 * 1. 요청자가 멤버인지 확인한다.
	 * 2. 대상 멤버를 찾는다.
	 * 3. 대상이 OWNER면 요청자도 OWNER여야 한다.
	 *    역할 변경만 막고 제거를 열어두면 ADMIN이 OWNER를 제거하는 우회로가 남는다.
	 * 4. 마지막 OWNER면 막는다.
	 * 5. 그 사람이 담당하던 작업의 담당자를 비운다.
	 *    작업 자체는 남겨 다른 멤버가 이어받을 수 있게 한다.
	 * 6. 멤버 행을 지운다.
	 *
	 * @throws CollabException 권한이 없거나, 대상이 멤버가 아니거나, 마지막 OWNER인 경우
	 */
	@Transactional
	public void removeMember(Long userId, Long projectId, Long targetUserId) {
		ProjectMember member = requireMember(projectId, userId);
		ProjectMember target = requireMember(projectId, targetUserId);

		if (target.getRole() == Role.OWNER) {
			requireRole(member, Role.OWNER);
			requireOtherOwnerExists(projectId);
		} else {
			requireRole(member, Role.OWNER, Role.ADMIN);
		}

		taskRepository.clearAssignee(projectId, targetUserId);
		projectMemberRepository.delete(target);
	}

	/**
	 * 그 프로젝트에 OWNER가 2명 이상인지 확인한다.
	 *
	 * OWNER를 강등하거나 제거하기 직전에 호출한다. 남은 OWNER가 1명뿐이면
	 * 이 요청이 마지막 OWNER를 없애는 것이므로 막는다.
	 *
	 * DB 제약으로 표현할 수 없어 서비스에서 세어 확인한다.
	 * ("특정 조건의 행이 최소 1건 남아야 한다"는 표준 SQL 제약에 없다)
	 */
	private void requireOtherOwnerExists(Long projectId) {
		if (projectMemberRepository.countByProjectIdAndRole(projectId, Role.OWNER) <= 1) {
			throw new CollabException(ErrorCode.LAST_OWNER);
		}
	}
}
