package com.example.collab.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 컨트롤러에서 빠져나온 예외를 오류 응답으로 바꾸는 곳.
 * 서비스는 예외를 던지기만 하고, 상태 코드와 응답 본문은 여기 한 곳에서 정한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/** 서비스가 의도적으로 던진 예외. 상태와 메시지가 ErrorCode에 있어 꺼내 조립만 한다. */
	@ExceptionHandler(CollabException.class)
	public ResponseEntity<ErrorResponse> handleCollabException(CollabException e) {
		ErrorCode errorCode = e.getErrorCode();
		log.warn("처리된 예외: {} - {}", errorCode.name(), errorCode.getMessage());

		return ResponseEntity
				.status(errorCode.getStatus())
				.body(ErrorResponse.of(errorCode));
	}

	/** @Valid 검증 실패. 여러 필드가 동시에 걸릴 수 있어 위반 목록을 모아 메시지에 덧붙인다. */
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
	 * 낙관적 락 충돌. 서비스의 version 비교를 통과한 두 요청이 실제로 동시에 커밋할 때 남는 경우다.
	 * 특정 JPA 구현에 묶이지 않도록 스프링이 감싸주는 타입으로 잡는다.
	 */
	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockingFailureException e) {
		log.warn("낙관적 락 충돌: {}", e.getMessage());

		return ResponseEntity
				.status(ErrorCode.TASK_VERSION_CONFLICT.getStatus())
				.body(ErrorResponse.of(ErrorCode.TASK_VERSION_CONFLICT));
	}
}
