import { useCallback, useEffect, useState } from 'react'
import * as api from './api'
import ProjectList from './components/ProjectList'
import TaskBoard from './components/TaskBoard'
import UserSwitcher from './components/UserSwitcher'

/**
 * 최상위 컴포넌트. 헤더(사용자 전환) + 왼쪽(프로젝트 목록) + 오른쪽(작업 목록) 구성이다.
 * 라우터를 두지 않은 이유: 화면 전환이 "프로젝트를 고른다" 하나뿐이라 주소로 나눌 대상이 없다.
 */
function App() {
	const [userId, setUserId] = useState(1)
	const [user, setUser] = useState(null)
	const [projects, setProjects] = useState([])
	const [projectId, setProjectId] = useState(null)
	const [error, setError] = useState(null)
	const [loading, setLoading] = useState(true)

	/** 새 프로젝트를 만든 직후 그것을 바로 열어주려고 고를 id를 받는다. */
	const loadProjects = useCallback(
		async (nextProjectId) => {
			const list = await api.getMyProjects(userId)
			setProjects(list)
			setProjectId(nextProjectId ?? list[0]?.id ?? null)
		},
		[userId],
	)

	/** cancelled : 응답이 오기 전에 사용자를 또 바꾸면, 먼저 보낸 요청이 최신 화면을 덮어쓴다. */
	useEffect(() => {
		let cancelled = false

		setLoading(true)
		setError(null)
		setUser(null)
		setProjects([])
		setProjectId(null)

		Promise.all([api.getUser(userId), api.getMyProjects(userId)])
			.then(([loadedUser, list]) => {
				if (cancelled) return
				setUser(loadedUser)
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

	return (
		<>
			<header className="header">
				<div className="header__title">
					<h1>Project Collab</h1>
					<a href="http://localhost:8080/swagger-ui.html" target="_blank" rel="noreferrer">
						API 문서
					</a>
				</div>
				<UserSwitcher userId={userId} user={user} onChange={setUserId} />
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
						project={selectedProject}
					/>
				) : (
					<section className="panel empty">
						{loading ? '불러오는 중…' : '왼쪽에서 프로젝트를 고르세요. 속한 프로젝트가 없다면 새로 만들 수 있습니다.'}
					</section>
				)}
			</div>
		</>
	)
}

export default App
