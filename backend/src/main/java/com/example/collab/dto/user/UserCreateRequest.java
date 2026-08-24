package com.example.collab.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 사용자 등록 요청 본문. (POST /api/users)
 *
 * record로 만드는 이유: 값을 담아 나르기만 하는 객체라 필드와 생성자,
 * 접근자가 전부 자동으로 생기고 값이 바뀌지 않는다(불변).
 * 접근자 이름은 name(), email() 처럼 필드명 그대로다.
 *
 * 붙은 검증 어노테이션은 컨트롤러 파라미터에 @Valid가 있을 때 동작한다.
 * 위반되면 컨트롤러 메서드는 아예 실행되지 않고
 * GlobalExceptionHandler가 400 응답으로 바꾼다.
 * springdoc이 이 어노테이션들을 읽어 Swagger 스키마에 필수 여부와
 * 길이 제한으로 표시해주기도 한다.
 *
 * @param name  표시용 이름
 * @param email 로그인에 쓰지는 않지만 사용자를 구분하는 값. 중복될 수 없다.
 */
@Schema(description = "사용자 등록 요청")
public record UserCreateRequest(

		@Schema(description = "이름", example = "김철수")
		@NotBlank(message = "이름은 필수입니다.")
		@Size(max = 50, message = "이름은 50자를 넘을 수 없습니다.")
		String name,

		@Schema(description = "이메일", example = "chulsoo@example.com")
		@NotBlank(message = "이메일은 필수입니다.")
		@Email(message = "이메일 형식이 올바르지 않습니다.")
		@Size(max = 255, message = "이메일은 255자를 넘을 수 없습니다.")
		String email
) {
}
