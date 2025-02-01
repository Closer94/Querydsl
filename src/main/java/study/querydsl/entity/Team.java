package study.querydsl.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) //JPA는 기본생성자가 있어야 한다. (기본 생성자는 protected 로 설정)
@ToString(of = {"id", "name"})
public class Team {

    @Id @GeneratedValue
    private Long id;
    private String name;

    @OneToMany(mappedBy = "team") //연관관계 맵핑(Member.Class 에 있는 필드명 team 을 mappedBy 해준다. -> 연관관계의 주인
    private List<Member> members = new ArrayList<>();

    public Team(String name) {
        this.name = name;
    }
}
