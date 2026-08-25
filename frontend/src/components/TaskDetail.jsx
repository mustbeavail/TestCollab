import { useEffect, useState } from 'react'
import * as api from '../api'
import { TASK_STATUS } from '../labels'

/**
 * 작업 상세·수정 창. 수정 권한이 없으면 같은 내용을 읽기 전용으로 보여준다.
 *
 * 동시 수정 처리: 조회 시점의 version을 들고 있다가 저장할 때 함께 보낸다.
 * 그 사이 남이 먼저 저장했으면 409로 거절되는데, 이때 입력을 지우지 않고
 * 경고와 [최신 내용 불러오기]를 띄운다. 덮어쓰기를 막으면서 친 내용도 잃지 않게 하려는 것이다.
 */
export default function TaskDetail({ userId, projectId, task, members, editable, onClose, onSaved }) {
	const [form, setForm] = useState({
		title: task.title,
		description: task.description ?? '',
		status: task.status,
		assigneeId: task.assignee?.id ?? '',
		version: task.version,
	})
	const [error, setError] = useState(null)
	const [conflict, setConflict] = useState(false)
	const [saving, setSaving] = useState(false)

	// Esc로 닫는다. 창이 사라질 때 리스너도 함께 걷어낸다.
	useEffect(() => {
		const onKeyDown = (e) => {
			if (e.key === 'Escape') onClose()
		}
		window.addEventListener('keydown', onKeyDown)
		return () => window.removeEventListener('keydown', onKeyDown)
	}, [onClose])

	const set = (key, value) => setForm((prev) => ({ ...prev, [key]: value }))

	/** 409면 창을 닫지 않고 충돌 안내만 띄운다. 그 밖의 오류는 서버 메시지를 그대로 보여준다. */
	const submit = async (event) => {
		event.preventDefault()
		setSaving(true)
		setError(null)
		setConflict(false)
		try {
			await api.updateTask(userId, projectId, task.id, {
				title: form.title.trim(),
				description: form.description,
				status: form.status,
				assigneeId: form.assigneeId === '' ? null : Number(form.assigneeId),
				version: form.version,
			})
			onSaved()
		} catch (e) {
			if (e.status === 409) setConflict(true)
			else setError(e.message)
		} finally {
			setSaving(false)
		}
	}

	/** 새 version까지 함께 받아, 최신 내용을 확인한 뒤 다시 저장할 수 있게 한다. */
	const reload = async () => {
		const latest = await api.getTask(userId, projectId, task.id)
		setForm({
			title: latest.title,
			description: latest.description ?? '',
			status: latest.status,
			assigneeId: latest.assignee?.id ?? '',
			version: latest.version,
		})
		setConflict(false)
	}

	/** 되돌릴 수 없는 동작이라 확인을 먼저 받는다. */
	const remove = async () => {
		if (!window.confirm(`작업 "${task.title}"을(를) 삭제할까요? 되돌릴 수 없습니다.`)) return
		try {
			await api.deleteTask(userId, projectId, task.id)
			onSaved()
		} catch (e) {
			setError(e.message)
		}
	}

	return (
		// 바깥을 누르면 닫는다. 안쪽 클릭은 창에서 전파를 막는다.
		<div className="modal-backdrop" onClick={onClose}>
			<div className="modal" onClick={(e) => e.stopPropagation()}>
				<header className="modal__header">
					<h3>작업 상세</h3>
					<button type="button" className="icon-button" onClick={onClose} aria-label="닫기">
						×
					</button>
				</header>

				<form className="modal__body" onSubmit={submit}>
					<label>
						제목
						<input
							type="text"
							value={form.title}
							onChange={(e) => set('title', e.target.value)}
							maxLength={200}
							disabled={!editable}
							required
						/>
					</label>

					<label>
						설명
						<textarea
							rows={4}
							value={form.description}
							onChange={(e) => set('description', e.target.value)}
							maxLength={2000}
							disabled={!editable}
						/>
					</label>

					<div className="modal__row">
						<label>
							상태
							<select
								value={form.status}
								onChange={(e) => set('status', e.target.value)}
								disabled={!editable}
							>
								{Object.entries(TASK_STATUS).map(([value, label]) => (
									<option key={value} value={value}>
										{label}
									</option>
								))}
							</select>
						</label>

						<label>
							담당자
							<select
								value={form.assigneeId}
								onChange={(e) => set('assigneeId', e.target.value)}
								disabled={!editable}
							>
								<option value="">미지정</option>
								{members.map((m) => (
									<option key={m.userId} value={m.userId}>
										{m.name}
									</option>
								))}
							</select>
						</label>
					</div>

					<p className="hint">version {form.version} · 이 값이 서버와 다르면 저장이 거절됩니다.</p>

					{conflict && (
						<div className="banner banner--warn">
							다른 사용자가 먼저 이 작업을 수정했습니다. 입력한 내용은 그대로 두었습니다.
							<button type="button" onClick={reload}>
								최신 내용 불러오기
							</button>
						</div>
					)}
					{error && <p className="banner banner--error">{error}</p>}

					{editable ? (
						<div className="modal__actions">
							<button type="button" className="danger" onClick={remove}>
								삭제
							</button>
							<button type="submit" disabled={saving || !form.title.trim()}>
								{saving ? '저장 중…' : '저장'}
							</button>
						</div>
					) : (
						<p className="hint">담당자 본인이거나 소유자·관리자만 수정·삭제할 수 있습니다.</p>
					)}
				</form>
			</div>
		</div>
	)
}
