import { useState } from 'react'

/**
 * 요청자 식별자를 고르는 부분. 인증이 구현 대상이 아니라 로그인 화면 대신 사용자 ID를 직접 넣는다.
 * 여기서 정한 값이 모든 요청의 X-User-Id로 나가고, 역할에 따라 화면이 달라진다.
 */
export default function UserSwitcher({ userId, user, onChange }) {
	// 입력 중인 값을 따로 든다. 한 글자마다 올려보내면 없는 ID로 요청이 계속 나간다.
	const [draft, setDraft] = useState(String(userId))

	const submit = (event) => {
		event.preventDefault()
		const parsed = Number(draft)
		if (!Number.isInteger(parsed) || parsed < 1) return
		setDraft(String(parsed))   // '01'처럼 쳤을 때 적용된 값과 보이는 값을 맞춘다
		onChange(parsed)
	}

	return (
		<form className="user-switcher" onSubmit={submit}>
			<label htmlFor="user-id">사용자 ID</label>
			<input
				id="user-id"
				type="number"
				min="1"
				value={draft}
				onChange={(e) => setDraft(e.target.value)}
			/>
			<button type="submit">전환</button>
			<span className="user-switcher__name">
				{user ? `${user.name} (${user.email})` : '조회되지 않는 사용자'}
			</span>
		</form>
	)
}
