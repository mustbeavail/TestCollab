package com.example.collab.dto.project;

import com.example.collab.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 멤버 역할 변경 요청 본문. (PATCH /api/projects/{projectId}/members/{userId})
 *
 * 바꿀 대상(userId)은 경로에서 오므로 본문에는 새 역할만 담는다.
 *
 * 이 요청이 막히는 두 경우(둘 다 서비스가 판정한다):
 * - OWNER를 임명하거나 해임하는 변경인데 요청자가 OWNER가 아닌 경우
 * - 마지막 남은 OWNER를 다른 역할로 낮추려는 경우
 *
 * @param role 새로 부여할 역할
 */
@Schema(description = "멤버 역할 변경 요청")
public record MemberRoleUpdateRequest(

		@Schema(description = "새 역할", example = "ADMIN")
		@NotNull(message = "역할은 필수입니다.")
		Role role
) {
}
