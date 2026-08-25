package com.example.collab;

import com.example.collab.domain.*;
import com.example.collab.dto.project.MemberAddRequest;
import com.example.collab.dto.project.MemberRoleUpdateRequest;
import com.example.collab.dto.project.ProjectUpdateRequest;
import com.example.collab.dto.task.TaskCreateRequest;
import com.example.collab.dto.task.TaskUpdateRequest;
import com.example.collab.exception.CollabException;
import com.example.collab.exception.ErrorCode;
import com.example.collab.repository.ProjectMemberRepository;
import com.example.collab.repository.ProjectRepository;
import com.example.collab.repository.TaskRepository;
import com.example.collab.repository.UserRepository;
import com.example.collab.service.ProjectService;
import com.example.collab.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 역할별 권한 규칙과 프로젝트 간 데이터 격리를 검증한다.
 * 역할 3종 × 기능 8종의 분기가 촘촘해 눈으로는 구멍을 놓치기 쉬운 부분이다.
 *
 * 준비된 데이터:
 *   프로젝트 A — owner(OWNER), admin(ADMIN), member(MEMBER), 작업 하나(담당자 member)
 *   프로젝트 B — outsider(OWNER), 작업 하나(담당자 없음)   ← A와 완전히 분리
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("권한 규칙")
class PermissionTest {

	@Autowired ProjectService projectService;
	@Autowired TaskService taskService;
	@Autowired UserRepository userRepository;
	@Autowired ProjectRepository projectRepository;
	@Autowired ProjectMemberRepository projectMemberRepository;
	@Autowired TaskRepository taskRepository;

	/** 벌크 연산 뒤 1차 캐시를 비워 DB의 실제 값을 다시 읽기 위해 쓴다. */
	@Autowired EntityManager entityManager;

	private Long ownerId, adminId, memberId, outsiderId;
	private Long projectA, projectB;
	private Long taskInA, taskInB;

	@BeforeEach
	void setUp() {
		User owner = userRepository.save(User.create("김오너", "owner@example.com"));
		User admin = userRepository.save(User.create("이관리", "admin@example.com"));
		User member = userRepository.save(User.create("박멤버", "member@example.com"));
		User outsider = userRepository.save(User.create("정외부", "outsider@example.com"));

		Project a = projectRepository.save(Project.create("협업 서비스 개편", null));
		Project b = projectRepository.save(Project.create("사내 위키 이전", null));

		projectMemberRepository.save(ProjectMember.create(a, owner, Role.OWNER));
		projectMemberRepository.save(ProjectMember.create(a, admin, Role.ADMIN));
		projectMemberRepository.save(ProjectMember.create(a, member, Role.MEMBER));
		projectMemberRepository.save(ProjectMember.create(b, outsider, Role.OWNER));

		taskInA = taskRepository.save(Task.create(a, "A의 작업", null, member)).getId();
		taskInB = taskRepository.save(Task.create(b, "B의 작업", null, null)).getId();

		ownerId = owner.getId();
		adminId = admin.getId();
		memberId = member.getId();
		outsiderId = outsider.getId();
		projectA = a.getId();
		projectB = b.getId();
	}

	/** 예외가 기대한 ErrorCode로 발생하는지 확인한다. 검증문이 반복되어 메서드로 뺀다. */
	private void assertFails(ThrowingCallable action, ErrorCode expected) {
		assertThatThrownBy(action)
				.isInstanceOf(CollabException.class)
				.extracting(e -> ((CollabException) e).getErrorCode())
				.isEqualTo(expected);
	}


	@Nested
	@DisplayName("비멤버 차단")
	class NonMember {

		@Test
		@DisplayName("비멤버는 프로젝트를 조회조차 할 수 없다")
		void 비멤버는_조회_불가() {
			assertFails(() -> projectService.get(outsiderId, projectA), ErrorCode.NOT_PROJECT_MEMBER);
			assertFails(() -> projectService.findMembers(outsiderId, projectA), ErrorCode.NOT_PROJECT_MEMBER);
			assertFails(() -> taskService.get(outsiderId, projectA, taskInA), ErrorCode.NOT_PROJECT_MEMBER);
			assertFails(() -> taskService.search(outsiderId, projectA, null, null, null, false,
					PageRequest.of(0, 20)), ErrorCode.NOT_PROJECT_MEMBER);
		}

		@Test
		@DisplayName("존재하지 않는 프로젝트도 비멤버와 같은 응답을 준다")
		void 없는_프로젝트도_같은_응답() {
			// 404와 403을 구분하면 id를 넣어보는 것만으로 프로젝트의 존재를 알아낼 수 있다
			assertFails(() -> projectService.get(ownerId, 99999L), ErrorCode.NOT_PROJECT_MEMBER);
		}

		@Test
		@DisplayName("내 프로젝트 목록에는 내가 속한 것만 나온다")
		void 내_프로젝트만_조회된다() {
			assertThat(projectService.findMyProjects(ownerId))
					.extracting("id").containsExactly(projectA);
			assertThat(projectService.findMyProjects(outsiderId))
					.extracting("id").containsExactly(projectB);
		}
	}

	@Nested
	@DisplayName("역할별 허용·차단")
	class RoleMatrix {

		@Test
		@DisplayName("프로젝트 수정은 OWNER와 ADMIN만 가능하다")
		void 프로젝트_수정() {
			ProjectUpdateRequest request = new ProjectUpdateRequest("바뀐 이름", null);

			assertThatCode(() -> projectService.update(ownerId, projectA, request)).doesNotThrowAnyException();
			assertThatCode(() -> projectService.update(adminId, projectA, request)).doesNotThrowAnyException();
			assertFails(() -> projectService.update(memberId, projectA, request), ErrorCode.NO_PERMISSION);
		}

		@Test
		@DisplayName("프로젝트 삭제는 OWNER만 가능하다")
		void 프로젝트_삭제() {
			assertFails(() -> projectService.delete(adminId, projectA), ErrorCode.NO_PERMISSION);
			assertFails(() -> projectService.delete(memberId, projectA), ErrorCode.NO_PERMISSION);
			assertThatCode(() -> projectService.delete(ownerId, projectA)).doesNotThrowAnyException();
		}

		@Test
		@DisplayName("멤버 추가는 OWNER와 ADMIN만 가능하다")
		void 멤버_추가() {
			assertFails(() -> projectService.addMember(memberId, projectA,
					new MemberAddRequest(outsiderId, Role.MEMBER)), ErrorCode.NO_PERMISSION);

			assertThatCode(() -> projectService.addMember(adminId, projectA,
					new MemberAddRequest(outsiderId, Role.MEMBER))).doesNotThrowAnyException();
		}

		@Test
		@DisplayName("작업 생성은 멤버 전원이 가능하다")
		void 작업_생성() {
			TaskCreateRequest request = new TaskCreateRequest("새 작업", null, null);

			assertThatCode(() -> taskService.create(memberId, projectA, request)).doesNotThrowAnyException();
			assertFails(() -> taskService.create(outsiderId, projectA, request), ErrorCode.NOT_PROJECT_MEMBER);
		}
	}

	@Nested
	@DisplayName("OWNER 임명·해임은 OWNER만")
	class OwnerOnlyRoleChange {

		@Test
		@DisplayName("ADMIN은 자기 자신을 OWNER로 올릴 수 없다")
		void 어드민_자기승격_차단() {
			// 이것이 뚫리면 ADMIN이 언제든 OWNER가 될 수 있어 두 역할을 나눈 의미가 사라진다
			assertFails(() -> projectService.changeRole(adminId, projectA, adminId,
					new MemberRoleUpdateRequest(Role.OWNER)), ErrorCode.NO_PERMISSION);
		}

		@Test
		@DisplayName("ADMIN은 OWNER를 강등하거나 제거할 수 없다")
		void 어드민의_오너_해임_차단() {
			assertFails(() -> projectService.changeRole(adminId, projectA, ownerId,
					new MemberRoleUpdateRequest(Role.MEMBER)), ErrorCode.NO_PERMISSION);
			assertFails(() -> projectService.removeMember(adminId, projectA, ownerId),
					ErrorCode.NO_PERMISSION);
		}

		@Test
		@DisplayName("ADMIN은 MEMBER와 ADMIN 사이 변경은 할 수 있다")
		void 어드민의_일반_역할변경_허용() {
			assertThatCode(() -> projectService.changeRole(adminId, projectA, memberId,
					new MemberRoleUpdateRequest(Role.ADMIN))).doesNotThrowAnyException();
		}

		@Test
		@DisplayName("OWNER는 다른 멤버를 OWNER로 임명할 수 있다")
		void 오너의_임명_허용() {
			assertThatCode(() -> projectService.changeRole(ownerId, projectA, adminId,
					new MemberRoleUpdateRequest(Role.OWNER))).doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("OWNER 최소 1명 유지")
	class LastOwner {

		@Test
		@DisplayName("마지막 OWNER는 스스로도 강등할 수 없다")
		void 마지막_오너_강등_차단() {
			assertFails(() -> projectService.changeRole(ownerId, projectA, ownerId,
					new MemberRoleUpdateRequest(Role.MEMBER)), ErrorCode.LAST_OWNER);
		}

		@Test
		@DisplayName("마지막 OWNER는 제거할 수 없다")
		void 마지막_오너_제거_차단() {
			assertFails(() -> projectService.removeMember(ownerId, projectA, ownerId),
					ErrorCode.LAST_OWNER);
		}

		@Test
		@DisplayName("OWNER가 2명이면 한 명은 내려올 수 있다")
		void 오너가_둘이면_강등_가능() {
			projectService.changeRole(ownerId, projectA, adminId, new MemberRoleUpdateRequest(Role.OWNER));

			assertThatCode(() -> projectService.changeRole(ownerId, projectA, ownerId,
					new MemberRoleUpdateRequest(Role.MEMBER))).doesNotThrowAnyException();

			assertThat(projectMemberRepository.countByProjectIdAndRole(projectA, Role.OWNER)).isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("프로젝트 간 데이터 격리")
	class Isolation {

		@Test
		@DisplayName("다른 프로젝트의 작업은 상세 조회로 새어 나가지 않는다")
		void 다른_프로젝트_작업_상세_차단() {
			// A의 멤버가 A의 경로로 B의 작업 id를 넣어도 찾을 수 없어야 한다
			assertFails(() -> taskService.get(ownerId, projectA, taskInB), ErrorCode.TASK_NOT_FOUND);
		}

		@Test
		@DisplayName("다른 프로젝트의 작업은 목록에 섞이지 않는다")
		void 다른_프로젝트_작업_목록_차단() {
			assertThat(taskService.search(ownerId, projectA, null, null, null, false, PageRequest.of(0, 20)))
					.extracting("id").containsExactly(taskInA);
		}

		@Test
		@DisplayName("다른 프로젝트의 사용자는 담당자로 지정할 수 없다")
		void 비멤버는_담당자_불가() {
			assertFails(() -> taskService.create(ownerId, projectA,
					new TaskCreateRequest("새 작업", null, outsiderId)), ErrorCode.ASSIGNEE_NOT_MEMBER);
		}
	}

	@Nested
	@DisplayName("작업 수정·삭제 권한")
	class TaskEdit {

		@Test
		@DisplayName("담당자 본인은 자기 작업을 수정할 수 있다")
		void 담당자_본인_수정_가능() {
			Long version = taskService.get(memberId, projectA, taskInA).version();

			assertThatCode(() -> taskService.update(memberId, projectA, taskInA,
					new TaskUpdateRequest("담당자가 고침", null, TaskStatus.DONE, memberId, version)))
					.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("담당자가 아닌 MEMBER는 남의 작업을 수정할 수 없다")
		void 남의_작업_수정_차단() {
			// 담당자를 비운 작업을 만들고, MEMBER 역할인 사람이 고치려 하면 막혀야 한다
			Long otherTask = taskRepository.save(
					Task.create(projectRepository.findById(projectA).orElseThrow(),
							"담당자 없는 작업", null, null)).getId();
			Long version = taskService.get(memberId, projectA, otherTask).version();

			assertFails(() -> taskService.update(memberId, projectA, otherTask,
					new TaskUpdateRequest("남이 고침", null, TaskStatus.DONE, null, version)),
					ErrorCode.NO_PERMISSION);
		}

		@Test
		@DisplayName("OWNER와 ADMIN은 남의 작업도 수정할 수 있다")
		void 관리자는_남의_작업도_수정_가능() {
			Long version = taskService.get(ownerId, projectA, taskInA).version();

			assertThatCode(() -> taskService.update(adminId, projectA, taskInA,
					new TaskUpdateRequest("관리자가 고침", null, TaskStatus.DONE, memberId, version)))
					.doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("멤버 제거의 뒷정리")
	class RemoveMember {

		@Test
		@DisplayName("제거된 멤버가 담당하던 작업은 삭제되지 않고 담당자만 비워진다")
		void 담당자만_비워진다() {
			projectService.removeMember(ownerId, projectA, memberId);

			// 담당자를 비우는 쿼리는 벌크 UPDATE라 1차 캐시에 옛 값이 남는다.
			// 운영에서는 멤버 제거 요청이 Task를 읽지 않아 생기지 않고, 테스트가 호출과 검증을
			// 같은 트랜잭션으로 묶어서 드러나는 상황이다.
			entityManager.clear();

			Task task = taskRepository.findById(taskInA).orElseThrow();
			assertThat(task.getAssignee()).isNull();
		}
	}
}
