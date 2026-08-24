package com.example.collab.service;

import com.example.collab.repository.ProjectMemberRepository;
import com.example.collab.repository.ProjectRepository;
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
 * 지켜야 할 규칙 두 가지:
 * - 프로젝트에는 항상 최소 1명의 OWNER가 남아야 한다.
 *   (마지막 OWNER의 역할 변경이나 제거를 막아야 한다)
 * - 프로젝트 생성자는 자동으로 OWNER 멤버가 된다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
}
