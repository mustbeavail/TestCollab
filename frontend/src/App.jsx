import { useCallback, useEffect, useState } from 'react'
import * as api from './api'
import ProjectList from './components/ProjectList'
import TaskBoard from './components/TaskBoard'
import UserCreate from './components/UserCreate'
import UserSwitcher from './components/UserSwitcher'

/**
 * 최상위 컴포넌트. 헤더(사용자 전환) + 왼쪽(프로젝트 목록) + 오른쪽(작업 목록) 구성이다.
 * 라우터를 두지 않은 이유: 화면 전환이 "프로젝트를 고른다" 하나뿐이라 주소로 나눌 대상이 없다.
 */
function App() {
	const [userId, setUserId] = useState(1)
	const [users, setUsers] = useState([])
	const [projects, setProjects] = useState([])
	const [projectId, setProjectId] = useState(null)
	const [error, setError] = useState(null)
	const [loading, setLoading] = useState(true)
	const [creatingUser, setCreatingUser] = useState(false)

	/**
	 * 사용자 목록은 DB가 유일한 사실이라 매번 서버에 묻는다. 브라우저에 사본을 두면
	 * 다른 탭에서 등록한 계정이 안 보이거나, 서버를 다시 띄운 뒤에도 목록에 남는다.
	 */
	const loadUsers = useCallback(async () => {
		setUsers(await api.getUsers())
	}, [])

	useEffect(() => {
		loadUsers().catch((e) => setError(e.message))
	}, [loadUsers])

/**
	 * 새 프로젝트를 만든 직후 그것을 바로 열어주려고 고를 id를 받는다.
	 * 목록에 없는 id면(방금 삭제된 프로젝트 등) 첫 번째로 되돌린다.
	 */
	const loadProjects = useCallback(
		async (nextProjectId) => {
			const list = await api.getMyProjects(userId)
			setProjects(list)

			const stillExists = list.some((p) => p.id === nextProjectId)
			setProjectId(stillExists ? nextProjectId : (list[0]?.id ?? null))
		},
		[userId],
	)

	/** cancelled : 응답이 오기 전에 사용자를 또 바꾸면, 먼저 보낸 요청이 최신 화면을 덮어쓴다. */
	useEffect(() => {
		let cancelled = false

		setLoading(true)
		setError(null)
		setProjects([])
		setProjectId(null)

		api.getMyProjects(userId)
			.then((list) => {
				if (cancelled) return
				setProjects(list)
				setProjectId(list[0]?.id ?? null)
			})
			.catch((e) => {
				if (!cancelled) setError(e.message)
			})
			.finally(() => {
				if (!cancelled) setLoading(false)
			})

		return () => {
			cancelled = true
		}
	}, [userId])

	const selectedProject = projects.find((p) => p.id === projectId) ?? null

	/** 프로젝트 정보나 멤버가 바뀐 뒤. 보고 있던 프로젝트는 그대로 두려고 지금 id를 넘긴다. */
	const refreshProjects = useCallback(
		() => loadProjects(projectId),
		[loadProjects, projectId],
	)

	/** 등록한 계정이 드롭다운에 들어와 있어야 전환 결과가 제대로 보인다. */
	const registerUser = async (created) => {
		setCreatingUser(false)
		await loadUsers()
		setUserId(created.id)
	}

	return (
		<>
			<header className="header">
				<div className="header__title">
					<h1>Project Collab</h1>
					<a href="http://localhost:8080/swagger-ui.html" target="_blank" rel="noreferrer">
						API 문서
					</a>
				</div>
				<div className="header__user">
					<UserSwitcher userId={userId} users={users} onChange={setUserId} />
					<button type="button" onClick={() => setCreatingUser(true)}>
						사용자 등록
					</button>
				</div>
			</header>

			{error && <p className="banner banner--error">{error}</p>}

			<div className="layout">
				<ProjectList
					projects={projects}
					selectedId={projectId}
					loading={loading}
					userId={userId}
					onSelect={setProjectId}
					onCreated={loadProjects}
				/>

				{/* key에 사용자와 프로젝트를 넣어, 둘 중 하나만 바뀌어도 검색어·페이지가 남지 않게 한다. */}
				{selectedProject ? (
					<TaskBoard
						key={`${userId}-${selectedProject.id}`}
						userId={userId}
						users={users}
						project={selectedProject}
						onProjectChanged={refreshProjects}
						onProjectDeleted={loadProjects}
					/>
				) : (
					<section className="panel empty">
						{loading ? '불러오는 중…' : '왼쪽에서 프로젝트를 고르세요. 속한 프로젝트가 없다면 새로 만들 수 있습니다.'}
					</section>
				)}
			</div>

			{creatingUser && (
				<UserCreate onClose={() => setCreatingUser(false)} onCreated={registerUser} />
			)}
		</>
	)
}

export default App
