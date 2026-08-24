package com.example.collab;

import com.example.collab.domain.*;
import com.example.collab.dto.task.TaskUpdateRequest;
import com.example.collab.exception.CollabException;
import com.example.collab.exception.ErrorCode;
import com.example.collab.repository.ProjectMemberRepository;
import com.example.collab.repository.ProjectRepository;
import com.example.collab.repository.TaskRepository;
import com.example.collab.repository.UserRepository;
import com.example.collab.service.TaskService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 두 사용자가 같은 작업을 동시에 수정할 때, 나중 요청이 앞선 변경을 덮어쓰지 않는지 검증한다.
 *
 * 과제가 "이 상황을 어떻게 다룰지 결정하고 구현하라"고 명시한 항목이라 테스트를 둔다.
 * 손으로 재현하려면 두 사람이 타이밍을 맞춰야 하지만, 테스트로는 정확히 재현된다.
 *
 * 이 클래스에 @Transactional을 붙이지 않은 이유:
 * 붙이면 모든 작업이 하나의 트랜잭션 안에서 돌고 끝나면 롤백된다. 그러면
 * "먼저 커밋된 변경"이라는 상황 자체를 만들 수 없다. 여기서는 실제 커밋이 필요하다.
 * 대신 각 테스트 시작 시 데이터를 직접 정리한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("동시 수정 처리")
class TaskConcurrentUpdateTest {

	@Autowired TaskService taskService;
	@Autowired UserRepository userRepository;
	@Autowired ProjectRepository projectRepository;
	@Autowired ProjectMemberRepository projectMemberRepository;
	@Autowired TaskRepository taskRepository;
	@Autowired EntityManagerFactory entityManagerFactory;

	private Long ownerId;
	private Long projectId;
	private Long taskId;

	/**
	 * 각 테스트 전에 데이터를 비우고, OWNER 한 명과 프로젝트 하나, 작업 하나를 만든다.
	 *
	 * 삭제 순서가 작업 → 멤버 → 프로젝트 → 사용자인 이유는 외래키 때문이다.
	 * 참조하는 쪽부터 지워야 제약에 걸리지 않는다.
	 */
	@BeforeEach
	void setUp() {
		taskRepository.deleteAll();
		projectMemberRepository.deleteAll();
		projectRepository.deleteAll();
		userRepository.deleteAll();

		User owner = userRepository.save(User.create("김오너", "owner@example.com"));
		Project project = projectRepository.save(Project.create("협업 서비스 개편", null));
		projectMemberRepository.save(ProjectMember.create(project, owner, Role.OWNER));
		Task task = taskRepository.save(Task.create(project, "로그인 화면 퍼블리싱", null, null));

		ownerId = owner.getId();
		projectId = project.getId();
		taskId = task.getId();
	}

	/**
	 * [겹 1] 사용자가 화면에서 보고 있던 사이에 남이 먼저 수정한 경우.
	 *
	 * 이것이 실제로 대부분의 충돌이 일어나는 상황이다.
	 * A가 작업을 조회해 version 0을 받고 화면에서 고치는 동안,
	 * B가 조회→수정→저장을 끝내 DB의 version이 1이 된다.
	 * A가 뒤늦게 version 0을 실어 저장을 요청하면 거절되어야 한다.
	 *
	 * 하이버네이트의 @Version만으로는 이 경우가 잡히지 않는다.
	 * A의 요청이 시작될 때 서버는 DB에서 version 1을 새로 읽으므로,
	 * 하이버네이트 입장에서는 아무 충돌이 없기 때문이다.
	 * 서비스가 클라이언트의 version을 직접 비교해야 잡힌다.
	 */
	@Test
	@DisplayName("남이 먼저 수정한 뒤 옛 version으로 수정하면 409로 거절된다")
	void 옛_버전으로_수정하면_충돌() {
		// A가 조회한 시점의 version
		Long versionSeenByA = taskService.get(ownerId, projectId, taskId).version();

		// 그 사이 B가 먼저 수정해 DB의 version이 올라간다
		taskService.update(ownerId, projectId, taskId,
				new TaskUpdateRequest("B가 고친 제목", null, TaskStatus.IN_PROGRESS, null, versionSeenByA));

		// A가 옛 version으로 저장을 시도한다
		assertThatThrownBy(() -> taskService.update(ownerId, projectId, taskId,
				new TaskUpdateRequest("A가 고친 제목", null, TaskStatus.DONE, null, versionSeenByA)))
				.isInstanceOf(CollabException.class)
				.extracting(e -> ((CollabException) e).getErrorCode())
				.isEqualTo(ErrorCode.TASK_VERSION_CONFLICT);

		// B의 변경이 그대로 남아 있어야 한다. A의 제목으로 덮어써지지 않았다.
		assertThat(taskRepository.findById(taskId).orElseThrow().getTitle())
				.isEqualTo("B가 고친 제목");
	}

	/**
	 * [겹 2] 두 요청의 트랜잭션이 실제로 겹치는 경우.
	 *
	 * 겹 1의 version 비교를 두 요청이 모두 통과할 수 있다.
	 * 같은 version을 들고 있던 두 사람이 동시에 저장을 누르면 그렇게 된다.
	 * 그때는 하이버네이트의 @Version이 마지막 방어선이 된다.
	 *
	 * 재현 방법:
	 * 영속성 컨텍스트 두 개(em1, em2)로 같은 작업을 각각 읽는다. 둘 다 version 0을 본다.
	 * em2가 먼저 커밋해 DB의 version이 1이 된다.
	 * em1이 커밋을 시도하면 UPDATE ... WHERE id = ? AND version = 0 의 결과가
	 * 0건이 되어 하이버네이트가 예외를 던진다.
	 *
	 * 스레드를 쓰지 않는 이유:
	 * 영속성 컨텍스트를 두 개 만들면 타이밍을 맞출 필요 없이 같은 상황이 재현된다.
	 * 스레드를 쓰면 순서가 매번 달라져 테스트가 불안정해진다.
	 */
	@Test
	@DisplayName("같은 version을 읽은 두 트랜잭션 중 나중 커밋이 거절된다")
	void 동시_커밋시_나중_트랜잭션이_충돌() {
		EntityManager em1 = entityManagerFactory.createEntityManager();
		EntityManager em2 = entityManagerFactory.createEntityManager();

		try {
			em1.getTransaction().begin();
			Task taskInTx1 = em1.find(Task.class, taskId);

			em2.getTransaction().begin();
			Task taskInTx2 = em2.find(Task.class, taskId);

			// 둘 다 같은 version을 보고 있다
			assertThat(taskInTx1.getVersion()).isEqualTo(taskInTx2.getVersion());

			// 2번이 먼저 커밋한다 → DB의 version이 올라간다
			taskInTx2.update("먼저 커밋한 제목", null, TaskStatus.IN_PROGRESS, null);
			em2.getTransaction().commit();

			// 1번이 뒤늦게 커밋하면 충돌이 감지되어야 한다
			taskInTx1.update("나중에 커밋한 제목", null, TaskStatus.DONE, null);
			assertThatThrownBy(() -> em1.getTransaction().commit())
					.isInstanceOf(RollbackException.class)
					.hasCauseInstanceOf(OptimisticLockException.class);

			// 먼저 커밋한 변경이 살아남는다
			assertThat(taskRepository.findById(taskId).orElseThrow().getTitle())
					.isEqualTo("먼저 커밋한 제목");
		} finally {
			// 예외가 나든 안 나든 반드시 닫는다. 닫지 않으면 커넥션이 반납되지 않는다.
			em1.close();
			em2.close();
		}
	}

	/**
	 * 정상적인 연속 수정은 막히지 않아야 한다.
	 *
	 * 충돌 감지가 지나치게 엄격해 정상 흐름까지 막으면 기능이 쓸모없어진다.
	 * 응답으로 받은 version을 다음 수정에 실어 보내면 계속 수정할 수 있어야 한다.
	 * (이것이 동작하려면 수정 응답의 version이 flush 이후의 값이어야 한다)
	 */
	@Test
	@DisplayName("응답받은 version으로 이어서 수정하면 계속 성공한다")
	void 최신_버전으로는_연속_수정_가능() {
		Long version = taskService.get(ownerId, projectId, taskId).version();

		for (int i = 1; i <= 3; i++) {
			version = taskService.update(ownerId, projectId, taskId,
					new TaskUpdateRequest("수정 " + i, null, TaskStatus.IN_PROGRESS, null, version))
					.version();
		}

		assertThat(taskRepository.findById(taskId).orElseThrow().getTitle()).isEqualTo("수정 3");
		assertThat(version).isEqualTo(3L);
	}
}
