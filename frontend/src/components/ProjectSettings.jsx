import { useState } from 'react'
import * as api from '../api'
import { ROLE } from '../labels'
import { userHint } from '../seedUsers'
import Modal from './Modal'

/**
 * 프로젝트 설정 창 — 수정·삭제와 멤버 관리.
 * 셋을 한 창에 둔 이유: 모두 "이 프로젝트를 어떻게 운영할지"에 대한 일이고 다루는 사람도 같다.
 *
 * 권한에 따라 입력칸을 잠근다. 실제 차단은 서버가 하고, 여기서는 눌러야 403이 날 것을 숨길 뿐이다.
 *   수정 OWNER·ADMIN / 삭제 OWNER / 멤버 관리 OWNER·ADMIN, 단 OWNER 임명·해임은 OWNER만
 */
export default function ProjectSettings({
	userId, users, project, members, onClose, onChanged, onDeleted, onMembersChanged,
}) {
	const [name, setName] = useState(project.name)
	const [description, setDescription] = useState(project.description ?? '')
	const [newMemberId, setNewMemberId] = useState('')
	const [newMemberRole, setNewMemberRole] = useState('MEMBER')
	const [error, setError] = useState(null)
	const [busy, setBusy] = useState(false)

	const isOwner = project.myRole === 'OWNER'
	const canManage = isOwner || project.myRole === 'ADMIN'

	/** 이미 멤버인 사람은 뺀다. 남겨두면 골라도 409(이미 멤버)를 받을 뿐이다. */
	const candidates = users.filter(
		(user) => !members.some((member) => member.userId === user.id),
	)

	/** 여섯 동작이 모두 "진행 중 표시 → 호출 → 실패 시 서버 메시지"라 껍데기를 공유한다. */
	const run = async (action) => {
		setBusy(true)
		setError(null)
		try {
			await action()
		} catch (e) {
			setError(e.message)
		} finally {
			setBusy(false)
		}
	}

	const saveProject = (event) => {
		event.preventDefault()
		run(async () => {
			await api.updateProject(userId, project.id, { name: name.trim(), description })
			await onChanged()
		})
	}

	/** 딸린 작업까지 사라지므로 확인을 먼저 받는다. */
	const removeProject = () =>
		run(async () => {
			if (!window.confirm(
				`프로젝트 "${project.name}"을(를) 삭제할까요?\n이 프로젝트의 작업과 멤버가 함께 사라지며 되돌릴 수 없습니다.`,
			)) return
			await api.deleteProject(userId, project.id)
			await onDeleted()
		})

	const submitAddMember = (event) => {
		event.preventDefault()
		run(async () => {
			await api.addMember(userId, project.id, {
				userId: Number(newMemberId),
				role: newMemberRole,
			})
			setNewMemberId('')
			setNewMemberRole('MEMBER')
			await onMembersChanged()
		})
	}

	const changeRole = (targetUserId, role) =>
		run(async () => {
			await api.changeMemberRole(userId, project.id, targetUserId, { role })
			await onMembersChanged()
		})

	/** 확인 문구에 "작업은 남는다"를 적는다. 그 사람 작업까지 지워질까 봐 주저하게 되기 때문이다. */
	const kickMember = (member) =>
		run(async () => {
			if (!window.confirm(
				`${member.name}님을 이 프로젝트에서 제거할까요?\n그가 담당하던 작업은 지워지지 않고 담당자만 비워집니다.`,
			)) return
			await api.removeMember(userId, project.id, member.userId)
			await onMembersChanged()
		})

	/** 대상이 OWNER면 해임에 해당하므로 OWNER만 건드릴 수 있다. */
	const canTouch = (member) => canManage && (isOwner || member.role !== 'OWNER')

	/** OWNER 임명은 OWNER만 하므로 ADMIN에게는 선택지에서 뺀다. */
	const roleOptions = Object.entries(ROLE).filter(([value]) => isOwner || value !== 'OWNER')

	return (
		<Modal title="프로젝트 설정" onClose={onClose}>
			<div className="modal__body">
				<form onSubmit={saveProject} className="settings__section">
					<label>
						프로젝트 이름
						<input
							type="text"
							value={name}
							onChange={(e) => setName(e.target.value)}
							maxLength={100}
							disabled={!canManage}
							required
						/>
					</label>

					<label>
						설명
						<textarea
							rows={2}
							value={description}
							onChange={(e) => setDescription(e.target.value)}
							maxLength={500}
							disabled={!canManage}
						/>
					</label>

					{canManage ? (
						<div className="modal__actions">
							<button type="submit" disabled={busy || !name.trim()}>
								정보 저장
							</button>
						</div>
					) : (
						<p className="hint">소유자·관리자만 프로젝트 정보를 고칠 수 있습니다.</p>
					)}
				</form>

				<section className="settings__section">
					<h4>멤버 {members.length}명</h4>

					<ul className="member-list">
						{members.map((member) => (
							<li key={member.userId}>
								<span className="member__name">
									{member.name}
									<span className="muted"> #{member.userId} · {member.email}</span>
								</span>

								{canTouch(member) ? (
									<select
										value={member.role}
										onChange={(e) => changeRole(member.userId, e.target.value)}
										disabled={busy}
										aria-label={`${member.name} 역할`}
									>
										{roleOptions.map(([value, label]) => (
											<option key={value} value={value}>
												{label}
											</option>
										))}
									</select>
								) : (
									<span className={`badge badge--${member.role.toLowerCase()}`}>
										{ROLE[member.role]}
									</span>
								)}

								{canTouch(member) && (
									<button
										type="button"
										className="danger"
										onClick={() => kickMember(member)}
										disabled={busy}
									>
										제거
									</button>
								)}
							</li>
						))}
					</ul>

					{canManage && (
						<form className="member-add" onSubmit={submitAddMember}>
							<select
								value={newMemberId}
								onChange={(e) => setNewMemberId(e.target.value)}
								aria-label="추가할 사용자"
								disabled={candidates.length === 0}
								required
							>
								<option value="">
									{candidates.length === 0
										? '추가할 수 있는 사용자가 없습니다'
										: '추가할 사용자 선택'}
								</option>
								{candidates.map((user) => (
									<option key={user.id} value={user.id}>
										{user.id} {user.name} ({userHint(user.id)})
									</option>
								))}
							</select>
							<select
								value={newMemberRole}
								onChange={(e) => setNewMemberRole(e.target.value)}
								aria-label="부여할 역할"
							>
								{roleOptions.map(([value, label]) => (
									<option key={value} value={value}>
										{label}
									</option>
								))}
							</select>
							<button type="submit" disabled={busy || !newMemberId}>
								멤버 추가
							</button>
						</form>
					)}
				</section>

				{error && <p className="banner banner--error">{error}</p>}

				{isOwner && (
					<section className="settings__section settings__danger">
						<div>
							<strong>프로젝트 삭제</strong>
							<p className="hint">작업과 멤버가 함께 사라집니다. 되돌릴 수 없습니다.</p>
						</div>
						<button type="button" className="danger" onClick={removeProject} disabled={busy}>
							프로젝트 삭제
						</button>
					</section>
				)}
			</div>
		</Modal>
	)
}
