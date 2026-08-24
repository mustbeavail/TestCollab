package com.example.collab.repository;

import com.example.collab.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Project 엔티티의 영속성 계층.
 *
 * "내가 속한 프로젝트 목록 조회"처럼 멤버십을 타고 들어가는 조회는
 * 여기 또는 ProjectMemberRepository에 메서드를 추가해 처리한다.
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {
}
