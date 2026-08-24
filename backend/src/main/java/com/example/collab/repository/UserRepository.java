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
}
