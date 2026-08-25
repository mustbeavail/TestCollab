package com.example.collab.domain;

/**
 * 프로젝트 안에서 멤버가 갖는 역할.
 * 같은 사용자도 프로젝트마다 역할이 다를 수 있어 User가 아니라 ProjectMember에 붙는다.
 */
public enum Role {
	OWNER,
	ADMIN,
	MEMBER
}
