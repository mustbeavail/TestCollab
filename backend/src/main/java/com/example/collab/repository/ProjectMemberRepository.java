package com.example.collab.repository;

import com.example.collab.domain.ProjectMember;
import com.example.collab.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * ProjectMember 엔티티의 영속성 계층.
 *
 * 권한 판정의 출발점이다. "이 사용자가 이 프로젝트의 멤버인가, 역할은 무엇인가"를
 * 여기서 조회한 뒤 서비스가 허용 여부를 결정한다.
 * 조회 결과가 없으면 비멤버이므로 조회를 포함한 모든 요청을 막는다.
 */
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

	/**
	 * 특정 프로젝트에서 특정 사용자의 멤버 정보를 찾는다.
	 *
	 * 모든 프로젝트·작업 API가 맨 처음 호출하는 조회다.
	 * 결과가 비어 있다는 것은 "그 프로젝트의 멤버가 아니다"라는 뜻이고,
	 * 프로젝트가 아예 존재하지 않는 경우도 똑같이 비어 있게 나온다.
	 * 둘을 구분하지 않는 이유는 ErrorCode.NOT_PROJECT_MEMBER 설명 참고.
	 *
	 * (project_id, user_id)에 유니크 제약이 있어 결과는 최대 한 건이다.
	 *
	 * @return 멤버 정보. 멤버가 아니면 비어 있는 Optional
	 */
	Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

	/**
	 * 이미 그 프로젝트의 멤버인지 확인한다. 멤버를 추가하기 전 중복 검사에 쓴다.
	 *
	 * 역할 정보가 필요 없는 자리라 엔티티를 읽어오지 않고 boolean으로 받는다.
	 */
	boolean existsByProjectIdAndUserId(Long projectId, Long userId);

	/**
	 * 특정 프로젝트에서 특정 역할을 가진 멤버가 몇 명인지 센다.
	 *
	 * "프로젝트에는 항상 최소 1명의 OWNER가 있어야 한다"를 지키기 위한 조회다.
	 * 마지막 OWNER를 강등하거나 제거하려 할 때 이 값이 1이면 막는다.
	 *
	 * 이 규칙을 DB 제약으로 표현할 수 없기 때문에 이렇게 세어 확인한다.
	 * ("특정 조건의 행이 최소 1건 남아야 한다"는 표준 SQL 제약에 없다)
	 */
	long countByProjectIdAndRole(Long projectId, Role role);

	/**
	 * 어떤 사용자가 속한 모든 멤버 정보를 프로젝트와 함께 조회한다.
	 * "내가 속한 프로젝트 목록" API가 쓴다.
	 *
	 * join fetch를 쓰는 이유:
	 * ProjectMember.project는 지연 로딩이라, 조회 결과 N건에서 각각
	 * getProject()에 접근하면 프로젝트 조회 쿼리가 N번 더 나간다(N+1 문제).
	 * join fetch는 조인해서 가져온 프로젝트를 그 자리에서 채워 넣으므로
	 * 쿼리가 한 번으로 끝난다.
	 *
	 * (일반 join과 달리 fetch가 붙어야 연관 엔티티까지 함께 로딩된다.
	 *  fetch 없는 join은 조건에만 쓰이고 프로젝트는 여전히 프록시로 남는다)
	 *
	 * 이 조회는 project 테이블 전체를 훑지 않는다. 내 멤버 행에서 출발하므로
	 * 내가 속하지 않은 프로젝트는 애초에 결과에 들어올 수 없다.
	 */
	@Query("select m from ProjectMember m join fetch m.project where m.user.id = :userId")
	List<ProjectMember> findByUserIdWithProject(@Param("userId") Long userId);

	/**
	 * 어떤 프로젝트의 모든 멤버를 사용자 정보와 함께 조회한다.
	 * 멤버 목록 API가 쓴다.
	 *
	 * 멤버 목록에는 이름과 이메일을 보여줘야 하는데 ProjectMember.user 역시
	 * 지연 로딩이라, 위와 같은 이유로 join fetch가 필요하다.
	 */
	@Query("select m from ProjectMember m join fetch m.user where m.project.id = :projectId")
	List<ProjectMember> findByProjectIdWithUser(@Param("projectId") Long projectId);
}
