package com.example.collab.dto.task;

import com.example.collab.domain.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 작업 수정 요청 본문. version은 조회 때 받은 값을 그대로 실어 보내야 충돌을 감지할 수 있다. */
@Schema(description = "작업 수정 요청")
public record TaskUpdateRequest(

		@Schema(description = "작업 제목", example = "로그인 화면 퍼블리싱")
		@NotBlank(message = "작업 제목은 필수입니다.")
		@Size(max = 200, message = "작업 제목은 200자를 넘을 수 없습니다.")
		String title,

		@Schema(description = "작업 설명", example = "반응형까지 포함합니다.")
		@Size(max = 2000, message = "설명은 2000자를 넘을 수 없습니다.")
		String description,

		@Schema(description = "작업 상태", example = "IN_PROGRESS")
		@NotNull(message = "상태는 필수입니다.")
		TaskStatus status,

		@Schema(description = "담당자 사용자 ID (해제하려면 생략)", example = "3")
		Long assigneeId,

		@Schema(description = "조회 시 받은 version 값. 동시 수정 충돌 감지에 쓰인다.", example = "2")
		@NotNull(message = "version은 필수입니다.")
		Long version
) {
}
