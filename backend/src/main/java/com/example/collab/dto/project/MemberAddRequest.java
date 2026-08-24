package com.example.collab.dto.project;

import com.example.collab.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 프로젝트 멤버 추가 요청 본문. (POST /api/projects/{projectId}/members)
 *
 * 어떤 프로젝트에 넣을지(projectId)는 경로에서 오고, 누가 추가하는지는
 * X-User-Id 헤더에서 온다. 본문에는 "누구를, 어떤 역할로"만 담는다.
 *
 * role에 OWNER를 넣을 수 있는지:
 * 값 자체는 막지 않는다. 다만 OWNER를 부여하는 요청은 요청자가 OWNER일 때만
 * 통과한다. ADMIN이 OWNER를 만들 수 있으면 ADMIN이 자기편 OWNER를 세워
 * 두 역할을 나눈 의미가 사라지기 때문이다. 그 판정은 서비스가 한다.
 *
 * @param userId 멤버로 추가할 사용자의 id
 * @param role   부여할 역할
 */
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
