package com.example.collab.controller;

import com.example.collab.dto.project.*;
import com.example.collab.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 프로젝트와 프로젝트 멤버 관련 HTTP 요청을 받는 진입점.
 *
 * 멤버 관리는 프로젝트에 종속된 자원이므로
 * /api/projects/{projectId}/members 형태의 하위 경로로 둔다.
 *
 * 인증이 없는 과제이므로 요청자 식별자는 X-User-Id 헤더로 받는다.
 * 쿼리 파라미터가 아니라 헤더로 두는 이유는, 요청자 식별이 모든 API에
 * 공통으로 붙는 값이라 개별 API의 입력과 같은 자리에 섞이면
 * 어느 쪽이 무엇인지 흐려지기 때문이다.
 *
 * 이 계층은 요청을 받아 서비스에 넘기고 결과를 응답으로 바꾸는 일만 한다.
 * 권한 판정은 전부 ProjectService가 한다.
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Project", description = "프로젝트 CRUD 및 멤버 관리")
public class ProjectController {

	private final ProjectService projectService;

	// ================================================================
	// 프로젝트
	// ================================================================

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "프로젝트 생성", description = "누구나 만들 수 있다. 만든 사람이 자동으로 OWNER 멤버가 된다.")
	public ProjectResponse create(
			@Parameter(description = "요청자 사용자 ID", example = "1")
			@RequestHeader("X-User-Id") Long userId,
			@Valid @RequestBody ProjectCreateRequest request
	) {
		return projectService.create(userId, request);
	}

	@GetMapping
	@Operation(summary = "내 프로젝트 목록", description = "요청자가 멤버로 속한 프로젝트만 돌려준다. 각 항목에 요청자의 역할이 담긴다.")
	public List<ProjectResponse> myProjects(
			@Parameter(description = "요청자 사용자 ID", example = "1")
			@RequestHeader("X-User-Id") Long userId
	) {
		return projectService.findMyProjects(userId);
	}

	@GetMapping("/{projectId}")
	@Operation(summary = "프로젝트 상세", description = "그 프로젝트의 멤버만 조회할 수 있다. 비멤버는 403.")
	public ProjectResponse get(
			@Parameter(description = "요청자 사용자 ID", example = "1")
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable Long projectId
	) {
		return projectService.get(userId, projectId);
	}

	@PatchMapping("/{projectId}")
	@Operation(summary = "프로젝트 수정", description = "OWNER와 ADMIN만 할 수 있다.")
	public ProjectResponse update(
			@Parameter(description = "요청자 사용자 ID", example = "1")
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable Long projectId,
			@Valid @RequestBody ProjectUpdateRequest request
	) {
		return projectService.update(userId, projectId, request);
	}

	/**
	 * 프로젝트를 삭제한다.
	 *
	 * @ResponseStatus(NO_CONTENT) : 돌려줄 본문이 없으므로 204로 응답한다.
	 *                               반환 타입이 void라 본문 없이 상태 코드만 나간다.
	 */
	@DeleteMapping("/{projectId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "프로젝트 삭제", description = "OWNER만 할 수 있다. 딸린 작업과 멤버도 함께 삭제된다.")
	public void delete(
			@Parameter(description = "요청자 사용자 ID", example = "1")
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable Long projectId
	) {
		projectService.delete(userId, projectId);
	}

	// ================================================================
	// 프로젝트 멤버
	// ================================================================

	@GetMapping("/{projectId}/members")
	@Operation(summary = "멤버 목록", description = "그 프로젝트의 멤버라면 누구나 조회할 수 있다.")
	public List<MemberResponse> members(
			@Parameter(description = "요청자 사용자 ID", example = "1")
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable Long projectId
	) {
		return projectService.findMembers(userId, projectId);
	}

	@PostMapping("/{projectId}/members")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "멤버 추가",
			description = "OWNER와 ADMIN이 할 수 있다. 다만 OWNER 역할을 부여하는 것은 OWNER만 가능하다.")
	public MemberResponse addMember(
			@Parameter(description = "요청자 사용자 ID", example = "1")
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable Long projectId,
			@Valid @RequestBody MemberAddRequest request
	) {
		return projectService.addMember(userId, projectId, request);
	}

	@PatchMapping("/{projectId}/members/{targetUserId}")
	@Operation(summary = "멤버 역할 변경",
			description = "OWNER를 임명하거나 해임하는 변경은 OWNER만 할 수 있다. 마지막 OWNER는 강등할 수 없다.")
	public MemberResponse changeRole(
			@Parameter(description = "요청자 사용자 ID", example = "1")
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable Long projectId,
			@Parameter(description = "역할을 바꿀 사용자 ID", example = "3")
			@PathVariable Long targetUserId,
			@Valid @RequestBody MemberRoleUpdateRequest request
	) {
		return projectService.changeRole(userId, projectId, targetUserId, request);
	}

	@DeleteMapping("/{projectId}/members/{targetUserId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "멤버 제거",
			description = "OWNER인 멤버를 제거하는 것은 OWNER만 할 수 있다. 마지막 OWNER는 제거할 수 없다. "
					+ "제거된 사람이 담당하던 작업은 삭제되지 않고 담당자만 비워진다.")
	public void removeMember(
			@Parameter(description = "요청자 사용자 ID", example = "1")
			@RequestHeader("X-User-Id") Long userId,
			@PathVariable Long projectId,
			@Parameter(description = "제거할 사용자 ID", example = "3")
			@PathVariable Long targetUserId
	) {
		projectService.removeMember(userId, projectId, targetUserId);
	}
}
