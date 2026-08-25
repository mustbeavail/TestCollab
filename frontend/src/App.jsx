import { useCallback, useEffect, useState } from 'react'
import * as api from './api'
import ProjectList from './components/ProjectList'
import TaskBoard from './components/TaskBoard'
import UserSwitcher from './components/UserSwitcher'

/**
 * 애플리케이션의 최상위 컴포넌트.
 *
 * 화면은 세 부분이다.
 *  - 상단 헤더 : 지금 누구로 보고 있는지(요청자 식별자)를 고르는 곳
 *  - 왼쪽      : 그 사용자가 속한 프로젝트 목록
 *  - 오른쪽    : 고른 프로젝트의 작업 목록
 *
 * 이 컴포넌트가 직접 들고 있는 상태:
 *  userId    : 모든 API 요청에 X-User-Id로 실리는 값. 이게 바뀌면 화면 전체가 다시 그려진다.
 *  user      : userId로 조회한 사용자 정보(이름·이메일). 헤더에 보여줘 잘못된 id를 바로 알아채게 한다.
 *  projects  : 내가 속한 프로젝트 목록
 *  projectId : 지금 고른 프로젝트
 *  error     : 사용자 조회나 프로젝트 목록 조회가 실패했을 때의 안내 문구
 *
 * 라우터를 두지 않은 이유: 화면 전환이 "프로젝트를 고른다" 하나뿐이라 주소로 나눌 대상이 없다.
 * 화면이 늘어나 주소 공유·뒤로가기가 필요해지면 그때 react-router를 넣는다.
 */
function App() {
	const [userId, setUserId] = useState(1)
	const [user, setUser] = useState(null)
	const [projects, setProjects] = useState([])
	const [projectId, setProjectId] = useState(null)
	const [error, setError] = useState(null)
	const [loading, setLoading] = useState(true)

	/**
	 * 프로젝트 목록을 다시 불러온다.
	 *
	 * @param nextProjectId 불러온 뒤 골라 둘 프로젝트. 넘기지 않으면 첫 번째를 고른다.
	 *                      새 프로젝트를 만든 직후 그 프로젝트를 바로 열어주려고 받는다.
	 *
	 * useCallback으로 감싼 이유: 이 함수를 자식(ProjectList)에 넘기는데,
	 * 매번 새 함수가 만들어지면 아래 useEffect의 의존성이 계속 바뀌어 재조회가 반복된다.
	 */
	const loadProjects = useCallback(
		async (nextProjectId) => {
			const list = await api.getMyProjects(userId)
			setProjects(list)
			setProjectId(nextProjectId ?? list[0]?.id ?? null)
		},
		[userId],
	)

	/**
	 * userId가 바뀔 때마다 사용자 정보와 프로젝트 목록을 함께 불러온다.
	 *
	 * 두 요청을 Promise.all로 동시에 보내는 이유: 서로 의존하지 않으므로
	 * 순서대로 기다릴 이유가 없다.
	 *
	 * cancelled 플래그는 응답이 도착하기 전에 사용자가 또 id를 바꾼 경우를 막는다.
	 * 먼저 보낸 요청이 늦게 도착해 최신 화면을 덮어쓰는 것을 방지한다.
	 */
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
		<div className="app">
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

				{/*
				 * key에 사용자와 프로젝트를 함께 넣는다.
				 * 둘 중 하나라도 바뀌면 TaskBoard가 새로 만들어져,
				 * 이전 프로젝트의 검색어·페이지 번호가 남아 있는 일이 없다.
				 */}
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
		</div>
	)
}

export default App
