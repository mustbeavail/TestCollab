package com.example.collab.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자가 특정 프로젝트에 속해 있다는 사실과, 그 프로젝트에서의 역할을 담는 엔티티.
 *
 * User와 Project 사이의 다대다 관계를 풀어낸 연결 테이블에 해당한다.
 * 역할을 User에 두지 않고 여기 두는 이유는, 같은 사용자라도
 * 프로젝트마다 다른 역할(A에선 OWNER, B에선 MEMBER)을 가질 수 있기 때문이다.
 *
 * 어떤 요청을 허용할지 판단할 때 이 엔티티를 조회한다.
 * 조회 결과가 없다는 것은 곧 "그 프로젝트의 멤버가 아니다"라는 뜻이고,
 * 그 경우 조회를 포함한 모든 동작을 막아야 한다.
 *
 * @Table 옵션 두 가지:
 * - uniqueConstraints: (project_id, user_id) 조합에 유니크 제약을 건다.
 *   같은 사람을 한 프로젝트에 두 번 추가하는 것을 DB가 막는다. 서비스에서 미리 조회해 걸러도,
 *   그 조회와 저장 사이에 다른 요청이 끼어들 수 있어 코드 검사만으로는 완전히 막히지 않는다.
 * - indexes: user_id 단독 인덱스. "내가 속한 프로젝트 목록" 조회가 user_id만으로 검색하는데,
 *   위 복합 유니크 인덱스는 앞 컬럼이 project_id라 그 조회에 쓰이지 못하기 때문이다.
 */
@Entity
@Table(
		uniqueConstraints = @UniqueConstraint(
				name = "uk_project_member_project_user",
				columnNames = {"project_id", "user_id"}
		),
		indexes = @Index(name = "idx_project_member_user", columnList = "user_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMember {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 소속 프로젝트.
	 *
	 * @ManyToOne : 여러 멤버 행이 하나의 프로젝트를 가리킨다. DB에는 project_id 외래키 컬럼이 생긴다.
	 * fetch = LAZY : 이 멤버를 조회할 때 프로젝트까지 함께 SELECT하지 않고, 실제로
	 *                project.getName() 같은 접근이 일어날 때 비로소 조회한다.
	 *                @ManyToOne의 기본값은 EAGER라 멤버 목록 100건을 읽으면 프로젝트 조회가
	 *                뒤따라 여러 번 나가는 N+1 문제가 생긴다. 그래서 명시적으로 LAZY로 바꾼다.
	 * optional = false : 이 연관관계가 비어 있을 수 없다는 뜻으로, 외래키 컬럼에 NOT NULL이 붙는다.
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	/** 이 멤버가 가리키는 사용자. 매핑 의도는 위 project와 같다. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	/**
	 * 이 멤버의 역할.
	 *
	 * @Enumerated(EnumType.STRING)은 enum을 DB에 문자열("OWNER")로 저장하라는 뜻이다.
	 * 기본값인 ORDINAL은 선언 순서를 숫자(0, 1, 2)로 저장하는데,
	 * 나중에 enum 상수 순서를 바꾸거나 중간에 하나 끼워 넣으면
	 * 기존 데이터의 의미가 통째로 어긋난다. 그래서 STRING을 쓴다.
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Role role;

	/** 이 사용자가 프로젝트에 합류한 시각. */
	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime joinedAt;

	/**
	 * 사용자를 프로젝트 멤버로 만든다.
	 *
	 * @param project 소속시킬 프로젝트
	 * @param user    멤버가 될 사용자
	 * @param role    부여할 역할(프로젝트 생성자라면 OWNER)
	 * @return 아직 저장되지 않은 ProjectMember
	 */
	public static ProjectMember create(Project project, User user, Role role) {
		ProjectMember member = new ProjectMember();
		member.project = project;
		member.user = user;
		member.role = role;
		return member;
	}

	/**
	 * 역할을 바꾼다. OWNER·ADMIN만 호출할 수 있고, 그 판정은 서비스가 미리 끝낸다.
	 *
	 * 여기서 "마지막 OWNER인가"는 검사하지 않는다. 그 판단에는 같은 프로젝트의 다른 멤버들을
	 * 세어봐야 해서 이 엔티티 하나가 가진 정보로는 알 수 없다. 그래서 프로젝트에 OWNER가
	 * 최소 1명 남는지는 서비스가 리포지토리로 개수를 세어 확인한다.
	 *
	 * @param role 새 역할
	 */
	public void changeRole(Role role) {
		this.role = role;
	}
}
