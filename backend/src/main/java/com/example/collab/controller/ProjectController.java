package com.example.collab.controller;

import com.example.collab.service.ProjectService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프로젝트와 프로젝트 멤버 관련 HTTP 요청을 받는 진입점.
 *
 * 멤버 관리는 프로젝트에 종속된 자원이므로
 * /api/projects/{projectId}/members 형태의 하위 경로로 둘 예정이다.
 *
 * 인증이 없는 과제이므로 요청자 식별자는 파라미터로 받는다.
 * 어떤 형태로 받을지(쿼리 파라미터 / 헤더)는 API 설계 단계에서 정한다.
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Project", description = "프로젝트 CRUD 및 멤버 관리")
public class ProjectController {

	private final ProjectService projectService;
}
