package com.example.collab.repository;

import com.example.collab.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

/** User 영속성 계층. 기본 CRUD는 Spring Data JPA가 런타임에 구현한다. */
public interface UserRepository extends JpaRepository<User, Long> {

	/** 존재 여부만 필요해 엔티티를 읽지 않는다. 메서드 이름으로 쿼리가 만들어진다. */
	boolean existsByEmail(String email);
}
