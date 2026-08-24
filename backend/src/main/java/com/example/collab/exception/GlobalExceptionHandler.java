package com.example.collab.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 컨트롤러에서 빠져나온 예외를 잡아 오류 응답으로 바꾸는 곳.
 *
 * @RestControllerAdvice : 모든 @RestController에 공통으로 적용되는 예외 처리기임을 알린다.
 *                         (@ControllerAdvice + @ResponseBody, 즉 반환값이 JSON 본문이 된다)
 *
 * 예외 처리를 여기 모으는 이유:
 * 각 컨트롤러 메서드마다 try-catch를 두면 같은 코드가 스무 번 반복되고,
 * 새 엔드포인트를 추가할 때 빠뜨리기 쉽다. 서비스는 예외를 던지기만 하고
 * "그것을 몇 번 상태 코드로, 어떤 본문으로 내보낼지"는 이 클래스 한 곳에서 정한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 서비스가 의도적으로 던진 예외를 처리한다.
	 *
	 * 상태 코드와 메시지가 모두 ErrorCode에 들어 있으므로 여기서는 꺼내 조립하기만 한다.
	 * 의도된 흐름이라 로그는 경고 수준으로 짧게만 남긴다.
	 */
	@ExceptionHandler(CollabException.class)
	public ResponseEntity<ErrorResponse> handleCollabException(CollabException e) {
		ErrorCode errorCode = e.getErrorCode();
		log.warn("처리된 예외: {} - {}", errorCode.name(), errorCode.getMessage());

		return ResponseEntity
				.status(errorCode.getStatus())
				.body(ErrorResponse.of(errorCode));
	}

	/**
	 * 요청 DTO의 @Valid 검증에 걸렸을 때 처리한다.
	 *
	 * 스프링이 @NotBlank, @Size 같은 제약 위반을 발견하면 컨트롤러 메서드를
	 * 실행하지 않고 이 예외를 던진다. 그래서 서비스 계층은 이미 검증된 값만 받는다.
	 *
	 * 어떤 필드가 왜 잘못됐는지를 알려주지 않으면 클라이언트가 고칠 수가 없으므로,
	 * 위반 목록을 "필드명: 사유" 형태로 모아 메시지에 덧붙인다.
	 * (여러 필드가 동시에 걸릴 수 있어 하나만 보여주지 않는다)
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
		String detail = e.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining(", "));

		log.warn("입력값 검증 실패: {}", detail);

		return ResponseEntity
				.status(ErrorCode.INVALID_REQUEST.getStatus())
				.body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, detail));
	}

	/**
	 * 낙관적 락 충돌을 처리한다.
	 *
	 * 작업 수정 흐름에서는 서비스가 클라이언트의 version을 먼저 비교해
	 * 대부분의 충돌을 CollabException(TASK_VERSION_CONFLICT)으로 걸러낸다.
	 * 그래도 이 처리기가 필요한 이유는, 그 비교를 통과한 두 요청이
	 * 동시에 커밋을 시도하는 경우가 남기 때문이다. 그때는 하이버네이트가
	 * UPDATE ... WHERE version = ? 의 결과가 0건인 것을 보고 이 예외를 던진다.
	 *
	 * OptimisticLockingFailureException은 스프링이 하이버네이트의
	 * OptimisticLockException을 감싸서 올려주는 예외다. 특정 JPA 구현에
	 * 묶이지 않도록 스프링 쪽 타입으로 잡는다.
	 */
	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockingFailureException e) {
		log.warn("낙관적 락 충돌: {}", e.getMessage());

		return ResponseEntity
				.status(ErrorCode.TASK_VERSION_CONFLICT.getStatus())
				.body(ErrorResponse.of(ErrorCode.TASK_VERSION_CONFLICT));
	}
}
