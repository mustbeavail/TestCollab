package com.example.collab.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 프로젝트 생성 요청 본문. (POST /api/projects)
 *
 * 만든 사람을 이 DTO에 담지 않는 이유:
 * 요청자는 X-User-Id 헤더로 따로 들어온다. 본문에 넣으면 "헤더의 나"와
 * "본문에 적은 나"가 어긋날 수 있고, 그러면 어느 쪽을 믿을지 정해야 한다.
 * 요청자 식별은 항상 헤더 한 곳에서만 온다는 규칙을 지킨다.
 *
 * @param name        프로젝트 이름
 * @param description 설명. 없어도 되는 값이라 검증에서 @NotBlank를 빼고 길이만 제한한다.
 */
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
