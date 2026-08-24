package com.example.collab.repository;

import com.example.collab.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * User 엔티티의 영속성 계층.
 *
 * JpaRepository<User, Long>을 상속하면 save·findById·findAll·delete 등
 * 기본 CRUD 메서드를 Spring Data JPA가 런타임에 구현해 준다.
 * 그래서 이 인터페이스에는 구현 클래스도, @Repository 어노테이션도 필요 없다.
 * (두 번째 타입 인자 Long은 User의 기본키 타입이다)
 *
 * 기본 CRUD로 안 되는 조회는 메서드 이름 규칙(findByXxx)이나
 * @Query로 여기에 추가한다.
 */
public interface UserRepository extends JpaRepository<User, Long> {

	/**
	 * 해당 이메일로 가입한 사용자가 이미 있는지 확인한다.
	 *
	 * 메서드 이름만으로 쿼리가 만들어진다. exists + By + 필드명(Email) 규칙을
	 * Spring Data가 해석해 "select count(*) > 0 from users where email = ?"에
	 * 해당하는 쿼리를 실행한다.
	 *
	 * 사용자 전체를 가져와 세지 않고 boolean으로 받는 이유는,
	 * 존재 여부만 알면 되는데 엔티티를 메모리로 읽어올 이유가 없기 때문이다.
	 *
	 * @param email 확인할 이메일
	 * @return 이미 존재하면 true
	 */
	boolean existsByEmail(String email);
}
