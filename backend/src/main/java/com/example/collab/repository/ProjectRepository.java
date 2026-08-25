package com.example.collab.repository;

import com.example.collab.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

/** Project 영속성 계층. 멤버십을 타는 조회는 ProjectMemberRepository에 둔다. */
public interface ProjectRepository extends JpaRepository<Project, Long> {
}
