import { userHint } from '../seedUsers'

/**
 * 요청자 식별자를 고르는 부분. 인증이 구현 대상이 아니라 로그인 화면 대신 계정을 고른다.
 * 여기서 정한 값이 모든 요청의 X-User-Id로 나가고, 역할에 따라 화면이 달라진다.
 * id를 직접 입력하게 하면 "3을 넣으면 무엇이 보이는지"를 알 수 없어, 설명과 함께 드롭다운으로 둔다.
 */
export default function UserSwitcher({ userId, users, onChange }) {
	// 목록을 아직 못 받았거나 고른 id가 목록에 없으면 select 값과 보이는 항목이 어긋난다.
	const isKnown = users.some((candidate) => candidate.id === userId)

	return (
		<div className="user-switcher">
			<label htmlFor="user-id">사용자</label>
			<select
				id="user-id"
				value={userId}
				onChange={(event) => onChange(Number(event.target.value))}
			>
				{!isKnown && <option value={userId}>{userId} · (목록에 없는 계정)</option>}
				{users.map((candidate) => (
					<option key={candidate.id} value={candidate.id}>
						{candidate.id} {candidate.name} ({userHint(candidate.id)})
					</option>
				))}
			</select>
		</div>
	)
}
