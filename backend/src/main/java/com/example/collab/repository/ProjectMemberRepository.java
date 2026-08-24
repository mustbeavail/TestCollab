package com.example.collab.repository;

import com.example.collab.domain.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ProjectMember 엔티티의 영속성 계층.
 *
 * 권한 판정의 출발점이다. "이 사용자가 이 프로젝트의 멤버인가, 역할은 무엇인가"를
 * 여기서 조회한 뒤 서비스가 허용 여부를 결정한다.
 * 조회 결과가 없으면 비멤버이므로 조회를 포함한 모든 요청을 막는다.
 */
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
}
