package com.example.collab.service;

import com.example.collab.repository.ProjectMemberRepository;
import com.example.collab.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 작업 CRUD와 목록 조회(검색·상태 필터·페이징)의 비즈니스 로직을 담는 계층.
 *
 * 작업 수정·삭제는 담당자 본인이거나 그 프로젝트의 OWNER·ADMIN만 가능하므로,
 * 작업이 속한 프로젝트에서 요청자의 역할을 조회해 판정한다.
 * 그래서 TaskRepository와 함께 ProjectMemberRepository를 주입받는다.
 *
 * 동시 수정 충돌은 Task 엔티티의 @Version이 감지한다.
 * 여기서 잡아 처리하지 않고 예외를 그대로 올려보내면
 * 예외 처리기가 409 Conflict 응답으로 변환한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

	private final TaskRepository taskRepository;
	private final ProjectMemberRepository projectMemberRepository;
}
