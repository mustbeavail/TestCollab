package com.example.collab.dto.project;

import com.example.collab.domain.ProjectMember;
import com.example.collab.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** 프로젝트 멤버 응답 본문. */
@Schema(description = "프로젝트 멤버 정보")
public record MemberResponse(

		@Schema(description = "사용자 ID", example = "3")
		Long userId,

		@Schema(description = "이름", example = "박멤버")
		String name,

		@Schema(description = "이메일", example = "member@example.com")
		String email,

		@Schema(description = "이 프로젝트에서의 역할", example = "MEMBER")
		Role role,

		@Schema(description = "합류 시각")
		LocalDateTime joinedAt
) {

	public static MemberResponse from(ProjectMember member) {
		return new MemberResponse(
				member.getUser().getId(),
				member.getUser().getName(),
				member.getUser().getEmail(),
				member.getRole(),
				member.getJoinedAt()
		);
	}
}
