package com.example.collab.repository;

import com.example.collab.domain.Task;
import com.example.collab.domain.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

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
	 * 작업 한 건을 id와 소속 프로젝트로 함께 찾는다.
	 *
	 * findById(taskId)를 쓰지 않는 이유:
	 * taskId만으로 찾으면 다른 프로젝트의 작업 id를 넣었을 때 남의 데이터가 그대로 나간다.
	 * 권한 검사는 projectId 기준으로 통과했는데 조회는 taskId 기준으로 하면,
	 * 그 둘이 어긋나는 순간 프로젝트 간 격리가 깨진다.
	 * 두 값을 함께 조건에 넣으면 "내가 권한을 확인한 그 프로젝트의 작업"만 나온다.
	 *
	 * @return 작업. 없거나 다른 프로젝트의 작업이면 비어 있는 Optional
	 */
	Optional<Task> findByIdAndProjectId(Long taskId, Long projectId);

	/**
	 * 작업 목록을 검색·필터·페이징해서 조회한다.
	 *
	 * 조건 세 가지는 모두 선택 사항이다. 값을 주지 않으면(null) 그 조건은 무시된다.
	 * "(:param is null or 조건)" 형태가 그 역할을 한다. 파라미터가 null이면
	 * 앞쪽이 참이 되어 뒤쪽 조건을 따지지 않는다.
	 * 조건 조합마다 별도 메서드를 만들 필요가 없어진다.
	 *
	 * QueryDSL을 쓰지 않은 이유:
	 * 동적 조건이 필요한 곳이 이 조회 한 군데뿐이다. 조건이 세 개인 쿼리 하나 때문에
	 * 빌드 설정과 생성 클래스를 들이는 것은 규모에 맞지 않는다.
	 *
	 * left join fetch t.assignee :
	 * 응답에 담당자 이름이 들어가는데 Task.assignee는 지연 로딩이라,
	 * 그냥 두면 결과 20건에 대해 담당자 조회가 최대 20번 더 나간다(N+1).
	 * 담당자가 없는 작업도 결과에서 빠지면 안 되므로 inner가 아니라 left join이다.
	 *
	 * countQuery를 따로 주는 이유:
	 * Page를 반환하려면 전체 건수를 세는 쿼리가 따로 필요하다. 개수만 셀 때는
	 * 담당자를 함께 읽어올 이유가 없으므로 join fetch를 뺀 쿼리를 지정한다.
	 *
	 * 첫 줄의 t.project.id 조건이 프로젝트 간 격리를 만든다.
	 * 이 조건이 빠지면 다른 프로젝트의 작업이 목록에 섞인다.
	 *
	 * @param projectId  조회할 프로젝트 (필수)
	 * @param keyword    제목 부분 검색어 (없으면 null)
	 * @param status     상태 필터 (없으면 null)
	 * @param assigneeId 담당자 필터 (없으면 null)
	 * @param pageable   페이지 번호·크기·정렬
	 */
	@Query(value = """
			select t from Task t
			left join fetch t.assignee
			where t.project.id = :projectId
			  and (:keyword is null or t.title like concat('%', :keyword, '%'))
			  and (:status is null or t.status = :status)
			  and (:assigneeId is null or t.assignee.id = :assigneeId)
			""",
			countQuery = """
					select count(t) from Task t
					where t.project.id = :projectId
					  and (:keyword is null or t.title like concat('%', :keyword, '%'))
					  and (:status is null or t.status = :status)
					  and (:assigneeId is null or t.assignee.id = :assigneeId)
					""")
	Page<Task> search(@Param("projectId") Long projectId,
	                  @Param("keyword") String keyword,
	                  @Param("status") TaskStatus status,
	                  @Param("assigneeId") Long assigneeId,
	                  Pageable pageable);

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
