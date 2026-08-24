package com.example.collab.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 서비스를 이용하는 사용자.
 *
 * 인증은 구현 대상이 아니므로 비밀번호 같은 인증용 필드는 두지 않는다.
 * 요청자가 누구인지는 API 파라미터로 전달받은 이 엔티티의 id로 판별한다.
 *
 * 역할(Role)은 여기 두지 않는다. 같은 사용자라도 프로젝트마다 역할이 다를 수 있어서,
 * 역할은 사용자-프로젝트 연결인 ProjectMember에 붙는다.
 *
 * @Table(name = "users")를 지정한 이유:
 * H2를 비롯한 다수 DB에서 USER는 예약어라, 테이블명을 그대로 user로 두면
 * 스키마 생성 단계에서 문법 오류가 난다. 그래서 복수형 users로 우회한다.
 *
 * @NoArgsConstructor(access = PROTECTED)를 쓰는 이유:
 * JPA는 엔티티를 DB에서 읽어올 때 기본 생성자로 빈 객체를 만든 뒤 필드를 채우므로
 * 기본 생성자가 반드시 있어야 한다. 다만 이 생성자를 외부에서 호출하면
 * 이름·이메일이 비어 있는 User가 만들어지므로, 접근 범위를 protected로 낮춰
 * JPA만 쓰게 하고 애플리케이션 코드는 아래 create()를 거치도록 강제한다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	/**
	 * 기본키. GenerationType.IDENTITY는 DB의 자동 증가 컬럼에 채번을 맡긴다는 뜻으로,
	 * 값을 넣지 않고 저장하면 DB가 1, 2, 3... 을 매긴다.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 화면에 보이는 이름. 담당자 표시에 쓴다. */
	@Column(nullable = false, length = 50)
	private String name;

	/**
	 * 사용자를 사람이 알아볼 수 있게 구분하는 값.
	 *
	 * unique = true는 DB에 유니크 제약을 만들어 같은 이메일로 두 번 등록되는 것을 막는다.
	 * 애플리케이션에서 "이미 있는지" 먼저 조회해 검사하더라도, 그 조회와 저장 사이에
	 * 다른 요청이 끼어들 수 있어 코드 검사만으로는 완전히 막히지 않는다. DB 제약이 마지막 방어선이다.
	 */
	@Column(nullable = false, unique = true, length = 255)
	private String email;

	/**
	 * 생성 시각.
	 *
	 * @CreationTimestamp는 하이버네이트가 INSERT 시점에 현재 시각을 자동으로 채워주는 어노테이션이다.
	 * 별도 설정(@EnableJpaAuditing 등) 없이 동작하고, 이후 UPDATE에서는 값이 바뀌지 않는다.
	 * updatable = false를 함께 둬 실수로 갱신되는 경로까지 막는다.
	 */
	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/**
	 * 사용자를 새로 만든다. 생성자를 직접 열지 않고 이 정적 메서드만 공개해,
	 * User를 만드는 경로를 한 곳으로 모은다.
	 *
	 * @param name  표시용 이름
	 * @param email 중복될 수 없는 식별용 이메일
	 * @return 아직 저장되지 않은(id가 null인) User. 저장은 호출한 쪽에서 repository.save()로 한다.
	 */
	public static User create(String name, String email) {
		User user = new User();
		user.name = name;
		user.email = email;
		return user;
	}
}
