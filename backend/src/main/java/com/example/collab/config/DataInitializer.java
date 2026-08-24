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
 * 애플리케이션이 뜰 때 예시 데이터를 넣는다.
 *
 * 왜 필요한가:
 * DB가 H2 인메모리이고 ddl-auto가 create-drop이라, 애플리케이션을 끄면 데이터가 사라진다.
 * 아무것도 넣지 않으면 Swagger UI를 열어도 사용자부터 하나씩 만들어야 조회 API를 눌러볼 수 있다.
 * 과제 제출 조건이 "클론 후 bootRun 한 번으로 동작"이므로, 받는 사람이 바로 확인할 수 있어야 한다.
 *
 * CommandLineRunner :
 * 스프링 부트가 기동을 마친 직후 run()을 한 번 호출하는 인터페이스다.
 *
 * data.sql(스프링 부트가 자동 실행하는 SQL 파일)을 쓰지 않은 이유:
 * - version, created_at, updated_at은 @Version·@CreationTimestamp·@UpdateTimestamp로
 *   하이버네이트가 채우는 값이다. SQL로 직접 INSERT하면 하이버네이트를 거치지 않으므로
 *   손으로 다 채워야 하고, 빠뜨리면 NOT NULL 제약에 걸려 기동이 실패한다.
 * - 엔티티 필드가 바뀌면 SQL은 런타임에야 깨지지만, 자바로 쓰면 컴파일러가 잡는다.
 * - 엔티티 팩토리(User.create 등)를 그대로 쓰므로 엔티티가 강제하는 규칙
 *   (작업은 항상 TODO로 시작 등)이 예시 데이터에도 똑같이 적용된다.
 *
 * @Profile("!test") :
 * test 프로파일에서는 이 빈을 만들지 않는다. 테스트는 각자 필요한 데이터를 직접 준비하는데,
 * 여기서 넣은 데이터가 함께 있으면 이메일이 중복되거나 조회 결과 건수가 어긋난다.
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

	/**
	 * 예시 데이터를 만든다.
	 *
	 * 데이터 구성이 이렇게 짜인 이유는 아래 log 출력과 각 단계 주석에 적었다.
	 * 한마디로, 과제 요구사항(역할 분기·데이터 격리·페이징)을 눌러보면 바로 확인할 수 있게 배치했다.
	 */
	@Override
	@Transactional
	public void run(String... args) {
		// --- 사용자 6명 ---
		// 인증이 없어 Swagger에서 X-User-Id에 무엇을 넣느냐로 결과가 갈린다.
		// 그래서 역할별 계정을 미리 갈라둔다.
		User owner = save("김오너", "owner@example.com");
		User admin = save("이관리", "admin@example.com");
		User member = save("박멤버", "member@example.com");
		User member2 = save("최멤버", "member2@example.com");
		User outsider = save("정외부", "outsider@example.com");   // 어느 프로젝트에도 속하지 않음 → 403 확인용
		User both = save("한겸직", "both@example.com");            // A에선 MEMBER, B에선 OWNER

		// --- 프로젝트 2개 ---
		// 하나로는 "다른 프로젝트의 데이터가 섞이지 않는다"를 보여줄 수 없어 둘로 나눈다.
		Project projectA = projectRepository.save(
				Project.create("협업 서비스 개편", "2026년 상반기 개편 작업"));
		Project projectB = projectRepository.save(
				Project.create("사내 위키 이전", "기존 위키를 새 시스템으로 옮긴다"));

		join(projectA, owner, Role.OWNER);
		join(projectA, admin, Role.ADMIN);
		join(projectA, member, Role.MEMBER);
		join(projectA, both, Role.MEMBER);      // 같은 사람이 프로젝트마다 다른 역할을 갖는 사례
		join(projectB, both, Role.OWNER);
		join(projectB, member2, Role.MEMBER);

		// --- 프로젝트 A의 작업 22개 ---
		// 기본 페이지 크기가 20이라 2페이지가 생긴다. 페이징이 동작하는 것이 눈으로 보인다.
		// 상태를 섞고, 담당자가 없는 작업도 넣어 필터와 null 처리를 함께 확인할 수 있게 한다.
		List<String> titles = List.of(
				"로그인 화면 퍼블리싱", "회원가입 유효성 검사", "비밀번호 재설정 메일",
				"프로젝트 목록 API 연동", "작업 상세 모달", "담당자 지정 드롭다운",
				"상태 필터 UI", "검색창 디바운스 적용", "페이지네이션 컴포넌트",
				"권한별 버튼 노출 처리", "409 충돌 안내 문구", "빈 상태 화면 디자인",
				"로딩 스피너 통일", "에러 토스트 컴포넌트", "반응형 레이아웃 점검",
				"접근성 키보드 이동", "다크 모드 색상 정의", "아이콘 세트 교체",
				"폰트 로딩 최적화", "번들 크기 줄이기", "배포 스크립트 정리", "README 초안 작성");

		TaskStatus[] statuses = {TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.DONE};
		User[] assignees = {member, admin, both, null};   // null은 담당자 미지정

		for (int i = 0; i < titles.size(); i++) {
			Task task = Task.create(projectA, titles.get(i), null, assignees[i % assignees.length]);
			// 상태는 생성 시 항상 TODO이므로, 예시 데이터에서만 섞어주기 위해 바로 바꾼다.
			task.update(task.getTitle(), null, statuses[i % statuses.length], task.getAssignee());
			taskRepository.save(task);
		}

		// --- 프로젝트 B의 작업 3개 ---
		// A의 목록을 조회했을 때 이 3개가 섞이지 않는 것으로 데이터 격리를 확인한다.
		taskRepository.save(Task.create(projectB, "위키 문서 목록 정리", null, both));
		taskRepository.save(Task.create(projectB, "이미지 첨부 마이그레이션", null, member2));
		taskRepository.save(Task.create(projectB, "권한 매핑 표 작성", null, null));

		// --- 안내 로그 ---
		// 인증이 없어 X-User-Id에 무엇을 넣어야 어떤 역할로 동작하는지 알려줘야 한다.
		// 이 출력을 보면 README를 열지 않고도 Swagger UI에서 바로 시험해볼 수 있다.
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

	/** 사용자 한 명을 저장한다. run() 안에서 여섯 번 반복되는 코드를 줄인다. */
	private User save(String name, String email) {
		return userRepository.save(User.create(name, email));
	}

	/** 사용자를 프로젝트 멤버로 넣는다. */
	private void join(Project project, User user, Role role) {
		projectMemberRepository.save(ProjectMember.create(project, user, role));
	}
}
