package com.example.collab.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 프로젝트 수정 요청 본문. (PATCH /api/projects/{projectId})
 *
 * 생성 요청과 필드가 같지만 별도 record로 둔다.
 * 하나로 합치면 앞으로 한쪽에만 필드를 추가해야 할 때 갈라내야 하고,
 * Swagger 문서에서도 생성과 수정이 같은 스키마로 묶여 구분이 흐려진다.
 *
 * 두 필드를 모두 받는 이유(PATCH인데 전체 값을 받는 것):
 * 부분 수정을 지원하려면 "값을 안 보냄"과 "null로 비움"을 구분해야 하는데,
 * JSON에서는 둘 다 null로 도착해 서버가 구별할 수 없다. 구별하려면
 * 필드마다 Optional을 감싸는 등 다루기 번거로운 장치가 필요하다.
 * 수정 화면이 두 값을 모두 들고 있는 구조라 전체를 받아 덮어쓰는 편이 단순하다.
 *
 * @param name        새 프로젝트 이름
 * @param description 새 설명(비우려면 null)
 */
@Schema(description = "프로젝트 수정 요청")
public record ProjectUpdateRequest(

		@Schema(description = "프로젝트 이름", example = "협업 서비스 개편 v2")
		@NotBlank(message = "프로젝트 이름은 필수입니다.")
		@Size(max = 100, message = "프로젝트 이름은 100자를 넘을 수 없습니다.")
		String name,

		@Schema(description = "프로젝트 설명", example = "범위를 조정했습니다.")
		@Size(max = 500, message = "설명은 500자를 넘을 수 없습니다.")
		String description
) {
}
