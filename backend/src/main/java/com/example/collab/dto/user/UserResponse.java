package com.example.collab.dto.user;

import com.example.collab.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** 사용자 정보 응답 본문. 엔티티를 그대로 내보내면 응답 필드가 엔티티 변경에 딸려 바뀐다. */
@Schema(description = "사용자 정보")
public record UserResponse(

		@Schema(description = "사용자 ID", example = "1")
		Long id,

		@Schema(description = "이름", example = "김철수")
		String name,

		@Schema(description = "이메일", example = "chulsoo@example.com")
		String email,

		@Schema(description = "가입 시각")
		LocalDateTime createdAt
) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getCreatedAt()
		);
	}
}
