package com.example.collab.dto.project;

import com.example.collab.domain.Project;
import com.example.collab.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 프로젝트 정보 응답 본문.
 *
 * myRole 필드가 있는 이유:
 * 같은 프로젝트라도 보는 사람에 따라 할 수 있는 일이 다르다. 이 값을 함께 내려주면
 * 프론트가 수정·삭제 버튼을 보여줄지 말지를 서버에 다시 묻지 않고 판단할 수 있다.
 * 프로젝트 자체의 속성이 아니라 "이 응답을 받는 사람"의 속성이라
 * 엔티티에서 바로 꺼낼 수 없고, 변환할 때 따로 넘겨받는다.
 *
 * @param id          프로젝트 식별자
 * @param name        이름
 * @param description 설명(없으면 null)
 * @param myRole      이 응답을 받는 사람의 역할
 * @param createdAt   생성 시각
 * @param updatedAt   마지막 수정 시각
 */
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

	/**
	 * 엔티티와 요청자의 역할을 합쳐 응답 DTO로 변환한다.
	 *
	 * @param project 변환할 프로젝트
	 * @param myRole  이 응답을 받는 사람의 역할. 호출하는 서비스가
	 *                권한 검사 과정에서 이미 조회해둔 ProjectMember에서 꺼내 넘긴다.
	 *                (역할을 얻으려고 조회를 한 번 더 하지 않는다)
	 */
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
