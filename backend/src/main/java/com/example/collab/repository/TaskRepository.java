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
 * Task 영속성 계층.
 * 모든 조회에 projectId 조건이 들어간다. 빠지면 다른 프로젝트의 작업이 섞인다.
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

	/** taskId만으로 찾으면 다른 프로젝트의 작업 id를 넣었을 때 남의 데이터가 나간다. */
	Optional<Task> findByIdAndProjectId(Long taskId, Long projectId);

	/**
	 * 검색·필터·페이징 조회. "(:param is null or 조건)"으로 선택 조건을 표현해
	 * 조합마다 메서드를 만들지 않는다. 동적 조건이 여기 하나뿐이라 QueryDSL은 쓰지 않았다.
	 *
	 * left join fetch : 응답에 담당자 이름이 들어가는데 지연 로딩이라 그냥 두면 N+1이 난다.
	 *                   담당자 없는 작업도 빠지면 안 되므로 left join이다.
	 * countQuery      : 건수만 셀 때는 담당자를 읽을 이유가 없어 fetch를 뺀 쿼리를 따로 준다.
	 *
	 * 담당자 조건이 둘로 갈리는 이유: assigneeId는 값을 주지 않으면 "담당자를 안 따진다"는
	 * 뜻이라 "담당자가 없는 작업만"을 표현할 수 없다. 뜻이 다른 조건이라 따로 받는다.
	 */
	@Query(value = """
			select t from Task t
			left join fetch t.assignee
			where t.project.id = :projectId
			  and (:keyword is null or t.title like concat('%', :keyword, '%'))
			  and (:status is null or t.status = :status)
			  and (:assigneeId is null or t.assignee.id = :assigneeId)
			  and (:unassignedOnly = false or t.assignee is null)
			""",
			countQuery = """
					select count(t) from Task t
					where t.project.id = :projectId
					  and (:keyword is null or t.title like concat('%', :keyword, '%'))
					  and (:status is null or t.status = :status)
					  and (:assigneeId is null or t.assignee.id = :assigneeId)
					  and (:unassignedOnly = false or t.assignee is null)
					""")
	Page<Task> search(@Param("projectId") Long projectId,
	                  @Param("keyword") String keyword,
	                  @Param("status") TaskStatus status,
	                  @Param("assigneeId") Long assigneeId,
	                  @Param("unassignedOnly") boolean unassignedOnly,
	                  Pageable pageable);

	/**
	 * 멤버 제거 시 그 사람이 담당하던 작업의 담당자만 비운다.
	 * assignee_id는 users를 가리켜 멤버를 제거해도 외래키가 끊기지 않으므로 직접 정리해야 한다.
	 * 벌크 연산이라 영속성 컨텍스트를 거치지 않는다(멤버 제거 흐름에서는 Task를 읽지 않아 무방).
	 */
	@Modifying
	@Query("update Task t set t.assignee = null "
			+ "where t.project.id = :projectId and t.assignee.id = :userId")
	void clearAssignee(@Param("projectId") Long projectId, @Param("userId") Long userId);
}
