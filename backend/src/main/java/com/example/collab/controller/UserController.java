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
 * 사용자 관련 HTTP 요청을 받는 진입점.
 *
 * @RestController : @Controller + @ResponseBody. 메서드 반환값을 뷰 이름이 아니라
 *                   JSON 응답 본문으로 직렬화한다.
 * @RequestMapping : 이 컨트롤러의 모든 메서드에 공통으로 붙는 URL 앞부분.
 * @Tag            : Swagger UI에서 이 컨트롤러의 API들을 묶어 보여줄 이름과 설명.
 *
 * 이 계층은 요청을 받아 서비스에 넘기고 결과를 응답으로 바꾸는 일만 한다.
 * 권한 판정 같은 판단은 서비스 계층에 둔다.
 *
 * 사용자 API에는 요청자 식별(X-User-Id)이 없다. 인증이 구현 대상이 아니고,
 * 등록·조회에 걸리는 권한 규칙도 명세에 없기 때문이다.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 등록·조회")
public class UserController {

	private final UserService userService;

	/**
	 * 사용자를 등록한다.
	 *
	 * @Valid : 요청 본문을 UserCreateRequest로 바꾼 뒤 검증 어노테이션을 확인한다.
	 *          위반이 있으면 이 메서드는 실행되지 않고 GlobalExceptionHandler가
	 *          400 응답으로 바꾼다. 덕분에 서비스는 검증된 값만 받는다.
	 * @ResponseStatus(CREATED) : 성공 시 200이 아니라 201을 내보낸다.
	 *          새 자원이 만들어졌음을 뜻하는 상태 코드다.
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "사용자 등록", description = "이름과 이메일로 사용자를 등록한다. 이메일은 중복될 수 없다.")
	public UserResponse register(@Valid @RequestBody UserCreateRequest request) {
		return userService.register(request);
	}

	/**
	 * 사용자 한 명을 조회한다.
	 *
	 * @PathVariable : URL 경로의 {userId} 자리에 들어온 값을 파라미터로 받는다.
	 */
	@GetMapping("/{userId}")
	@Operation(summary = "사용자 조회", description = "사용자 id로 한 명을 조회한다.")
	public UserResponse get(
			@Parameter(description = "조회할 사용자 ID", example = "1")
			@PathVariable Long userId
	) {
		return userService.get(userId);
	}
}
