package com.example.collab.exception;

import io.swagger.v3.oas.annotations.media.Schema;

/** 오류 응답 본문. 상태 코드와 무관하게 같은 구조로 파싱할 수 있도록 형식을 하나로 통일한다. */
@Schema(description = "오류 응답")
public record ErrorResponse(

		@Schema(description = "오류 종류 식별자", example = "NOT_PROJECT_MEMBER")
		String code,

		@Schema(description = "오류 설명", example = "프로젝트 멤버가 아닙니다.")
		String message
) {

	public static ErrorResponse of(ErrorCode errorCode) {
		return new ErrorResponse(errorCode.name(), errorCode.getMessage());
	}

	/** 검증 실패처럼 "어떤 필드가 왜 잘못됐는지"를 덧붙여야 할 때 쓴다. */
	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return new ErrorResponse(errorCode.name(), message);
	}
}
