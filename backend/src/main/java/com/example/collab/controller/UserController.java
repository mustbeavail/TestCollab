package com.example.collab.controller;

import com.example.collab.dto.user.UserCreateRequest;
import com.example.collab.dto.user.UserResponse;
import com.example.collab.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 API. 요청을 서비스에 넘기고 결과를 응답으로 바꾸는 일만 한다.
 * 사용자 API에만 X-User-Id가 없다. 인증이 구현 대상이 아니고 등록·조회에 걸린 권한 규칙도 없다.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 등록·조회")
public class UserController {

	private final UserService userService;

	/** @Valid 위반 시 이 메서드는 실행되지 않고 GlobalExceptionHandler가 400으로 바꾼다. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "사용자 등록", description = "이름과 이메일로 사용자를 등록한다. 이메일은 중복될 수 없다.")
	public UserResponse register(@Valid @RequestBody UserCreateRequest request) {
		return userService.register(request);
	}

	@GetMapping("/{userId}")
	@Operation(summary = "사용자 조회", description = "사용자 id로 한 명을 조회한다.")
	public UserResponse get(
			@Parameter(description = "조회할 사용자 ID", example = "1")
			@PathVariable Long userId
	) {
		return userService.get(userId);
	}
}
