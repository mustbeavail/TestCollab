import { useEffect } from 'react'

/**
 * 창(모달)의 공통 껍데기. 작업 상세·사용자 등록·프로젝트 설정이 같은 방식으로 열고 닫힌다.
 * 마우스만 쓰는 사람과 키보드를 쓰는 사람 어느 쪽도 갇히지 않도록 배경·닫기 버튼·Esc 세 가지를 둔다.
 */
export default function Modal({ title, onClose, children }) {
	// 창 안 어디에 포커스가 있든 Esc가 먹도록 window에 달고, 닫힐 때 걷어낸다.
	useEffect(() => {
		const onKeyDown = (e) => {
			if (e.key === 'Escape') onClose()
		}
		window.addEventListener('keydown', onKeyDown)
		return () => window.removeEventListener('keydown', onKeyDown)
	}, [onClose])

	return (
		// 배경을 누르면 닫는다. 안쪽 클릭까지 닫히지 않도록 창에서 전파를 멈춘다.
		<div className="modal-backdrop" onClick={onClose}>
			<div className="modal" onClick={(e) => e.stopPropagation()}>
				<header className="modal__header">
					<h3>{title}</h3>
					<button type="button" className="icon-button" onClick={onClose} aria-label="닫기">
						×
					</button>
				</header>
				{children}
			</div>
		</div>
	)
}
