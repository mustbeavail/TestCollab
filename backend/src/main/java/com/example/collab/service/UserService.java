package com.example.collab.service;

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
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;
}
