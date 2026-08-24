package com.example.collab.dto.task;

import com.example.collab.domain.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 작업 수정 요청 본문. (PATCH /api/projects/{projectId}/tasks/{taskId})
 *
 * version 필드가 이 DTO의 핵심이다.
 *
 * 클라이언트는 작업을 조회할 때 받은 version을 그대로 여기 실어 보낸다.
 * 서비스는 그 값과 DB의 현재 version을 비교해, 다르면 409로 거절한다.
 * 즉 "내가 화면에서 보고 있던 그 내용이 아직 그대로인가"를 확인하는 값이다.
 *
 * 이 비교가 없으면 다음이 막히지 않는다.
 *   A가 작업을 조회(version 2)하고 화면에서 고치는 동안
 *   B가 조회 → 수정 → 저장을 끝내면 DB는 version 3이 된다.
 *   A가 저장을 누르면 서버는 DB에서 version 3을 새로 읽으므로
 *   하이버네이트 입장에서는 충돌이 없고, B의 변경이 조용히 덮어써진다.
 *
 * @param title       새 제목
 * @param description 새 설명(비우려면 null)
 * @param status      새 상태
 * @param assigneeId  새 담당자 id(지정을 해제하려면 null)
 * @param version     수정하려는 작업을 조회했을 때 받은 version 값
 */
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
