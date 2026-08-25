package com.example.collab.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 오류 종류와 그에 대응하는 HTTP 상태·메시지를 한곳에 모아둔 enum.
 * 응답의 code에는 상수 이름이 그대로 나가, 메시지를 고쳐도 클라이언트 분기가 깨지지 않는다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// --- 400 : 요청 값이 잘못된 경우 ---
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),

	/** 요청자의 권한은 충분하고 지목한 대상이 잘못된 상황이라 403이 아니라 400이다. */
	ASSIGNEE_NOT_MEMBER(HttpStatus.BAD_REQUEST, "담당자는 해당 프로젝트의 멤버여야 합니다."),

	// --- 403 : 요청자가 그 일을 할 수 없는 경우 ---

	/** 프로젝트가 없는 경우도 이 오류로 응답한다. 404와 구분하면 id를 넣어보며 존재 여부를 알아낼 수 있다. */
	NOT_PROJECT_MEMBER(HttpStatus.FORBIDDEN, "프로젝트 멤버가 아닙니다."),
	NO_PERMISSION(HttpStatus.FORBIDDEN, "이 작업을 수행할 권한이 없습니다."),

	// --- 404 : 대상이 없는 경우 ---
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
	TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "작업을 찾을 수 없습니다."),

	// --- 409 : 요청은 옳으나 현재 상태와 충돌하는 경우 ---
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
	ALREADY_MEMBER(HttpStatus.CONFLICT, "이미 프로젝트 멤버입니다."),
	LAST_OWNER(HttpStatus.CONFLICT, "프로젝트에는 최소 1명의 OWNER가 있어야 합니다."),
	TASK_VERSION_CONFLICT(HttpStatus.CONFLICT, "다른 사용자가 먼저 수정했습니다.");

	private final HttpStatus status;
	private final String message;
}
