package com.example.collab.exception;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 오류가 났을 때 클라이언트에게 내려주는 응답 본문.
 *
 * 오류 응답 형식을 한 가지로 통일해, 클라이언트가 상태 코드와 무관하게
 * 같은 구조로 파싱할 수 있게 한다.
 *
 * {
 *   "code": "NOT_PROJECT_MEMBER",
 *   "message": "프로젝트 멤버가 아닙니다."
 * }
 *
 * @param code    ErrorCode 상수의 이름. 클라이언트가 분기 처리에 쓰는 값이다.
 * @param message 사람이 읽을 설명. 화면에 그대로 띄울 수 있게 한글로 둔다.
 */
@Schema(description = "오류 응답")
public record ErrorResponse(

		@Schema(description = "오류 종류 식별자", example = "NOT_PROJECT_MEMBER")
		String code,

		@Schema(description = "오류 설명", example = "프로젝트 멤버가 아닙니다.")
		String message
) {

	/** ErrorCode에 정의된 기본 메시지를 그대로 쓰는 경우. */
	public static ErrorResponse of(ErrorCode errorCode) {
		return new ErrorResponse(errorCode.name(), errorCode.getMessage());
	}

	/**
	 * 기본 메시지 대신 상황별 설명을 붙이는 경우.
	 * 입력값 검증 실패처럼 "어떤 필드가 왜 잘못됐는지"를 함께 알려야 할 때 쓴다.
	 */
	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return new ErrorResponse(errorCode.name(), message);
	}
}
