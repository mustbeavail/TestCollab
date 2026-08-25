/**
 * 백엔드 REST API 호출을 모아둔 곳.
 * 요청자 식별 헤더(X-User-Id)와 오류 응답 해석을 한 곳에서만 처리하려는 것이다.
 * 경로는 모두 /api로 시작하고, 개발 중에는 vite.config.js의 프록시가 8080으로 넘긴다.
 */

/** 상태 코드나 code로 분기해야 해서(예: 409 충돌) 문자열이 아니라 오류 클래스로 만든다. */
class ApiError extends Error {
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

/** 인증이 없어 요청자를 id로 지정해야 하는데, 화면이 그 id를 알 방법이 이것뿐이다. */
export const getUsers = () => request('/users')

export const createUser = (body) => request('/users', { method: 'POST', body })

export const getUser = (userId) => request(`/users/${userId}`)

// 프로젝트 · 멤버
export const createProject = (userId, body) => request('/projects', { method: 'POST', userId, body })

/** 각 항목에 요청자의 역할(myRole)이 들어 있다. */
export const getMyProjects = (userId) => request('/projects', { userId })

/** PATCH지만 두 값을 모두 보낸다. "안 보냄"과 "null로 비움"을 JSON에서 구별할 수 없다. */
export const updateProject = (userId, projectId, body) =>
	request(`/projects/${projectId}`, { method: 'PATCH', userId, body })

export const deleteProject = (userId, projectId) =>
	request(`/projects/${projectId}`, { method: 'DELETE', userId })

export const getMembers = (userId, projectId) =>
	request(`/projects/${projectId}/members`, { userId })

export const addMember = (userId, projectId, body) =>
	request(`/projects/${projectId}/members`, { method: 'POST', userId, body })

/** 마지막 OWNER를 낮추려 하면 409(LAST_OWNER)로 거절된다. */
export const changeMemberRole = (userId, projectId, targetUserId, body) =>
	request(`/projects/${projectId}/members/${targetUserId}`, { method: 'PATCH', userId, body })

/** 제거된 사람이 담당하던 작업은 지워지지 않고 담당자만 비워진다. */
export const removeMember = (userId, projectId, targetUserId) =>
	request(`/projects/${projectId}/members/${targetUserId}`, { method: 'DELETE', userId })

// 작업

/** 담당자 필터에서 "아직 담당자가 없는 작업"을 고른 상태. */
export const UNASSIGNED = 'none'

/**
 * 빈 문자열을 보내면 "빈 제목 검색"이 되므로, 값이 없는 조건은 쿼리에서 뺀다. page는 0부터다.
 * 담당자 필터는 화면에선 드롭다운 하나지만, "그 사람 담당"과 "담당자 없음"은 뜻이 다른
 * 조건이라 서버가 assigneeId와 unassignedOnly로 나눠 받는다.
 */
export const getTasks = (userId, projectId, { keyword, status, assigneeId, page = 0, size = 10 }) => {
	const params = new URLSearchParams({ page, size })
	if (keyword) params.set('keyword', keyword)
	if (status) params.set('status', status)
	if (assigneeId === UNASSIGNED) params.set('unassignedOnly', 'true')
	else if (assigneeId) params.set('assigneeId', assigneeId)
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
