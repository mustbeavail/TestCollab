package com.example.collab.dto.project;

import com.example.collab.domain.Project;
import com.example.collab.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** 프로젝트 정보 응답 본문. myRole은 요청자 기준이라 같은 프로젝트도 사람마다 다르게 나간다. */
@Schema(description = "프로젝트 정보")
public record ProjectResponse(

		@Schema(description = "프로젝트 ID", example = "1")
		Long id,

		@Schema(description = "프로젝트 이름", example = "협업 서비스 개편")
		String name,

		@Schema(description = "프로젝트 설명", example = "2026년 상반기 개편 작업")
		String description,

		@Schema(description = "요청자의 이 프로젝트에서의 역할", example = "OWNER")
		Role myRole,

		@Schema(description = "생성 시각")
		LocalDateTime createdAt,

		@Schema(description = "마지막 수정 시각")
		LocalDateTime updatedAt
) {

	public static ProjectResponse from(Project project, Role myRole) {
		return new ProjectResponse(
				project.getId(),
				project.getName(),
				project.getDescription(),
				myRole,
				project.getCreatedAt(),
				project.getUpdatedAt()
		);
	}
}
