package com.example.collab.dto.task;

import com.example.collab.domain.Task;
import com.example.collab.domain.TaskStatus;
import com.example.collab.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 작업 정보 응답 본문.
 * version은 화면에 보이지 않지만, 클라이언트가 수정 요청에 되돌려줘야 충돌 감지가 성립한다.
 */
@Schema(description = "작업 정보")
public record TaskResponse(

		@Schema(description = "작업 ID", example = "12")
		Long id,

		@Schema(description = "소속 프로젝트 ID", example = "1")
		Long projectId,

		@Schema(description = "작업 제목", example = "로그인 화면 퍼블리싱")
		String title,

		@Schema(description = "작업 설명", example = "반응형까지 포함합니다.")
		String description,

		@Schema(description = "작업 상태", example = "IN_PROGRESS")
		TaskStatus status,

		@Schema(description = "담당자 (미지정이면 null)")
		Assignee assignee,

		@Schema(description = "버전. 수정 요청에 그대로 실어 보내야 한다.", example = "2")
		Long version,

		@Schema(description = "생성 시각")
		LocalDateTime createdAt,

		@Schema(description = "마지막 수정 시각")
		LocalDateTime updatedAt
) {

	/** 담당자 이름까지 담아 목록에서 사용자 조회를 다시 하지 않게 한다. 화면에 필요한 두 값만 둔다. */
	@Schema(description = "담당자 요약 정보")
	public record Assignee(

			@Schema(description = "사용자 ID", example = "3")
			Long id,

			@Schema(description = "이름", example = "박멤버")
			String name
	) {
		public static Assignee from(User user) {
			return new Assignee(user.getId(), user.getName());
		}
	}

	/** 담당자가 지연 로딩이라 트랜잭션 안(서비스 계층)에서 호출해야 한다. open-in-view는 꺼져 있다. */
	public static TaskResponse from(Task task) {
		return new TaskResponse(
				task.getId(),
				task.getProject().getId(),
				task.getTitle(),
				task.getDescription(),
				task.getStatus(),
				task.getAssignee() == null ? null : Assignee.from(task.getAssignee()),
				task.getVersion(),
				task.getCreatedAt(),
				task.getUpdatedAt()
		);
	}
}
