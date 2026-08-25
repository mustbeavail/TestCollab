package com.example.collab.repository;

import com.example.collab.domain.ProjectMember;
import com.example.collab.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** ProjectMember 영속성 계층. 권한 판정의 출발점이다. */
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

	/** 모든 프로젝트·작업 API가 맨 처음 호출한다. 비어 있으면 비멤버(또는 없는 프로젝트)다. */
	Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

	boolean existsByProjectIdAndUserId(Long projectId, Long userId);

	/** "OWNER 최소 1명"은 SQL 제약으로 표현할 수 없어 세어서 확인한다. */
	long countByProjectIdAndRole(Long projectId, Role role);

	/** 내 멤버 행에서 출발해 join fetch로 프로젝트를 함께 읽는다(지연 로딩 N+1 방지). */
	@Query("select m from ProjectMember m join fetch m.project where m.user.id = :userId")
	List<ProjectMember> findByUserIdWithProject(@Param("userId") Long userId);

	/** 멤버 목록에 이름·이메일이 필요해 사용자를 함께 읽는다. */
	@Query("select m from ProjectMember m join fetch m.user where m.project.id = :projectId")
	List<ProjectMember> findByProjectIdWithUser(@Param("projectId") Long projectId);
}
