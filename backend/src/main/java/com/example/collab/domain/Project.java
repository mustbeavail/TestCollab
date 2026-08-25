package com.example.collab.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자들이 함께 일하는 단위. 권한 판정과 데이터 격리의 기준이 된다.
 * 멤버·작업 목록은 조건 조회가 필요해 컬렉션으로 두지 않고 반대편에서 단방향 참조한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 500)
	private String description;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	/** 생성자를 OWNER 멤버로 넣는 것은 ProjectMember 행을 만드는 일이라 서비스가 맡는다. */
	public static Project create(String name, String description) {
		Project project = new Project();
		project.name = name;
		project.description = description;
		return project;
	}

	/** 수정 가능한 필드를 이 메서드로 한정한다(setter를 열지 않는 이유). 권한 판정은 서비스가 끝낸다. */
	public void update(String name, String description) {
		this.name = name;
		this.description = description;
	}
}
