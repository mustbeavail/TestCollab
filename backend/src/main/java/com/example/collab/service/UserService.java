package com.example.collab.service;

import com.example.collab.domain.User;
import com.example.collab.dto.user.UserCreateRequest;
import com.example.collab.dto.user.UserResponse;
import com.example.collab.exception.CollabException;
import com.example.collab.exception.ErrorCode;
import com.example.collab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 등록·조회의 비즈니스 로직을 담는 계층.
 *
 * @Service : 이 클래스를 스프링 빈으로 등록해 컨트롤러가 주입받을 수 있게 한다.
 * @RequiredArgsConstructor : final 필드를 받는 생성자를 롬복이 만들어 준다.
 *                            생성자가 하나뿐이면 스프링이 그 생성자로 의존성을 주입하므로
 *                            @Autowired를 따로 붙일 필요가 없다.
 * @Transactional(readOnly = true) : 클래스 전체의 기본값을 읽기 전용 트랜잭션으로 둔다.
 *                            데이터를 바꾸는 메서드에만 @Transactional을 다시 붙여 덮어쓴다.
 *                            읽기 전용이면 JPA가 변경 감지(더티 체킹)를 생략해 불필요한 비교를 하지 않는다.
 *
 * 인증이 구현 대상이 아니므로 비밀번호 저장이나 로그인 처리는 없다.
 * 여기서 만든 사용자의 id가 다른 API의 X-User-Id 헤더 값으로 쓰인다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;

	/**
	 * 사용자를 등록한다.
	 *
	 * 처리 순서:
	 * 1. 같은 이메일로 가입한 사용자가 있는지 확인하고, 있으면 409로 거절한다.
	 * 2. User를 만들어 저장한다.
	 * 3. 저장된 사용자를 응답 DTO로 바꿔 돌려준다.
	 *
	 * 1번 검사를 통과해도 DB의 UNIQUE 제약이 마지막 방어선으로 남아 있다.
	 * 검사와 저장 사이에 다른 요청이 끼어들어 같은 이메일을 넣을 수 있기 때문이다.
	 * 검사를 두는 이유는 그 드문 경우가 아닌 대부분의 상황에서
	 * DB 제약 위반 예외 대신 뜻이 분명한 메시지를 주기 위해서다.
	 *
	 * @param request 이름과 이메일
	 * @return 저장된 사용자 정보(id 포함)
	 * @throws CollabException 이메일이 이미 사용 중인 경우
	 */
	@Transactional
	public UserResponse register(UserCreateRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new CollabException(ErrorCode.DUPLICATE_EMAIL);
		}

		User user = userRepository.save(User.create(request.name(), request.email()));
		return UserResponse.from(user);
	}

	/**
	 * 사용자 한 명을 조회한다.
	 *
	 * @param userId 조회할 사용자 id
	 * @return 사용자 정보
	 * @throws CollabException 그 id의 사용자가 없는 경우
	 */
	public UserResponse get(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new CollabException(ErrorCode.USER_NOT_FOUND));

		return UserResponse.from(user);
	}
}
