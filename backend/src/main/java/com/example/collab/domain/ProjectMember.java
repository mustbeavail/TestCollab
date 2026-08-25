package com.example.collab.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * 사용자가 어떤 프로젝트에 속해 있다는 사실과 그 프로젝트에서의 역할.
 * 조회 결과가 없다는 것이 곧 "멤버가 아니다"이며, 권한 판정은 이 엔티티로 한다.
 *
 * uniqueConstraints : 같은 사람을 한 프로젝트에 두 번 넣는 것을 DB가 막는다.
 * indexes           : "내 프로젝트 목록"은 user_id만으로 검색하는데,
 *                     위 복합 인덱스는 앞 컬럼이 project_id라 쓰이지 못한다.
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

	// LAZY : @ManyToOne 기본값 EAGER면 멤버 목록을 읽을 때마다 프로젝트 조회가 뒤따른다(N+1).
	// @OnDelete : 프로젝트를 지우면 이 행도 DB가 함께 지운다.
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	/** STRING으로 저장한다. 기본값 ORDINAL은 enum 순서가 바뀌면 기존 데이터의 의미가 어긋난다. */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Role role;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime joinedAt;

	public static ProjectMember create(Project project, User user, Role role) {
		ProjectMember member = new ProjectMember();
		member.project = project;
		member.user = user;
		member.role = role;
		return member;
	}

	/** "마지막 OWNER인가"는 다른 멤버를 세어봐야 알 수 있어 서비스가 검사한다. */
	public void changeRole(Role role) {
		this.role = role;
	}
}
