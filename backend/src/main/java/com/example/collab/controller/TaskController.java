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
 * 경로를 /api/projects/{projectId}/tasks 형태로 둘지
 * 지금처럼 /api/tasks 아래에 두고 프로젝트 식별자를 파라미터로 받을지는
 * API 설계 단계에서 정한다. 지금 경로는 임시다.
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Task", description = "작업 CRUD 및 목록 조회(검색·필터·페이징)")
public class TaskController {

	private final TaskService taskService;
}
