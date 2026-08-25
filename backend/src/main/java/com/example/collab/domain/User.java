package com.example.collab.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 서비스를 이용하는 사용자. 인증이 구현 대상이 아니라 비밀번호 같은 필드는 두지 않는다.
 * 테이블명이 users인 이유: USER는 다수 DB에서 예약어라 스키마 생성이 실패한다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // JPA 전용 기본 생성자. 생성은 create()로만.
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String name;

	/** 중복 검사와 저장 사이에 다른 요청이 끼어들 수 있어, DB 유니크 제약을 마지막 방어선으로 둔다. */
	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/** @return 아직 저장되지 않은 User. 저장은 호출한 쪽에서 한다. */
	public static User create(String name, String email) {
		User user = new User();
		user.name = name;
		user.email = email;
		return user;
	}
}
