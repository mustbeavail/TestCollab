package com.example.collab.controller;

import com.example.collab.service.TaskService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 작업 관련 HTTP 요청을 받는 진입점.
 *
 * 작업은 항상 어떤 프로젝트에 속하고 권한도 프로젝트 기준으로 판정되므로,
 * 경로를 프로젝트 하위에 둔다. 프로젝트 식별자가 경로에 항상 들어가면
 * 권한 검사와 조회 조건에서 그것을 빠뜨리기 어려워진다.
 * ({projectId}는 각 메서드가 @PathVariable로 받는다)
 */
@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
@RequiredArgsConstructor
@Tag(name = "Task", description = "작업 CRUD 및 목록 조회(검색·필터·페이징)")
public class TaskController {

	private final TaskService taskService;
}
