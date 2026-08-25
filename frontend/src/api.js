/**
 * 백엔드 REST API 호출을 모아둔 곳.
 * 요청자 식별 헤더(X-User-Id)와 오류 응답 해석을 한 곳에서만 처리하려는 것이다.
 * 경로는 모두 /api로 시작하고, 개발 중에는 vite.config.js의 프록시가 8080으로 넘긴다.
 */

/** 상태 코드나 code로 분기해야 해서(예: 409 충돌) 문자열이 아니라 오류 클래스로 만든다. */
export class ApiError extends Error {
	constructor(status, code, message) {
		super(message)
		this.status = status
		this.code = code
	}
}

/** 204는 파싱할 본문이 없어 null을 돌려주고, 실패 응답은 ApiError로 바꿔 던진다. */
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

// 사용자
export const getUser = (userId) => request(`/users/${userId}`)

// 프로젝트 · 멤버
export const createProject = (userId, body) => request('/projects', { method: 'POST', userId, body })

/** 각 항목에 요청자의 역할(myRole)이 들어 있다. */
export const getMyProjects = (userId) => request('/projects', { userId })

export const getMembers = (userId, projectId) =>
	request(`/projects/${projectId}/members`, { userId })

// 작업

/** 빈 문자열을 보내면 "빈 제목 검색"이 되므로, 값이 없는 조건은 쿼리에서 뺀다. page는 0부터다. */
export const getTasks = (userId, projectId, { keyword, status, page = 0, size = 10 }) => {
	const params = new URLSearchParams({ page, size })
	if (keyword) params.set('keyword', keyword)
	if (status) params.set('status', status)
	return request(`/projects/${projectId}/tasks?${params}`, { userId })
}

export const getTask = (userId, projectId, taskId) =>
	request(`/projects/${projectId}/tasks/${taskId}`, { userId })

export const createTask = (userId, projectId, body) =>
	request(`/projects/${projectId}/tasks`, { method: 'POST', userId, body })

/** body의 version이 서버와 다르면 409로 거절된다. */
export const updateTask = (userId, projectId, taskId, body) =>
	request(`/projects/${projectId}/tasks/${taskId}`, { method: 'PATCH', userId, body })

export const deleteTask = (userId, projectId, taskId) =>
	request(`/projects/${projectId}/tasks/${taskId}`, { method: 'DELETE', userId })
