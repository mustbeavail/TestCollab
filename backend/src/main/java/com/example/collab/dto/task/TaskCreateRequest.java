package com.example.collab.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 작업 생성 요청 본문. 상태는 항상 TODO로 시작하므로 받지 않는다. */
@Schema(description = "작업 생성 요청")
public record TaskCreateRequest(

		@Schema(description = "작업 제목", example = "로그인 화면 퍼블리싱")
		@NotBlank(message = "작업 제목은 필수입니다.")
		@Size(max = 200, message = "작업 제목은 200자를 넘을 수 없습니다.")
		String title,

		@Schema(description = "작업 설명", example = "반응형까지 포함합니다.")
		@Size(max = 2000, message = "설명은 2000자를 넘을 수 없습니다.")
		String description,

		@Schema(description = "담당자 사용자 ID (미지정 가능)", example = "3")
		Long assigneeId
) {
}
