import { useCallback, useEffect, useState } from 'react'
import * as api from '../api'
import { ROLE, TASK_STATUS } from '../labels'
import TaskDetail from './TaskDetail'

const PAGE_SIZE = 10

/**
 * 오른쪽 본문 — 고른 프로젝트의 작업 목록 화면.
 *
 * 한 화면에서 과제의 작업 요구사항을 모두 다룬다.
 *  - 검색(제목), 상태 필터, 페이징
 *  - 작업 생성
 *  - 행을 누르면 상세 창이 열리고 거기서 수정·삭제
 *
 * props
 *  userId  : 요청자 식별자
 *  project : 고른 프로젝트. project.myRole로 버튼을 보일지 판단한다.
 *
 * 이 컴포넌트가 들고 있는 상태:
 *  members  : 담당자 드롭다운을 채울 멤버 목록 (프로젝트가 바뀔 때 한 번만 불러온다)
 *  page     : 스프링이 내려준 Page 객체 그대로 (content·totalPages·totalElements)
 *  keyword  : 실제로 서버에 보낸 검색어. 입력 중인 값(draft)과 구분한다.
 *  status   : 상태 필터. 빈 문자열이면 조건을 걸지 않는다.
 *  pageNo   : 0부터 시작하는 페이지 번호
 *  selected : 상세 창에 띄운 작업. null이면 창이 닫힌 상태다.
 */
export default function TaskBoard({ userId, project }) {
	const [members, setMembers] = useState([])
	const [page, setPage] = useState(null)
	const [keywordDraft, setKeywordDraft] = useState('')
	const [keyword, setKeyword] = useState('')
	const [status, setStatus] = useState('')
	const [pageNo, setPageNo] = useState(0)
	const [selected, setSelected] = useState(null)
	const [error, setError] = useState(null)
	const [loading, setLoading] = useState(true)

	// 새 작업 입력칸
	const [newTitle, setNewTitle] = useState('')
	const [newAssigneeId, setNewAssigneeId] = useState('')

	/**
	 * 현재 검색 조건으로 작업 목록을 다시 불러온다.
	 *
	 * 수정·삭제 뒤에도 이 함수를 불러 목록을 최신 상태로 맞춘다.
	 * (수정 결과를 화면에서 직접 바꿔 끼우지 않는 이유: 상태를 바꾸면 필터 조건에서
	 *  빠져야 할 수도 있어, 서버가 판단한 목록을 그대로 받는 편이 정확하다.)
	 */
	const loadTasks = useCallback(async () => {
		setLoading(true)
		setError(null)
		try {
			setPage(await api.getTasks(userId, project.id, { keyword, status, page: pageNo, size: PAGE_SIZE }))
		} catch (e) {
			setError(e.message)
		} finally {
			setLoading(false)
		}
	}, [userId, project.id, keyword, status, pageNo])

	useEffect(() => {
		loadTasks()
	}, [loadTasks])

	// 멤버 목록은 검색 조건과 무관하므로 프로젝트가 바뀔 때만 불러온다.
	useEffect(() => {
		api.getMembers(userId, project.id).then(setMembers).catch(() => setMembers([]))
	}, [userId, project.id])

	/**
	 * 이 작업을 요청자가 수정·삭제할 수 있는지 판단한다.
	 * 규칙은 백엔드와 같다 — 담당자 본인이거나 OWNER·ADMIN.
	 *
	 * 프론트에서 다시 판단하는 이유는 버튼을 보일지 정하기 위해서다.
	 * 실제 차단은 서버가 하고, 여기서는 눌러봐야 403이 나는 버튼을 숨길 뿐이다.
	 */
	const canEdit = (task) =>
		project.myRole === 'OWNER' || project.myRole === 'ADMIN' || task.assignee?.id === userId

	/** 검색어를 적용한다. 조건이 바뀌면 보던 페이지 번호는 의미가 없으므로 첫 페이지로 돌아간다. */
	const submitSearch = (event) => {
		event.preventDefault()
		setKeyword(keywordDraft.trim())
		setPageNo(0)
	}

	const changeStatus = (value) => {
		setStatus(value)
		setPageNo(0)
	}

	/** 작업을 만든다. 상태는 백엔드가 TODO로 시작시키므로 여기서 보내지 않는다. */
	const submitCreate = async (event) => {
		event.preventDefault()
		if (!newTitle.trim()) return

		setError(null)
		try {
			await api.createTask(userId, project.id, {
				title: newTitle.trim(),
				description: '',
				assigneeId: newAssigneeId === '' ? null : Number(newAssigneeId),
			})
			setNewTitle('')
			setNewAssigneeId('')
			// 새 작업은 가장 최근 것이므로(기본 정렬이 생성 시각 내림차순) 첫 페이지에서 보인다.
			if (pageNo === 0) await loadTasks()
			else setPageNo(0)
		} catch (e) {
			setError(e.message)
		}
	}

	const tasks = page?.content ?? []
	const totalPages = page?.totalPages ?? 0

	return (
		<section className="panel board">
			<header className="board__header">
				<div>
					<h2>{project.name}</h2>
					<p className="hint">
						{project.description || '설명 없음'} · 내 역할 {ROLE[project.myRole]} · 멤버 {members.length}명
					</p>
				</div>
			</header>

			{/* 검색 · 상태 필터 */}
			<div className="filters">
				<form onSubmit={submitSearch}>
					<input
						type="search"
						placeholder="제목 검색"
						value={keywordDraft}
						onChange={(e) => setKeywordDraft(e.target.value)}
					/>
					<button type="submit">검색</button>
				</form>

				<select value={status} onChange={(e) => changeStatus(e.target.value)}>
					<option value="">상태 전체</option>
					{Object.entries(TASK_STATUS).map(([value, label]) => (
						<option key={value} value={value}>
							{label}
						</option>
					))}
				</select>
			</div>

			{/* 작업 추가 — 프로젝트 멤버라면 누구나 만들 수 있다 */}
			<form className="task-create" onSubmit={submitCreate}>
				<input
					type="text"
					placeholder="새 작업 제목"
					value={newTitle}
					onChange={(e) => setNewTitle(e.target.value)}
					maxLength={200}
				/>
				<select value={newAssigneeId} onChange={(e) => setNewAssigneeId(e.target.value)}>
					<option value="">담당자 미지정</option>
					{members.map((m) => (
						<option key={m.userId} value={m.userId}>
							{m.name}
						</option>
					))}
				</select>
				<button type="submit" disabled={!newTitle.trim()}>
					작업 추가
				</button>
			</form>

			{error && <p className="banner banner--error">{error}</p>}

			{/* 목록 */}
			{loading ? (
				<p className="hint">불러오는 중…</p>
			) : tasks.length === 0 ? (
				<p className="hint">조건에 맞는 작업이 없습니다.</p>
			) : (
				<table className="task-table">
					<thead>
						<tr>
							<th>제목</th>
							<th>상태</th>
							<th>담당자</th>
							<th>수정 시각</th>
						</tr>
					</thead>
					<tbody>
						{tasks.map((task) => (
							<tr key={task.id} onClick={() => setSelected(task)}>
								<td>{task.title}</td>
								<td>
									<span className={`badge badge--${task.status.toLowerCase()}`}>
										{TASK_STATUS[task.status]}
									</span>
								</td>
								<td>{task.assignee?.name ?? '—'}</td>
								<td className="muted">{task.updatedAt.slice(0, 16).replace('T', ' ')}</td>
							</tr>
						))}
					</tbody>
				</table>
			)}

			{/* 페이징 — 스프링 Page의 number는 0부터라 화면에서는 +1 해서 보여준다 */}
			{totalPages > 1 && (
				<div className="pager">
					<button type="button" onClick={() => setPageNo(pageNo - 1)} disabled={pageNo === 0}>
						이전
					</button>
					<span>
						{pageNo + 1} / {totalPages} 페이지 (총 {page.totalElements}건)
					</span>
					<button
						type="button"
						onClick={() => setPageNo(pageNo + 1)}
						disabled={pageNo >= totalPages - 1}
					>
						다음
					</button>
				</div>
			)}

			{/* 상세 창 — 행을 눌렀을 때만 뜬다 */}
			{selected && (
				<TaskDetail
					userId={userId}
					projectId={project.id}
					task={selected}
					members={members}
					editable={canEdit(selected)}
					onClose={() => setSelected(null)}
					onSaved={() => {
						setSelected(null)
						loadTasks()
					}}
				/>
			)}
		</section>
	)
}
