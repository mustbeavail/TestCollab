package com.example.collab.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 프로젝트에 속한 할 일.
 * 목록 조회가 늘 "프로젝트 + 상태"로 걸려 (project_id, status) 복합 인덱스를 둔다.
 */
@Entity
@Table(indexes = @Index(name = "idx_task_project_status", columnList = "project_id, status"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 데이터 격리의 기준. 단건 조회에도 이 조건을 함께 걸어 남의 프로젝트 작업이 나가지 않게 한다.
	 * @OnDelete : 프로젝트 삭제 시 DB가 한 문장으로 함께 지운다(CascadeType.REMOVE는 건마다 DELETE).
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Project project;

	/**
	 * 담당자(미지정 가능). project_member가 아니라 users를 가리키는 이유:
	 * 멤버를 제거해도 작업의 담당자 정보가 외래키 제약에 걸려 무너지지 않게 하려는 것이다.
	 * "담당자가 그 프로젝트 멤버인가"는 서비스가 검사한다.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assignee_id")
	private User assignee;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(length = 2000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TaskStatus status;

	/**
	 * 낙관적 락. 커밋 시 UPDATE ... WHERE id = ? AND version = ? 로 나가며 version을 1 올린다.
	 * 그 사이 남이 먼저 수정했으면 걸리는 행이 0건이 되어 하이버네이트가 충돌 예외를 던진다.
	 */
	@Version
	private Long version;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	/** 상태를 인자로 받지 않는다. 아무도 손대지 않은 작업은 항상 TODO로 시작한다. */
	public static Task create(Project project, String title, String description, User assignee) {
		Task task = new Task();
		task.project = project;
		task.title = title;
		task.description = description;
		task.assignee = assignee;
		task.status = TaskStatus.TODO;
		return task;
	}

	/** project는 바꾸지 않는다. 옮길 수 있으면 권한을 검사한 프로젝트와 저장되는 프로젝트가 달라진다. */
	public void update(String title, String description, TaskStatus status, User assignee) {
		this.title = title;
		this.description = description;
		this.status = status;
		this.assignee = assignee;
	}
}
