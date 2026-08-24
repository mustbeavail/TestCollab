package com.example.collab.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 작업 생성 요청 본문. (POST /api/projects/{projectId}/tasks)
 *
 * status를 받지 않는 이유:
 * 새로 만든 작업은 항상 TODO에서 시작한다. 아직 아무도 손대지 않은 작업이
 * 처음부터 DONE인 채로 만들어질 이유가 없다. 이 규칙은 Task.create()가 강제한다.
 *
 * version을 받지 않는 이유:
 * 낙관적 락은 "이미 있는 것을 고칠 때" 필요한 장치다. 새로 만드는 작업에는
 * 먼저 고친 사람이 있을 수 없어 비교할 대상 자체가 없다.
 *
 * @param title       작업 제목. 목록 조회의 검색 대상이 된다.
 * @param description 설명(없으면 null)
 * @param assigneeId  담당자로 지정할 사용자 id. 지정하지 않으려면 null.
 *                    값이 있으면 그 사람이 이 프로젝트의 멤버인지 서비스가 확인한다.
 */
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
