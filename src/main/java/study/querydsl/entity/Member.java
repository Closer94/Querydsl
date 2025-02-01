package study.querydsl.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) //JPA는 기본생성자가 있어야 한다. (기본 생성자는 protected 로 설정)
@ToString(of = {"id", "username", "age"})
public class Member {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;
    private String username;
    private int age;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id") // 1대다 에서 "다" 에 @JoinColunm 을 사용
    private Team team;

    public Member(String username) {
        this(username, 0);
    }

    public Member(String username, int age) {
        this(username, age, null);

    }

    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;
        if (team != null) {
            changeTeam(team);
        }
    }

    // Team이 변경된 경우 (TeamA 에서 TeamB 로 변경)
    // 1. Member.class 에서 team 값 변경 (Member 테이블의 Team 속성 값 변경)
    // 2. Team.class 에서 변경된 Member 도 변경(Team 테이블의 Member 속성 값 변경)
    private void changeTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }

    //@ToString(of = {"id", "username", "age"})
    //아래의 항목을 위에 @ToString 어노테이션에서 처리
    //단, 연관관계 필드 team 은 ToString 에 설정하면 안된다.
    /*
    @Override
    public String toString() {
        return "Member{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", age=" + age +
                ", team=" + team +
                '}';
    }
     */
}
