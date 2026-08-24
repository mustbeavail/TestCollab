package com.example.collab.controller;

import com.example.collab.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 등록·조회")
public class UserController {

	private final UserService userService;
}
