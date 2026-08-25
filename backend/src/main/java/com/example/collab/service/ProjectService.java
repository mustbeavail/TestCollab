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
 * 프로젝트 CRUD와 멤버 관리. 권한 판정이 전부 이 계층에서 이뤄진다.
 *
 * 지켜야 할 규칙:
 * - 프로젝트에는 항상 최소 1명의 OWNER가 남는다.
 * - 프로젝트 생성자는 자동으로 OWNER가 된다.
 * - OWNER 임명·해임은 OWNER만 한다. 명세에 없는 제약이지만, 열어두면
 *   ADMIN이 스스로를 OWNER로 올릴 수 있어 두 역할을 나눈 의미가 사라진다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final UserRepository userRepository;

	/** 멤버 제거 시 담당자를 비우는 데만 쓴다. 프로젝트 삭제의 연쇄 삭제는 DB가 한다. */
	private final TaskRepository taskRepository;

	// ================================================================
	// 권한 판정 — 아래 모든 메서드가 맨 처음 호출한다
	// ================================================================

	/**
	 * 요청자의 멤버 정보(역할·프로젝트 포함)를 돌려준다.
	 * 프로젝트가 없는 경우도 같은 예외를 던진다. 404와 구분하면 프로젝트의 존재가 드러난다.
	 */
	private ProjectMember requireMember(Long projectId, Long userId) {
		return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
				.orElseThrow(() -> new CollabException(ErrorCode.NOT_PROJECT_MEMBER));
	}

	/** 반드시 requireMember 뒤에 호출한다. 순서가 바뀌면 비멤버에게 프로젝트의 존재가 드러난다. */
	private void requireRole(ProjectMember member, Role... allowed) {
		if (!Arrays.asList(allowed).contains(member.getRole())) {
			throw new CollabException(ErrorCode.NO_PERMISSION);
		}
	}

	// ================================================================
	// 프로젝트
	// ================================================================

	/**
	 * 프로젝트를 만들고 생성자를 OWNER 멤버로 넣는다. 권한 검사가 없는 유일한 API다.
	 * 두 저장이 한 트랜잭션이어야 한다. 멤버 저장이 실패하면 아무도 손댈 수 없는 프로젝트가 남는다.
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

	/** 내 멤버 행에서 출발하므로 속하지 않은 프로젝트는 결과에 들어올 수 없다. */
	public List<ProjectResponse> findMyProjects(Long userId) {
		return projectMemberRepository.findByUserIdWithProject(userId).stream()
				.map(member -> ProjectResponse.from(member.getProject(), member.getRole()))
				.toList();
	}

	public ProjectResponse get(Long userId, Long projectId) {
		ProjectMember member = requireMember(projectId, userId);
		return ProjectResponse.from(member.getProject(), member.getRole());
	}

	/**
	 * 프로젝트를 수정한다. OWNER·ADMIN만 가능.
	 * flush()가 필요한 이유: updatedAt은 UPDATE가 나갈 때 채워지는데 기본 시점은 커밋이라,
	 * 그냥 두면 응답에 수정 전 시각이 담긴다(DB 값은 정상).
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

	/** OWNER만 가능. 딸린 작업·멤버는 @OnDelete(CASCADE)로 DB가 함께 지운다. */
	@Transactional
	public void delete(Long userId, Long projectId) {
		ProjectMember member = requireMember(projectId, userId);
		requireRole(member, Role.OWNER);

		projectRepository.delete(member.getProject());
	}

	// ================================================================
	// 프로젝트 멤버
	// ================================================================

	public List<MemberResponse> findMembers(Long userId, Long projectId) {
		requireMember(projectId, userId);

		return projectMemberRepository.findByProjectIdWithUser(projectId).stream()
				.map(MemberResponse::from)
				.toList();
	}

	/** OWNER 역할을 부여하는 것은 OWNER만 할 수 있다. 중복 추가는 유니크 제약이 마지막 방어선이다. */
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
	 * 역할을 바꾼다. 대상을 먼저 찾는 이유는 현재 역할을 알아야 "OWNER 해임인가"를 판단할 수 있어서다.
	 * 마지막 OWNER의 강등은 자기 자신이 하더라도 같은 검사에 걸린다.
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
	 * 멤버를 제거한다. OWNER 제거를 OWNER로 제한하지 않으면 ADMIN이 OWNER를 밀어내는 우회로가 남는다.
	 * 담당하던 작업은 지우지 않고 담당자만 비워 다른 멤버가 이어받게 한다.
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

	/** OWNER가 1명뿐이면 이 요청이 마지막 OWNER를 없애는 것이므로 막는다. */
	private void requireOtherOwnerExists(Long projectId) {
		if (projectMemberRepository.countByProjectIdAndRole(projectId, Role.OWNER) <= 1) {
			throw new CollabException(ErrorCode.LAST_OWNER);
		}
	}
}
