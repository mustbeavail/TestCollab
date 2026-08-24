package com.example.collab.repository;

import com.example.collab.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Task 엔티티의 영속성 계층.
 *
 * 작업 목록 조회는 검색·상태 필터·페이징을 함께 지원해야 한다.
 * 페이징은 메서드 파라미터로 Pageable을 받고 반환 타입을 Page<Task>로 두면
 * Spring Data JPA가 LIMIT/OFFSET과 전체 건수 조회를 함께 처리한다.
 *
 * 이때 조회 조건에 소속 프로젝트를 반드시 포함해야 한다.
 * 그러지 않으면 다른 프로젝트의 작업이 목록에 섞여 나온다.
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

	/**
	 * 멤버가 프로젝트에서 제거될 때, 그 사람이 담당하던 작업의 담당자를 비운다.
	 *
	 * 왜 필요한가:
	 * task.assignee_id는 project_member가 아니라 users를 가리키므로,
	 * 멤버를 제거해도 외래키가 끊기지 않는다. 그대로 두면
	 * "그 프로젝트의 멤버가 아닌 사람이 담당자"인 작업이 남는다.
	 * 작업까지 함께 지우면 남의 작업 내용이 사라지고, 멤버 제거를 막으면
	 * 퇴사·이동을 처리할 방법이 없어진다. 그래서 담당자만 비운다.
	 *
	 * @Modifying : 이 쿼리가 SELECT가 아니라 UPDATE/DELETE임을 알린다.
	 *              이게 없으면 Spring Data가 조회 쿼리로 실행하려다 실패한다.
	 *              변경 쿼리라 @Transactional 안에서만 호출할 수 있다.
	 *
	 * 이 쿼리는 영속성 컨텍스트를 거치지 않고 DB로 직행한다(벌크 연산).
	 * 따라서 같은 트랜잭션에서 이미 읽어둔 Task 객체가 있으면 그 객체의
	 * assignee는 비워지지 않은 옛 상태로 남는다. 멤버 제거 흐름에서는
	 * Task를 읽지 않으므로 문제되지 않는다.
	 *
	 * @param projectId 대상 프로젝트
	 * @param userId    담당자에서 내릴 사용자
	 */
	@Modifying
	@Query("update Task t set t.assignee = null "
			+ "where t.project.id = :projectId and t.assignee.id = :userId")
	void clearAssignee(@Param("projectId") Long projectId, @Param("userId") Long userId);
}
