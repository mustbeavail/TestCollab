package com.example.collab.dto.project;

import com.example.collab.domain.ProjectMember;
import com.example.collab.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 프로젝트 멤버 한 명의 정보 응답 본문.
 *
 * id가 ProjectMember의 식별자가 아니라 userId인 이유:
 * 멤버 관리 API의 경로가 /members/{userId} 형태다. 클라이언트가 역할 변경이나
 * 제거를 호출하려면 사용자 id가 필요하지, 연결 테이블의 행 번호는 쓸 일이 없다.
 * 내부 식별자를 굳이 밖으로 내보내지 않는다.
 *
 * @param userId   사용자 식별자
 * @param name     사용자 이름
 * @param email    사용자 이메일
 * @param role     이 프로젝트에서의 역할
 * @param joinedAt 합류 시각
 */
@Schema(description = "프로젝트 멤버 정보")
public record MemberResponse(

		@Schema(description = "사용자 ID", example = "3")
		Long userId,

		@Schema(description = "이름", example = "박멤버")
		String name,

		@Schema(description = "이메일", example = "member@example.com")
		String email,

		@Schema(description = "이 프로젝트에서의 역할", example = "MEMBER")
		Role role,

		@Schema(description = "합류 시각")
		LocalDateTime joinedAt
) {

	/**
	 * 엔티티를 응답 DTO로 변환한다.
	 *
	 * 이름과 이메일을 꺼내려면 연관된 User에 접근해야 한다. User는 지연 로딩이라
	 * 이 시점에 조회 쿼리가 나갈 수 있다. 멤버 목록처럼 여러 건을 변환할 때는
	 * 조회 단계에서 미리 join fetch로 함께 읽어와 쿼리가 건수만큼 반복되지 않게 한다.
	 */
	public static MemberResponse from(ProjectMember member) {
		return new MemberResponse(
				member.getUser().getId(),
				member.getUser().getName(),
				member.getUser().getEmail(),
				member.getRole(),
				member.getJoinedAt()
		);
	}
}
