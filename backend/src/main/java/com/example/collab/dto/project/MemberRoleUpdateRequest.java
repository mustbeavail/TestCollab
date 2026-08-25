package com.example.collab.dto.project;

import com.example.collab.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 멤버 역할 변경 요청 본문. */
@Schema(description = "멤버 역할 변경 요청")
public record MemberRoleUpdateRequest(

		@Schema(description = "새 역할", example = "ADMIN")
		@NotNull(message = "역할은 필수입니다.")
		Role role
) {
}
