package com.example.collab.exception;

import lombok.Getter;

/**
 * 이 서비스가 의도적으로 던지는 유일한 예외. 오류 종류는 ErrorCode로 전달한다.
 * 상태 코드 결정은 GlobalExceptionHandler가 하므로 서비스에는 HTTP 표현이 섞이지 않는다.
 */
@Getter
public class CollabException extends RuntimeException {

	private final ErrorCode errorCode;

	public CollabException(ErrorCode errorCode) {
		super(errorCode.getMessage());   // 로그·스택트레이스에 사유가 함께 찍히도록
		this.errorCode = errorCode;
	}
}
