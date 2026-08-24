package com.example.collab.service;

import com.example.collab.repository.ProjectMemberRepository;
import com.example.collab.repository.ProjectRepository;
import com.example.collab.repository.TaskRepository;
import com.example.collab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로젝트 CRUD와 멤버 관리의 비즈니스 로직을 담는 계층.
 *
 * 권한 판정이 여기서 이뤄진다. 컨트롤러는 요청자 식별자를 넘겨주기만 하고,
 * "이 사람이 이 프로젝트에서 무엇을 할 수 있는가"는 이 계층이
 * ProjectMemberRepository로 역할을 조회해 결정한다.
 *
 * 지켜야 할 규칙 세 가지:
 * - 프로젝트에는 항상 최소 1명의 OWNER가 남아야 한다.
 *   (마지막 OWNER의 역할 변경이나 제거를 막아야 한다)
 * - 프로젝트 생성자는 자동으로 OWNER 멤버가 된다.
 * - OWNER를 임명하거나 해임하는 일은 OWNER만 할 수 있다.
 *   ADMIN은 MEMBER와 ADMIN 사이만 다룬다. 이 제한이 없으면 ADMIN이
 *   자기 자신을 OWNER로 바꿀 수 있어 두 역할을 나눈 의미가 사라진다.
 *   (명세에 없는 제약이라 README에 근거를 남긴다)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;

	/** 프로젝트 생성 시 생성자를, 멤버 추가 시 대상자를 조회하는 데 쓴다. */
	private final UserRepository userRepository;

	/**
	 * 멤버를 제거할 때 그 사람이 담당하던 작업의 담당자를 비우는 데 쓴다.
	 *
	 * 프로젝트 삭제에는 쓰지 않는다. 딸린 작업은 @OnDelete(CASCADE)로
	 * DB가 지우므로 서비스가 관여할 일이 없다.
	 */
	private final TaskRepository taskRepository;
}
