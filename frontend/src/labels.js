/**
 * 백엔드가 내려주는 열거형 값(TaskStatus, Role)을 화면에 보여줄 한글 이름으로 바꾸는 표.
 *
 * 한글 이름을 컴포넌트마다 적으면 같은 값이 화면마다 다르게 불릴 수 있어 한 곳에 모은다.
 * 키는 백엔드 enum 이름과 정확히 같아야 한다.
 */

/** 작업 상태. 표시 순서(할 일 → 진행 중 → 완료)가 곧 필터 드롭다운의 순서가 된다. */
export const TASK_STATUS = {
	TODO: '할 일',
	IN_PROGRESS: '진행 중',
	DONE: '완료',
}

/** 프로젝트에서의 역할. */
export const ROLE = {
	OWNER: '소유자',
	ADMIN: '관리자',
	MEMBER: '멤버',
}
