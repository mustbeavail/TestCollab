/**
 * 백엔드 REST API를 호출하는 얇은 래퍼.
 *
 * 화면 컴포넌트가 fetch를 직접 부르지 않고 이 파일의 함수만 쓰도록 모아 둔 이유:
 * - 요청자 식별 헤더(X-User-Id)를 매 호출마다 손으로 붙이면 빠뜨리기 쉽다.
 * - 오류 응답({code, message})을 해석하는 코드가 화면마다 흩어진다.
 * 두 가지를 여기서 한 번만 처리한다.
 *
 * 요청 경로는 모두 '/api'로 시작한다. 개발 중에는 vite.config.js의 프록시가
 * 이 요청을 백엔드(localhost:8080)로 넘긴다.
 */

/**
 * 백엔드가 2xx가 아닌 응답을 준 경우 던지는 오류.
 *
 * status  : HTTP 상태 코드 (403, 404, 409 ...)
 * code    : 백엔드 ErrorCode 이름 (예: 'TASK_VERSION_CONFLICT')
 * message : 사용자에게 그대로 보여줄 수 있는 한글 설명
 *
 * 화면에서 "충돌이면 다르게 안내"처럼 분기하려면 상태 코드나 code가 필요해서
 * 문자열 하나가 아니라 별도 오류 클래스로 만든다.
 */
export class ApiError extends Error {
	constructor(status, code, message) {
		super(message)
		this.status = status
		this.code = code
	}
}

/**
 * 모든 API 호출이 거쳐 가는 공통 함수.
 *
 * @param path    '/projects' 처럼 '/api' 뒤에 붙는 경로
 * @param options method : HTTP 메서드 (기본 GET)
 *                userId : 요청자 식별자. 있으면 X-User-Id 헤더로 실린다.
 *                body   : 있으면 JSON으로 직렬화해 본문에 싣는다.
 * @returns 응답 본문을 파싱한 객체. 204(본문 없음)면 null.
 *
 * 동작 순서:
 * 1) 헤더를 만든다. 본문이 있을 때만 Content-Type을 붙인다.
 * 2) 요청을 보낸다.
 * 3) 204면 파싱할 본문이 없으므로 바로 null을 돌려준다.
 * 4) 본문을 JSON으로 읽는다. 파싱에 실패하면(빈 본문 등) null로 둔다.
 * 5) 실패 응답이면 ApiError를 던진다. 화면은 try/catch로 한 곳에서 받는다.
 */
async function request(path, { method = 'GET', userId, body } = {}) {
	const response = await fetch(`/api${path}`, {
		method,
		headers: {
			...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
			...(userId === undefined ? {} : { 'X-User-Id': String(userId) }),
		},
		body: body === undefined ? undefined : JSON.stringify(body),
	})

	if (response.status === 204) return null

	const data = await response.json().catch(() => null)

	if (!response.ok) {
		throw new ApiError(
			response.status,
			data?.code ?? 'UNKNOWN',
			data?.message ?? '요청을 처리하지 못했습니다.',
		)
	}
	return data
}

// ================================================================
// 사용자
// ================================================================

/** 사용자 한 명을 조회한다. 화면 상단에서 "지금 누구로 보고 있는지"를 확인하는 데 쓴다. */
export const getUser = (userId) => request(`/users/${userId}`)

// ================================================================
// 프로젝트 · 멤버
// ================================================================

/** 프로젝트를 만든다. 누구나 만들 수 있고, 만든 사람이 자동으로 OWNER 멤버가 된다. */
export const createProject = (userId, body) => request('/projects', { method: 'POST', userId, body })

/** 요청자가 멤버로 속한 프로젝트만 돌려준다. 각 항목에 요청자의 역할(myRole)이 들어 있다. */
export const getMyProjects = (userId) => request('/projects', { userId })

/** 프로젝트의 멤버 목록. 담당자 선택 드롭다운을 채우는 데 쓴다. */
export const getMembers = (userId, projectId) =>
	request(`/projects/${projectId}/members`, { userId })

// ================================================================
// 작업
// ================================================================

/**
 * 작업 목록을 조회한다.
 *
 * keyword·status는 값이 없으면 아예 쿼리에 넣지 않는다.
 * 빈 문자열을 보내면 "빈 제목을 검색"하는 뜻이 되어 조건이 걸리기 때문이다.
 *
 * page는 0부터 시작한다(스프링 Pageable 규칙). 화면에서는 +1 해서 보여준다.
 */
export const getTasks = (userId, projectId, { keyword, status, page = 0, size = 10 }) => {
	const params = new URLSearchParams({ page, size })
	if (keyword) params.set('keyword', keyword)
	if (status) params.set('status', status)
	return request(`/projects/${projectId}/tasks?${params}`, { userId })
}

/** 작업 하나를 조회한다. 수정 충돌(409) 뒤 최신 내용을 다시 받아올 때 쓴다. */
export const getTask = (userId, projectId, taskId) =>
	request(`/projects/${projectId}/tasks/${taskId}`, { userId })

/** 작업을 만든다. 상태는 백엔드가 항상 TODO로 시작시킨다. */
export const createTask = (userId, projectId, body) =>
	request(`/projects/${projectId}/tasks`, { method: 'POST', userId, body })

/**
 * 작업을 수정한다.
 *
 * body에는 조회 때 받은 version을 그대로 실어야 한다.
 * 그 사이 다른 사용자가 먼저 수정했다면 백엔드가 409(TASK_VERSION_CONFLICT)로 거절한다.
 */
export const updateTask = (userId, projectId, taskId, body) =>
	request(`/projects/${projectId}/tasks/${taskId}`, { method: 'PATCH', userId, body })

/** 작업을 삭제한다. 응답 본문이 없어 null이 돌아온다. */
export const deleteTask = (userId, projectId, taskId) =>
	request(`/projects/${projectId}/tasks/${taskId}`, { method: 'DELETE', userId })
