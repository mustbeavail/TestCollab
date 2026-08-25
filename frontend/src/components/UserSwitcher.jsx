import { useState } from 'react'

/**
 * 헤더에서 "지금 누구로 API를 호출할지"를 고르는 부분.
 *
 * 이 과제는 인증이 구현 대상이 아니고 요청자 식별자를 파라미터로 받는다고 가정한다.
 * 그래서 로그인 화면 대신 사용자 ID를 직접 넣는 칸을 둔다.
 * 여기서 정한 값이 모든 요청의 X-User-Id 헤더로 나가고, 역할에 따라 화면이 달라진다.
 *
 * props
 *  userId   : 지금 적용된 사용자 ID
 *  user     : 그 ID로 조회한 사용자 정보. 조회에 실패했으면 null이다.
 *  onChange : 새 ID를 적용할 때 부르는 함수
 *
 * 입력값(draft)을 따로 들고 있는 이유:
 * 한 글자 칠 때마다 API를 부르면 존재하지 않는 ID로 요청이 계속 나간다.
 * 폼을 제출했을 때만 상위로 올려보낸다.
 */
export default function UserSwitcher({ userId, user, onChange }) {
	const [draft, setDraft] = useState(String(userId))

	/**
	 * 입력한 값을 적용한다.
	 *
	 * 정수이고 1 이상일 때만 올려보낸다. 소수점이나 음수는 사용자 ID가 될 수 없다.
	 * 올려보낸 뒤 입력칸도 해석된 값으로 맞춘다('01'처럼 쳤을 때
	 * 적용된 값과 보이는 값이 어긋나지 않게 한다).
	 */
	const submit = (event) => {
		event.preventDefault()
		const parsed = Number(draft)
		if (!Number.isInteger(parsed) || parsed < 1) return
		setDraft(String(parsed))
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
