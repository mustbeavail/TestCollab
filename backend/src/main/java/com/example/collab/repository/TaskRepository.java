package com.example.collab.repository;

import com.example.collab.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
