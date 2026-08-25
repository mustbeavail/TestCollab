package com.example.collab.config;

import com.example.collab.domain.*;
import com.example.collab.repository.ProjectMemberRepository;
import com.example.collab.repository.ProjectRepository;
import com.example.collab.repository.TaskRepository;
import com.example.collab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 기동 직후 예시 데이터를 넣는다(CommandLineRunner). H2 인메모리라 끄면 사라진다.
 * 제출 조건이 "클론 후 bootRun 한 번으로 동작"이라, 받는 사람이 바로 눌러볼 수 있어야 한다.
 *
 * data.sql 대신 자바로 넣는 이유: version·createdAt은 하이버네이트가 채우는 값이라
 * SQL로 직접 INSERT하면 손으로 다 채워야 하고, 엔티티가 강제하는 규칙도 우회된다.
 * @Profile("!test") : 테스트는 각자 데이터를 준비하므로 여기 데이터가 섞이면 건수가 어긋난다.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

	private final UserRepository userRepository;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final TaskRepository taskRepository;

	@Override
	@Transactional
	public void run(String... args) {
		// 역할별 계정을 갈라둔다. X-User-Id에 무엇을 넣느냐로 결과가 갈리기 때문이다.
		User owner = save("김오너", "owner@example.com");
		User admin = save("이관리", "admin@example.com");
		User member = save("박멤버", "member@example.com");
		User member2 = save("최멤버", "member2@example.com");
		User outsider = save("정외부", "outsider@example.com");   // 어디에도 속하지 않음 → 403 확인용
		User both = save("한겸직", "both@example.com");            // A에선 MEMBER, B에선 OWNER

		// 프로젝트가 둘이어야 "다른 프로젝트의 데이터가 섞이지 않는다"를 확인할 수 있다.
		Project projectA = projectRepository.save(
				Project.create("협업 서비스 개편", "2026년 상반기 개편 작업"));
		Project projectB = projectRepository.save(
				Project.create("사내 위키 이전", "기존 위키를 새 시스템으로 옮긴다"));

		join(projectA, owner, Role.OWNER);
		join(projectA, admin, Role.ADMIN);
		join(projectA, member, Role.MEMBER);
		join(projectA, both, Role.MEMBER);
		join(projectB, both, Role.OWNER);
		join(projectB, member2, Role.MEMBER);

		// 22개 — 기본 페이지 크기가 20이라 2페이지가 생긴다. 상태와 담당자(미지정 포함)도 섞는다.
		List<String> titles = List.of(
				"로그인 화면 퍼블리싱", "회원가입 유효성 검사", "비밀번호 재설정 메일",
				"프로젝트 목록 API 연동", "작업 상세 모달", "담당자 지정 드롭다운",
				"상태 필터 UI", "검색창 디바운스 적용", "페이지네이션 컴포넌트",
				"권한별 버튼 노출 처리", "409 충돌 안내 문구", "빈 상태 화면 디자인",
				"로딩 스피너 통일", "에러 토스트 컴포넌트", "반응형 레이아웃 점검",
				"접근성 키보드 이동", "다크 모드 색상 정의", "아이콘 세트 교체",
				"폰트 로딩 최적화", "번들 크기 줄이기", "배포 스크립트 정리", "README 초안 작성");

		TaskStatus[] statuses = {TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.DONE};
		User[] assignees = {member, admin, both, null};

		for (int i = 0; i < titles.size(); i++) {
			Task task = Task.create(projectA, titles.get(i), null, assignees[i % assignees.length]);
			// 생성 시 상태는 항상 TODO라, 예시 데이터에서만 곧바로 바꿔 섞는다.
			task.update(task.getTitle(), null, statuses[i % statuses.length], task.getAssignee());
			taskRepository.save(task);
		}

		// A 목록에 이 3개가 섞이지 않는 것으로 데이터 격리를 확인한다.
		taskRepository.save(Task.create(projectB, "위키 문서 목록 정리", null, both));
		taskRepository.save(Task.create(projectB, "이미지 첨부 마이그레이션", null, member2));
		taskRepository.save(Task.create(projectB, "권한 매핑 표 작성", null, null));

		// 어떤 id로 호출해야 어떤 역할이 되는지 알려준다. README 없이도 Swagger에서 시험해볼 수 있다.
		log.info("""

				=== 초기 데이터 준비 완료 ===
				요청 헤더 X-User-Id 에 아래 id를 넣어 호출한다.
				  프로젝트 {} (협업 서비스 개편) : OWNER={}, ADMIN={}, MEMBER={},{}   작업 {}개
				  프로젝트 {} (사내 위키 이전)   : OWNER={}, MEMBER={}                작업 3개
				  어느 프로젝트에도 속하지 않은 사용자: {}  (403 확인용)
				  사용자 {}은 A에서 MEMBER, B에서 OWNER 이다.
				Swagger UI : http://localhost:8080/swagger-ui.html
				""",
				projectA.getId(), owner.getId(), admin.getId(), member.getId(), both.getId(), titles.size(),
				projectB.getId(), both.getId(), member2.getId(),
				outsider.getId(), both.getId());
	}

	private User save(String name, String email) {
		return userRepository.save(User.create(name, email));
	}

	private void join(Project project, User user, Role role) {
		projectMemberRepository.save(ProjectMember.create(project, user, role));
	}
}
