import { useState } from 'react'
import * as api from '../api'
import { ROLE } from '../labels'

/**
 * 왼쪽 사이드바 — 내가 속한 프로젝트 목록과 새 프로젝트 만들기.
 *
 * 목록에는 프로젝트 이름과 "이 프로젝트에서 내 역할"을 함께 보여준다.
 * 같은 사용자라도 프로젝트마다 역할이 다를 수 있어, 역할을 봐야
 * 오른쪽 화면에서 어떤 버튼이 보이는지가 납득된다.
 *
 * props
 *  projects   : 프로젝트 목록 (각 항목에 myRole이 들어 있다)
 *  selectedId : 지금 고른 프로젝트 ID
 *  loading    : 목록을 불러오는 중인지
 *  userId     : 요청자 식별자 (새 프로젝트 생성에 쓴다)
 *  onSelect   : 프로젝트를 고를 때 부르는 함수
 *  onCreated  : 새 프로젝트를 만든 뒤 목록을 다시 불러오도록 부르는 함수
 */
export default function ProjectList({ projects, selectedId, loading, userId, onSelect, onCreated }) {
	const [name, setName] = useState('')
	const [error, setError] = useState(null)
	const [saving, setSaving] = useState(false)

	/**
	 * 새 프로젝트를 만든다.
	 *
	 * 순서: 1) 이름이 비었으면 아무것도 하지 않는다
	 *       2) POST /api/projects (만든 사람이 자동으로 OWNER가 된다)
	 *       3) 목록을 다시 불러오면서 방금 만든 프로젝트를 골라 둔다
	 */
	const submit = async (event) => {
		event.preventDefault()
		if (!name.trim()) return

		setSaving(true)
		setError(null)
		try {
			const created = await api.createProject(userId, { name: name.trim(), description: '' })
			setName('')
			await onCreated(created.id)
		} catch (e) {
			setError(e.message)
		} finally {
			setSaving(false)
		}
	}

	return (
		<aside className="panel sidebar">
			<h2>내 프로젝트</h2>

			{loading ? (
				<p className="hint">불러오는 중…</p>
			) : projects.length === 0 ? (
				<p className="hint">속한 프로젝트가 없습니다.</p>
			) : (
				<ul className="project-list">
					{projects.map((project) => (
						<li key={project.id}>
							<button
								type="button"
								className={project.id === selectedId ? 'project is-selected' : 'project'}
								onClick={() => onSelect(project.id)}
							>
								<span className="project__name">{project.name}</span>
								<span className={`badge badge--${project.myRole.toLowerCase()}`}>
									{ROLE[project.myRole]}
								</span>
							</button>
						</li>
					))}
				</ul>
			)}

			<form className="project-create" onSubmit={submit}>
				<input
					type="text"
					placeholder="새 프로젝트 이름"
					value={name}
					onChange={(e) => setName(e.target.value)}
					maxLength={100}
				/>
				<button type="submit" disabled={saving || !name.trim()}>
					{saving ? '만드는 중…' : '만들기'}
				</button>
			</form>
			{error && <p className="hint hint--error">{error}</p>}
		</aside>
	)
}
