package com.example.collab.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 프로젝트 생성 요청 본문. */
@Schema(description = "프로젝트 생성 요청")
public record ProjectCreateRequest(

		@Schema(description = "프로젝트 이름", example = "협업 서비스 개편")
		@NotBlank(message = "프로젝트 이름은 필수입니다.")
		@Size(max = 100, message = "프로젝트 이름은 100자를 넘을 수 없습니다.")
		String name,

		@Schema(description = "프로젝트 설명", example = "2026년 상반기 개편 작업")
		@Size(max = 500, message = "설명은 500자를 넘을 수 없습니다.")
		String description
) {
}
