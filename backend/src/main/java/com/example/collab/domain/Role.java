package com.example.collab.domain;

/**
 * 프로젝트 안에서 멤버가 갖는 역할.
 *
 * 과제 명세가 세 가지로 못박은 값이라 지원자가 정할 여지가 없다.
 * - OWNER  : 프로젝트 생성자에게 부여된다. 삭제까지 포함해 모든 권한을 갖는다.
 * - ADMIN  : 프로젝트 수정과 멤버 관리를 할 수 있다. 프로젝트 삭제는 못 한다.
 * - MEMBER : 조회와 작업 생성만 가능하다.
 *
 * 같은 사용자라도 프로젝트가 다르면 역할이 다를 수 있으므로,
 * 이 값은 User가 아니라 ProjectMember(사용자-프로젝트 연결)에 붙는다.
 */
public enum Role {
	OWNER,
	ADMIN,
	MEMBER
}
