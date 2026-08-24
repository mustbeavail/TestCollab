package com.example.collab.controller;

import com.example.collab.domain.TaskStatus;
import com.example.collab.dto.task.TaskCreateRequest;
import com.example.collab.dto.task.TaskResponse;
import com.example.collab.dto.task.TaskUpdateRequest;
import com.example.collab.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "작업 생성",
			description = "그 프로젝트의 멤버라면 누구나 만들 수 있다. 상태는 항상 TODO로 시작한다. "
					+ "담당자를 지정하면 그 사람이 이 프로젝트의 멤버인지 확인한다.")
	public TaskResponse create(
			@Parameter(description = "요청자 사용자 ID", example = "1")
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable Long projectId,
			@Valid @RequestBody TaskCreateRequest request
	) {
		return taskService.create(userId, projectId, request);
	}

	/**
	 * 작업 목록을 조회한다.
	 *
	 * 세 필터는 모두 선택 사항이다(required = false). 값을 주지 않으면 그 조건은 무시된다.
	 *
	 * Pageable을 파라미터로 두면 스프링이 요청의 page·size·sort 값을 채워 넘겨준다.
	 * @PageableDefault는 그 값들이 없을 때 쓸 기본값이다.
	 * 최신 작업이 먼저 보이도록 createdAt 내림차순을 기본 정렬로 둔다.
	 */
	@GetMapping
	@Operation(summary = "작업 목록 조회",
			description = "제목 검색·상태 필터·담당자 필터와 페이징을 지원한다. 다른 프로젝트의 작업은 섞이지 않는다.")
	public Page<TaskResponse> list(
			@Parameter(description = "요청자 사용자 ID", example = "1")
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable Long projectId,

			@Parameter(description = "제목에 포함된 검색어", example = "로그인")
			@RequestParam(required = false) String keyword,

			@Parameter(description = "상태 필터", example = "IN_PROGRESS")
			@RequestParam(required = false) TaskStatus status,

			@Parameter(description = "담당자 사용자 ID 필터", example = "3")
			@RequestParam(required = false) Long assigneeId,

			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
			Pageable pageable
	) {
		return taskService.search(userId, projectId, keyword, status, assigneeId, pageable);
	}

	@GetMapping("/{taskId}")
	@Operation(summary = "작업 상세", description = "그 프로젝트의 멤버만 조회할 수 있다.")
	public TaskResponse get(
			@Parameter(description = "요청자 사용자 ID", example = "1")
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable Long projectId,
			@PathVariable Long taskId
	) {
		return taskService.get(userId, projectId, taskId);
	}

	@PatchMapping("/{taskId}")
	@Operation(summary = "작업 수정",
			description = "담당자 본인이거나 OWNER·ADMIN만 할 수 있다. "
					+ "조회 시 받은 version을 함께 보내야 하며, 그 사이 다른 사용자가 먼저 수정했다면 409로 거절된다.")
	public TaskResponse update(
			@Parameter(description = "요청자 사용자 ID", example = "1")
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable Long projectId,
			@PathVariable Long taskId,
			@Valid @RequestBody TaskUpdateRequest request
	) {
		return taskService.update(userId, projectId, taskId, request);
	}

	@DeleteMapping("/{taskId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "작업 삭제", description = "담당자 본인이거나 OWNER·ADMIN만 할 수 있다.")
	public void delete(
			@Parameter(description = "요청자 사용자 ID", example = "1")
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable Long projectId,
			@PathVariable Long taskId
	) {
		taskService.delete(userId, projectId, taskId);
	}
}
