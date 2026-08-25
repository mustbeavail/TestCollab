package com.example.collab.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 사용자 등록 요청 본문. 검증 어노테이션은 컨트롤러의 @Valid가 있을 때 동작한다. */
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
