package com.example.collab.domain;

/**
 * 작업의 진행 상태. "안 함 / 하는 중 / 끝남"을 덮는 최소 구성으로 정했다.
 * 보류·검토중 같은 값은 협업 규칙이 정해져야 쓸모가 생겨 넣지 않았다.
 */
public enum TaskStatus {
	TODO,
	IN_PROGRESS,
	DONE
}
