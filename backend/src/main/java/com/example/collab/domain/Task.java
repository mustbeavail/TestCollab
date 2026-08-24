package com.example.collab.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 프로젝트에 속한 할 일.
 *
 * 제목·담당자·상태를 가지며, 수정과 삭제는 담당자 본인이거나
 * 그 프로젝트의 OWNER·ADMIN만 할 수 있다.
 *
 * @Table의 indexes:
 * 작업 목록 조회는 항상 "이 프로젝트의 작업" 조건이 먼저 걸리고 그 위에 상태 필터가 얹힌다.
 * 그래서 (project_id, status) 순서의 복합 인덱스를 둔다. 앞 컬럼만 쓰는 조회
 * (프로젝트의 전체 작업)에도 이 인덱스가 그대로 쓰이므로 project_id 단독 인덱스는 필요 없다.
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
	 * 이 작업이 속한 프로젝트. 데이터 격리의 기준이 되는 값이다.
	 *
	 * 작업을 단건으로 조회할 때도 id만으로 찾지 않고 이 project_id를 조건에 함께 넣는다.
	 * id만으로 찾으면 다른 프로젝트의 작업 id를 넣었을 때 남의 데이터가 그대로 나간다.
	 *
	 * fetch = LAZY인 이유는 ProjectMember의 설명과 같다(기본값 EAGER의 N+1 방지).
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	/**
	 * 담당자. 지정하지 않은 작업을 허용하므로 null이 될 수 있고, 외래키 컬럼도 NULL 허용이다.
	 *
	 * 이 외래키가 users를 가리키고 project_member를 가리키지 않는 이유:
	 * project_member를 참조하면 "담당자는 반드시 그 프로젝트 멤버"가 DB 차원에서 보장되지만,
	 * 멤버를 제거하는 순간 그 사람이 맡던 작업의 담당자 정보가 외래키 제약에 걸려 함께 무너진다.
	 * 대신 "담당자로 지정하려는 사용자가 그 프로젝트의 멤버인가"는 서비스가 검사한다.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assignee_id")
	private User assignee;

	/** 작업 제목. 목록 조회의 검색 대상이 되는 값이다. */
	@Column(nullable = false, length = 200)
	private String title;

	/** 작업 설명. 없어도 되는 값이라 NULL을 허용한다. */
	@Column(length = 2000)
	private String description;

	/** 진행 상태. 목록 조회의 필터 조건이 된다. 문자열로 저장하는 이유는 Role과 같다. */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TaskStatus status;

	/**
	 * 낙관적 락을 위한 버전 번호. 두 사용자가 같은 작업을 동시에 수정할 때
	 * 나중 요청이 앞선 변경을 조용히 덮어쓰는 것을 막는다.
	 *
	 * 동작 방식:
	 * 1. 조회 시점의 version 값을 클라이언트에 함께 내려준다.
	 * 2. 수정 요청은 그 version을 그대로 실어 보낸다.
	 * 3. Hibernate가 커밋할 때 UPDATE ... WHERE id = ? AND version = ? 형태로 쿼리를 만들고,
	 *    동시에 version을 1 올린다.
	 * 4. 그 사이 다른 사용자가 먼저 수정했다면 DB의 version이 이미 올라가 있어
	 *    WHERE 조건에 걸리는 행이 0건이 되고, Hibernate가 예외를 던진다.
	 *
	 * 검사와 갱신이 UPDATE 한 문장 안에서 끝나므로, 별도 SELECT로 비교할 때 생기는
	 * "비교와 쓰기 사이의 틈"이 없다.
	 *
	 * 이 필드의 증가와 WHERE 조건 추가는 모두 Hibernate가 처리하므로
	 * 애플리케이션 코드에서 version을 직접 건드릴 일은 없다.
	 */
	@Version
	private Long version;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	/**
	 * 작업을 새로 만든다. 프로젝트 멤버라면 누구나 만들 수 있고, 그 판정은 서비스가 미리 끝낸다.
	 *
	 * 상태는 인자로 받지 않고 항상 TODO로 시작한다. 아직 아무도 손대지 않은 작업이
	 * 처음부터 DONE인 상태로 만들어질 이유가 없기 때문이다.
	 *
	 * @param project     이 작업이 속할 프로젝트
	 * @param title       제목
	 * @param description 설명(없으면 null)
	 * @param assignee    담당자(지정하지 않으면 null)
	 * @return 아직 저장되지 않은 Task. status는 TODO, version은 저장 시 0으로 채워진다.
	 */
	public static Task create(Project project, String title, String description, User assignee) {
		Task task = new Task();
		task.project = project;
		task.title = title;
		task.description = description;
		task.assignee = assignee;
		task.status = TaskStatus.TODO;
		return task;
	}

	/**
	 * 제목·설명·상태·담당자를 바꾼다. 담당자 본인이나 OWNER·ADMIN만 호출할 수 있고,
	 * 그 판정과 "새 담당자가 이 프로젝트의 멤버인가" 검사는 서비스가 미리 끝낸다.
	 *
	 * project는 바꾸지 않는다. 작업을 다른 프로젝트로 옮기는 것은 요구사항에 없고,
	 * 허용하면 권한 검사를 통과한 프로젝트와 실제로 저장되는 프로젝트가 달라져 격리가 깨진다.
	 *
	 * 이 메서드가 값을 바꾸면 커밋 시점에 JPA가 UPDATE를 내보내면서 version을 함께 올린다.
	 * 그 사이 다른 사용자가 먼저 수정했다면 여기서 바꾼 내용은 반영되지 않고 충돌 예외가 난다.
	 *
	 * @param title       새 제목
	 * @param description 새 설명(없으면 null)
	 * @param status      새 상태
	 * @param assignee    새 담당자(지정 해제하려면 null)
	 */
	public void update(String title, String description, TaskStatus status, User assignee) {
		this.title = title;
		this.description = description;
		this.status = status;
		this.assignee = assignee;
	}
}
