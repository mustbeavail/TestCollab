import { useState } from 'react'
import * as api from '../api'
import { ROLE } from '../labels'

/**
 * 왼쪽 사이드바 — 내가 속한 프로젝트 목록과 새 프로젝트 만들기.
 * 역할 배지를 함께 보여준다. 같은 사용자도 프로젝트마다 역할이 달라, 오른쪽 화면에서
 * 어떤 버튼이 보이는지가 이 값으로 설명된다.
 */
export default function ProjectList({ projects, selectedId, loading, userId, onSelect, onCreated }) {
	const [name, setName] = useState('')
	const [description, setDescription] = useState('')
	const [error, setError] = useState(null)
	const [saving, setSaving] = useState(false)

	/**
	 * 만든 사람이 자동으로 OWNER가 된다. 만든 뒤 목록을 다시 불러오며 그 프로젝트를 골라 둔다.
	 * 설명을 비우면 null로 보낸다. 빈 문자열로 두면 "설명 없음"과 구분되는데 화면에서는 같다.
	 */
	const submit = async (event) => {
		event.preventDefault()
		if (!name.trim()) return

		setSaving(true)
		setError(null)
		try {
			const created = await api.createProject(userId, {
				name: name.trim(),
				description: description.trim() || null,
			})
			setName('')
			setDescription('')
			await onCreated(created.id)
		} catch (e) {
			setError(e.message)
		} finally {
			setSaving(false)
		}
	}

	return (
		<aside className="panel">
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
				<input
					type="text"
					placeholder="설명 (선택)"
					value={description}
					onChange={(e) => setDescription(e.target.value)}
					maxLength={500}
				/>
				<button type="submit" disabled={saving || !name.trim()}>
					{saving ? '만드는 중…' : '만들기'}
				</button>
			</form>
			{error && <p className="hint hint--error">{error}</p>}
		</aside>
	)
}
