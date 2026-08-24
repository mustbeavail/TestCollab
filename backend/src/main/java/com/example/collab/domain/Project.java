package com.example.collab.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자들이 함께 일하는 단위.
 *
 * 멤버와 역할을 가지며, 작업(Task)은 반드시 어떤 프로젝트에 속한다.
 * 권한 판정과 데이터 격리가 모두 이 프로젝트를 기준으로 이뤄진다.
 * (프로젝트에 속하지 않은 사용자는 그 프로젝트의 작업을 조회조차 할 수 없다)
 *
 * 멤버 목록과 작업 목록을 @OneToMany 컬렉션으로 들고 있지 않은 이유:
 * 두 목록 모두 페이징·필터·권한 조회가 필요해 실제로는 리포지토리에 조건을 걸어 가져온다.
 * 컬렉션 필드를 두면 프로젝트를 읽을 때마다 딸린 데이터를 통째로 끌어올 위험만 생기고
 * 쓰이지는 않는다. 그래서 연관관계는 ProjectMember·Task 쪽에서 단방향으로만 참조한다.
 *
 * 프로젝트를 삭제할 때 딸린 멤버·작업은 DB가 함께 지운다.
 * ProjectMember.project와 Task.project에 붙인 @OnDelete(CASCADE)가 그 역할을 한다.
 * (자식 행이 남으면 외래키 제약에 걸려 프로젝트 삭제 자체가 실패하므로 처리가 필요하다)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 프로젝트 이름. 목록 화면에서 프로젝트를 구분하는 값이다. */
	@Column(nullable = false, length = 100)
	private String name;

	/** 프로젝트 설명. 없어도 되는 값이라 NULL을 허용한다. */
	@Column(length = 500)
	private String description;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/**
	 * 마지막 수정 시각.
	 *
	 * @UpdateTimestamp는 하이버네이트가 UPDATE를 내보낼 때마다 현재 시각으로 갱신한다.
	 * 변경 감지(더티 체킹)로 실제 UPDATE가 나갈 때만 바뀌므로, 값이 그대로면 갱신되지 않는다.
	 */
	@UpdateTimestamp
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	/**
	 * 프로젝트를 새로 만든다.
	 *
	 * 만든 사람을 인자로 받지 않는 이유: "생성자가 OWNER 멤버가 된다"는 규칙은
	 * Project 한 행이 아니라 ProjectMember 한 행을 더 만드는 일이라 서비스가 두 단계로 처리한다.
	 *
	 * @param name        프로젝트 이름
	 * @param description 설명(없으면 null)
	 * @return 아직 저장되지 않은 Project
	 */
	public static Project create(String name, String description) {
		Project project = new Project();
		project.name = name;
		project.description = description;
		return project;
	}

	/**
	 * 이름과 설명을 바꾼다. OWNER·ADMIN만 호출할 수 있으며, 그 판정은 서비스가 미리 끝낸다.
	 *
	 * setter 대신 이 메서드 하나만 두는 이유: 수정 가능한 필드가 무엇인지 여기서 한눈에 드러나고,
	 * id나 createdAt처럼 바뀌면 안 되는 값에는 애초에 변경 통로가 생기지 않는다.
	 *
	 * 이 객체가 트랜잭션 안에서 조회된 상태라면, 값을 바꾸는 것만으로 커밋 시점에
	 * JPA가 변경을 감지해 UPDATE를 내보낸다(별도 save() 호출이 필요 없다).
	 *
	 * @param name        새 이름
	 * @param description 새 설명
	 */
	public void update(String name, String description) {
		this.name = name;
		this.description = description;
	}
}
