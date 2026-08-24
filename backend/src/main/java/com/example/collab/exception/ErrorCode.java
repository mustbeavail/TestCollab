package com.example.collab.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 이 서비스에서 발생할 수 있는 오류의 종류를 한곳에 모아둔 enum.
 *
 * 상황마다 예외 클래스를 따로 만들지 않고, 예외 클래스 하나(CollabException)에
 * 이 enum 값을 실어 보내는 방식을 쓴다. 오류 종류가 열 개 남짓인 규모에서
 * 클래스를 열 개 만드는 것은 얻는 것 없이 파일 수만 늘린다.
 *
 * 각 상수가 가진 값:
 * - status  : 이 오류를 어떤 HTTP 상태 코드로 응답할지
 * - message : 클라이언트에게 보여줄 한글 메시지
 *
 * 응답의 code 필드에는 상수 이름(예: "NOT_PROJECT_MEMBER")이 그대로 들어간다.
 * 사람이 읽는 message와 달리, 클라이언트가 분기 처리에 쓰라고 두는 값이다.
 * 메시지 문구를 고쳐도 code는 그대로라 클라이언트 코드가 깨지지 않는다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// --- 400 Bad Request : 요청 값 자체가 잘못된 경우 ---

	/** @Valid 검증에 걸렸을 때. 어떤 필드가 왜 잘못됐는지는 message에 덧붙여 내려준다. */
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),

	/**
	 * 작업의 담당자로 지정하려는 사용자가 그 프로젝트의 멤버가 아닌 경우.
	 *
	 * 권한 문제(403)가 아니라 입력값 문제(400)로 분류한다.
	 * 요청을 보낸 사람의 권한은 충분한데, 지목한 대상이 잘못된 상황이기 때문이다.
	 */
	ASSIGNEE_NOT_MEMBER(HttpStatus.BAD_REQUEST, "담당자는 해당 프로젝트의 멤버여야 합니다."),

	// --- 403 Forbidden : 요청자가 그 일을 할 수 없는 경우 ---

	/**
	 * 요청자가 그 프로젝트의 멤버가 아닌 경우.
	 *
	 * 프로젝트가 아예 존재하지 않는 경우도 이 오류로 응답한다(404가 아니다).
	 * 둘을 구분해 응답하면, 남의 프로젝트 id를 하나씩 넣어보는 것만으로
	 * "이 id의 프로젝트가 존재하는가"를 알아낼 수 있기 때문이다.
	 */
	NOT_PROJECT_MEMBER(HttpStatus.FORBIDDEN, "프로젝트 멤버가 아닙니다."),

	/** 멤버이긴 하나 역할이 모자란 경우(예: MEMBER가 프로젝트를 수정하려 할 때). */
	NO_PERMISSION(HttpStatus.FORBIDDEN, "이 작업을 수행할 권한이 없습니다."),

	// --- 404 Not Found : 대상이 없는 경우 ---

	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
	PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."),
	TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "작업을 찾을 수 없습니다."),

	// --- 409 Conflict : 요청 자체는 옳으나 현재 상태와 충돌하는 경우 ---

	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
	ALREADY_MEMBER(HttpStatus.CONFLICT, "이미 프로젝트 멤버입니다."),

	/** 마지막 남은 OWNER를 강등하거나 제거하려 한 경우. */
	LAST_OWNER(HttpStatus.CONFLICT, "프로젝트에는 최소 1명의 OWNER가 있어야 합니다."),

	/**
	 * 작업을 수정하는 사이 다른 사용자가 먼저 수정한 경우(낙관적 락 충돌).
	 *
	 * 클라이언트가 보낸 version과 DB의 현재 version이 다를 때,
	 * 또는 커밋 시점에 하이버네이트가 충돌을 감지했을 때 쓴다.
	 */
	TASK_VERSION_CONFLICT(HttpStatus.CONFLICT, "다른 사용자가 먼저 수정했습니다.");

	private final HttpStatus status;
	private final String message;
}
