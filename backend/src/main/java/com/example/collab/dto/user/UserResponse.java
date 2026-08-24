package com.example.collab.dto.user;

import com.example.collab.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 사용자 정보 응답 본문.
 *
 * User 엔티티를 그대로 내보내지 않고 이 DTO로 바꿔서 내보내는 이유:
 * - 엔티티를 직렬화하면 지연 로딩 프록시가 건드려지면서 의도치 않은 쿼리가 나갈 수 있다.
 * - 응답에 어떤 필드가 나갈지 통제할 수 없게 된다. 엔티티에 필드를 하나 추가하는
 *   순간 모든 API의 응답이 조용히 바뀐다.
 *
 * @param id        사용자 식별자. 다른 API의 X-User-Id 헤더에 넣는 값이다.
 * @param name      이름
 * @param email     이메일
 * @param createdAt 가입 시각
 */
@Schema(description = "사용자 정보")
public record UserResponse(

		@Schema(description = "사용자 ID", example = "1")
		Long id,

		@Schema(description = "이름", example = "김철수")
		String name,

		@Schema(description = "이메일", example = "chulsoo@example.com")
		String email,

		@Schema(description = "가입 시각")
		LocalDateTime createdAt
) {

	/**
	 * 엔티티를 응답 DTO로 변환한다.
	 *
	 * 변환 로직을 DTO 안에 정적 메서드로 두면, 어떤 필드가 어디서 오는지
	 * 한 파일 안에서 확인된다. 서비스마다 변환 코드를 흩어 두지 않기 위한 규칙이다.
	 */
	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getCreatedAt()
		);
	}
}
