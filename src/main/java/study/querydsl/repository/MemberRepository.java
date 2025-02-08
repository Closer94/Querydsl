package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
    //MemberJpaRepository.class > findByUsername() 를 아래로 대체 가능
    List<Member> findByUsername(String username);


}
