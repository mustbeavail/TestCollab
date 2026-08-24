package com.example.collab.dto.task;

import com.example.collab.domain.Task;
import com.example.collab.domain.TaskStatus;
import com.example.collab.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 작업 정보 응답 본문.
 *
 * version을 함께 내려주는 이유:
 * 클라이언트가 이 값을 들고 있다가 수정 요청에 그대로 실어 보내야
 * 서버가 동시 수정 충돌을 감지할 수 있다(TaskUpdateRequest 설명 참고).
 * 화면에 보이지는 않지만 반드시 필요한 값이다.
 *
 * projectId를 담는 이유:
 * 목록 응답 한 건만 보고도 어느 프로젝트의 작업인지 알 수 있어야,
 * 클라이언트가 수정·삭제 요청 경로(/api/projects/{projectId}/tasks/{taskId})를
 * 조립할 수 있다.
 *
 * @param id          작업 식별자
 * @param projectId   소속 프로젝트 식별자
 * @param title       제목
 * @param description 설명(없으면 null)
 * @param status      진행 상태
 * @param assignee    담당자 정보(없으면 null)
 * @param version     동시 수정 충돌 감지에 쓰는 버전 번호
 * @param createdAt   생성 시각
 * @param updatedAt   마지막 수정 시각
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

	/**
	 * 담당자를 나타내는 중첩 record.
	 *
	 * 담당자 id만 내려주면 이름을 보여주기 위해 클라이언트가 사용자 조회를
	 * 한 번 더 해야 한다. 그렇다고 UserResponse를 통째로 넣으면 작업 목록에
	 * 이메일과 가입 시각까지 따라 나가 응답이 불필요하게 커진다.
	 * 화면에 필요한 두 값만 담는다.
	 *
	 * @param id   담당자 사용자 id
	 * @param name 담당자 이름
	 */
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

	/**
	 * 엔티티를 응답 DTO로 변환한다.
	 *
	 * 담당자가 없는 작업이 있으므로 null 검사를 거친다.
	 * 담당자와 프로젝트는 모두 지연 로딩이라 이 변환 시점에 조회가 나갈 수 있다.
	 * 그래서 이 메서드는 반드시 트랜잭션 안에서(=서비스 계층에서) 호출한다.
	 * open-in-view가 꺼져 있어, 컨트롤러로 나간 뒤에 호출하면
	 * LazyInitializationException이 발생한다.
	 *
	 * getProject().getId()는 프록시의 식별자만 읽는 것이라 실제 조회를 유발하지 않는다.
	 */
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
