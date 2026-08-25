import { useState } from 'react'
import * as api from '../api'
import Modal from './Modal'

/**
 * 사용자 등록 창.
 * 인증이 없는 과제라 "회원가입"이 아니라 요청자로 쓸 계정을 하나 늘리는 화면이다.
 * 그래서 비밀번호가 없고, 등록 직후 그 계정으로 전환한다(id를 손으로 다시 고를 이유가 없다).
 */
export default function UserCreate({ onClose, onCreated }) {
	const [name, setName] = useState('')
	const [email, setEmail] = useState('')
	const [error, setError] = useState(null)
	const [saving, setSaving] = useState(false)

	/**
	 * 형식 검사를 프론트에서 다시 하지 않는 이유: required와 type="email"이 명백한 실수를
	 * 걸러 주고, 진짜 판정은 서버의 Bean Validation이 한다. 양쪽에 두면 한쪽만 고쳐 어긋난다.
	 */
	const submit = async (event) => {
		event.preventDefault()
		setSaving(true)
		setError(null)
		try {
			onCreated(await api.createUser({ name: name.trim(), email: email.trim() }))
		} catch (e) {
			setError(e.message)
		} finally {
			setSaving(false)
		}
	}

	return (
		<Modal title="사용자 등록" onClose={onClose}>
			<form className="modal__body" onSubmit={submit}>
				<label>
					이름
					<input
						type="text"
						value={name}
						onChange={(e) => setName(e.target.value)}
						maxLength={50}
						required
					/>
				</label>

				<label>
					이메일
					<input
						type="email"
						value={email}
						onChange={(e) => setEmail(e.target.value)}
						maxLength={255}
						required
					/>
				</label>

				<p className="hint">등록하면 그 사용자로 화면이 전환됩니다. 이메일은 중복될 수 없습니다.</p>

				{error && <p className="banner banner--error">{error}</p>}

				<div className="modal__actions">
					<button type="submit" disabled={saving || !name.trim() || !email.trim()}>
						{saving ? '등록 중…' : '등록'}
					</button>
				</div>
			</form>
		</Modal>
	)
}
