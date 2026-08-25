import { useCallback, useEffect, useState } from 'react'
import * as api from '../api'
import { ROLE, TASK_STATUS } from '../labels'
import TaskDetail from './TaskDetail'

const PAGE_SIZE = 10

/**
 * 오른쪽 본문 — 고른 프로젝트의 작업 목록.
 * 검색·상태 필터·페이징·작업 생성을 한 화면에서 다루고, 행을 누르면 상세 창이 열린다.
 */
export default function TaskBoard({ userId, project }) {
	const [members, setMembers] = useState([])
	const [page, setPage] = useState(null)     // 스프링 Page 객체 그대로
	const [keywordDraft, setKeywordDraft] = useState('')
	const [keyword, setKeyword] = useState('') // 실제로 서버에 보낸 검색어
	const [status, setStatus] = useState('')
	const [pageNo, setPageNo] = useState(0)
	const [selected, setSelected] = useState(null)
	const [error, setError] = useState(null)
	const [loading, setLoading] = useState(true)

	const [newTitle, setNewTitle] = useState('')
	const [newAssigneeId, setNewAssigneeId] = useState('')

	/**
	 * 현재 조건으로 목록을 다시 불러온다. 수정·삭제 뒤에도 이것을 부른다.
	 * 응답을 화면에서 직접 갈아끼우지 않는 이유: 상태가 바뀌면 필터 조건에서 빠져야 할 수도 있다.
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

	// 멤버 목록은 검색 조건과 무관해 프로젝트가 바뀔 때만 부른다.
	useEffect(() => {
		api.getMembers(userId, project.id).then(setMembers).catch(() => setMembers([]))
	}, [userId, project.id])

	/** 규칙은 백엔드와 같다. 차단은 서버가 하고, 여기서는 눌러야 403이 날 버튼을 숨길 뿐이다. */
	const canEdit = (task) =>
		project.myRole === 'OWNER' || project.myRole === 'ADMIN' || task.assignee?.id === userId

	// 조건이 바뀌면 보던 페이지 번호는 의미가 없으므로 첫 페이지로 돌아간다.
	const submitSearch = (event) => {
		event.preventDefault()
		setKeyword(keywordDraft.trim())
		setPageNo(0)
	}

	const changeStatus = (value) => {
		setStatus(value)
		setPageNo(0)
	}

	/** 상태는 백엔드가 TODO로 시작시키므로 보내지 않는다. */
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
			// 기본 정렬이 생성 시각 내림차순이라 새 작업은 첫 페이지에 있다.
			if (pageNo === 0) await loadTasks()
			else setPageNo(0)
		} catch (e) {
			setError(e.message)
		}
	}

	const tasks = page?.content ?? []
	const totalPages = page?.totalPages ?? 0

	return (
		<section className="panel">
			<header className="board__header">
				<div>
					<h2>{project.name}</h2>
					<p className="hint">
						{project.description || '설명 없음'} · 내 역할 {ROLE[project.myRole]} · 멤버 {members.length}명
					</p>
				</div>
			</header>

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

			{/* 작업 생성은 프로젝트 멤버라면 누구나 할 수 있다 */}
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

			{/* 스프링 Page의 number는 0부터라 화면에서는 +1 해서 보여준다 */}
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
