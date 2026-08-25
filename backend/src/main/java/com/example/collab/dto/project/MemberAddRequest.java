package com.example.collab.dto.project;

import com.example.collab.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 프로젝트 멤버 추가 요청 본문. */
@Schema(description = "프로젝트 멤버 추가 요청")
public record MemberAddRequest(

		@Schema(description = "추가할 사용자 ID", example = "3")
		@NotNull(message = "사용자 ID는 필수입니다.")
		Long userId,

		@Schema(description = "부여할 역할", example = "MEMBER")
		@NotNull(message = "역할은 필수입니다.")
		Role role
) {
}
