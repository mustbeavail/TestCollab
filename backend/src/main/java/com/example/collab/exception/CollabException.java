package com.example.collab.exception;

import lombok.Getter;

/**
 * 이 서비스가 의도적으로 던지는 유일한 예외.
 *
 * 어떤 오류인지는 ErrorCode에 담아 전달한다. 서비스 계층은
 * throw new CollabException(NOT_PROJECT_MEMBER) 한 줄로 흐름을 끊고,
 * 그것을 몇 번 상태 코드로 내보낼지는 GlobalExceptionHandler가 결정한다.
 * 덕분에 서비스 코드에 HTTP 관련 표현이 섞이지 않는다.
 *
 * RuntimeException을 상속하는 이유:
 * 검사 예외(Exception)로 만들면 호출하는 쪽마다 try-catch나 throws 선언을 강요받는다.
 * 여기서 던지는 예외는 대부분 그 자리에서 처리할 수 있는 종류가 아니라
 * 요청을 중단시키고 응답으로 바꿔야 하는 것이므로, 잡지 않고 위로 흘려보낸다.
 * 또한 스프링은 기본적으로 RuntimeException이 올라올 때만 트랜잭션을 롤백한다.
 */
@Getter
public class CollabException extends RuntimeException {

	private final ErrorCode errorCode;

	/**
	 * @param errorCode 발생한 오류의 종류. 상태 코드와 메시지를 함께 들고 있다.
	 */
	public CollabException(ErrorCode errorCode) {
		// 부모 생성자에 메시지를 넘겨두면 로그나 스택트레이스에 사유가 함께 찍힌다.
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
}
