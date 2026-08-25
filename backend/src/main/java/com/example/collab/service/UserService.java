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
 * 사용자 등록·조회. 인증이 구현 대상이 아니라 비밀번호 저장이나 로그인 처리는 없다.
 * 여기서 만든 id가 다른 API의 X-User-Id 값으로 쓰인다.
 *
 * readOnly = true를 클래스 기본값으로 두고, 변경 메서드에만 @Transactional을 다시 붙인다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;

	/**
	 * 사용자를 등록한다. 이메일 중복은 DB 유니크 제약이 마지막 방어선이고,
	 * 여기서 미리 검사하는 것은 제약 위반 예외 대신 뜻이 분명한 메시지를 주기 위해서다.
	 */
	@Transactional
	public UserResponse register(UserCreateRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new CollabException(ErrorCode.DUPLICATE_EMAIL);
		}

		User user = userRepository.save(User.create(request.name(), request.email()));
		return UserResponse.from(user);
	}

	public UserResponse get(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new CollabException(ErrorCode.USER_NOT_FOUND));

		return UserResponse.from(user);
	}
}
