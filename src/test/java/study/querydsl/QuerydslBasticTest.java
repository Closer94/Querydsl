package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.select;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasticTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

    }

    @Test
    public void startJPQL() throws Exception {
        //member1 을 찾아라
        String qlString = "select m from Member m where m.username = :username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() throws Exception {
//        JPAQueryFactory queryFactory = new JPAQueryFactory(em); //JPAQueryFactory 를 만들때 EntityManager 을 생성자로 넣어줘야한다.
//        QMember m = new QMember("m"); //어떤 QMember인지 생성 시 이름(별칭)을 생성자로 넣어 주어야한다.
//        QMember member = QMember.member;

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) //파라미터 바인딩 처리
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() throws Exception {
        Member findMember = queryFactory
                .selectFrom(member) //select member from member 을 합침
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void searchAndParam() throws Exception {
        Member findMember = queryFactory
                .selectFrom(member) //select member from member 을 합침
                .where(member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void resultFetch() throws Exception {
        //리스트 조회
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        //단건 조회
        Member member = queryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.username.eq("member1"))
                .fetchOne();

        // == limit(1).fetchOne()
        Member fetchFirst = queryFactory
                .selectFrom(QMember.member)
                .fetchFirst();


    }

    /**
     * 회언 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 오름차순(asc)
     * 단, 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     *
     * @throws Exception
     */
    @Test
    public void sort() throws Exception {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(),
                        member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();

    }

    @Test
    public void paging1() throws Exception {
        //given
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(2)
                .limit(2)
                .fetch();
        for (Member member : result) {
            System.out.println("member = " + member);
        }

        assertThat(result.size()).isEqualTo(2);
        //when

        //then

    }

    @Test
    public void aggregation() throws Exception{
        //given
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();
        //when

        //then
        //Tuple은 Querydsl에서 제공하는 Tuple임.
        //Tuple은 데이터 타입이 많을 때 사용됨.
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 가 ㅌ 팀의 평균 연령을 구해라.
     * @throws Exception
     */
    @Test
    public void group() throws Exception{
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15); //(10+20) / 2

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35); //(30+40) / 2


    }

    /**
     * 팀A 에 소속된 모든 호원 조회
     * @throws Exception
     */
    @Test
    public void join() throws Exception{
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");


    }

    /**
     * 세타 조인(연관 관계 없는 조인)
     * 회원의 이름이 팀 이름과 같은 회원 조회
     * @throws Exception
     */
    @Test
    public void theta_join() throws Exception{
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team) // 세타 조인
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA 인 팀만 조인, 회원은 모두 조회
     * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
     * @throws Exception
     */
    @Test
    public void join_on_filtering() throws Exception{
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    /**
     * 연관관계가 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     * @throws Exception
     */
    @Test
    public void join_on_no_relation() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member) // 세타 조인
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    @PersistenceUnit
    EntityManagerFactory emf;

    // Member 데이터를 읽어올 때, 연관관계 맵핑되어있는 Team 은 Lazy로 설정했기때문에 Proxy로 가져온다.
    // 따라서 Member 데이터를 읽어올 때는 Team 을 제외하고 Member 만 조회하는 SQL문이 날란간다.
    // 이때, 만약에 Team 을 조회하는 것이 발생한다면 Team 을 또 조회하는 N+1 이 발생한다.
    @Test
    public void fetchJoinNo() throws Exception{
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();

    }

    // 페치 조인 --> 한방 쿼리 적용 (Member와 그에 해당하는 Team 도 같이 한방에 조회한다.)
    @Test
    public void fetchJoinUse() throws Exception{
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();

    }

    /**
     * 나이가 가장 많은 회원 조회
     * @throws Exception
     */
    @Test
    public void subQuery() throws Exception{
        // 서브쿼리 사용 시, QMember 의 디폴트 alias 랑 겹치면 안됨으로
        // 별도의 alias 를 가진 QMember 을 생성한다.
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);

    }

    /**
     * 나이가 평균 이상인 회원
     * @throws Exception
     */
    @Test
    public void subQueryGoe() throws Exception{
        // 서브쿼리 사용 시, QMember 의 디폴트 alias 랑 겹치면 안됨으로
        // 별도의 alias 를 가진 QMember 을 생성한다.
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);

    }

    /**
     * 서브쿼리 예제
     * @throws Exception
     */
    @Test
    public void subQueryIn() throws Exception{
        // 서브쿼리 사용 시, QMember 의 디폴트 alias 랑 겹치면 안됨으로
        // 별도의 alias 를 가진 QMember 을 생성한다.
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        //서브쿼리
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);

    }

    /**
     * select 절에 subQuery를 넣는 예제
     * 예. Member의 username 과 멤버의 평균 나이도 같이 select
     * @throws Exception
     */
    @Test
    public void selectSubQuery() throws Exception{
        // 서브쿼리 사용 시, QMember 의 디폴트 alias 랑 겹치면 안됨으로
        // 별도의 alias 를 가진 QMember 을 생성한다.
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    @Test
    public void basicCase() throws Exception{
        List<String> result = queryFactory
                .select(
                        member.age
                                .when(10).then("열살")
                                .when(20).then("스무살")
                                .otherwise("기타")

                ).from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }


    }

    @Test
    public void complexCase() throws Exception{
        List<String> result = queryFactory
                .select(
                        new CaseBuilder()
                                .when(member.age.between(0, 20)).then("0~20살")
                                .when(member.age.between(21, 30)).then("21~31살")
                                .otherwise("기타")
                ).from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }

    }

    //상수 "A" 를 쿼리 결과값에 붙여서 나온다.
    /** 결과
     * tuple = [member1, A]
     * tuple = [member2, A]
     * tuple = [member3, A]
     * tuple = [member4, A]
     */
    @Test
    public void constant() throws Exception{
        List<Tuple> result = queryFactory
                .select(
                        member.username,
                        Expressions.constant("A")
                )
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
    
    //문자 더하기
    //ex. {username}_{age}
    //select concat(concat(member1.username,?1),str(member1.age))
    @Test
    public void concat() throws Exception{
        List<String> result = queryFactory
                .select(member.username.concat("_")
                        .concat(member.age.stringValue()) //나이는 문자열이 아님으로 concat 하기전에 문자열로 바꿔줘야한다.
                ).from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    //프로젝션: select 대상 지정
    //프로젝션 대상이 하나(username)인 경우
    @Test
    public void simpleProjection() throws Exception{
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String username : result) {
            System.out.println("username = " + username);
        }

    }

    @Test
    public void tupleProjection() throws Exception{
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }

    }

    //JPQL 로 죄회한 결과를 DTO로 반환
    //-> 조회한 것을 MemberDto 생성자에 담아서 반환
    @Test
    public void findDto() throws Exception{
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    // Querydsl 빈 생성(Bean population)
    // 결과를 DTO 반환할 때 사용

    // case1. 프로퍼티 접근 - Setter (반드시 DTO에 setter 작성해야한다.)
    // 반드시 bean 주입 시 DTO의 기본생성자가 존재해야 한다.
    @Test
    public void findDtoBySetter() throws Exception{
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age)
                )
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    //case2. 필드 직접 접근
    //setter 상관없이 조회한 값이 DTO 필드에 들어간다.
    @Test
    public void findDtoByField() throws Exception{
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);

        }
    }

    //case2. 필드 직접 접근
    //setter 상관없이 조회한 값이 DTO 필드에 들어간다.
    //만약 조회한 필드와 매개변수명이 다른 경우 .as("DTO 필드명") 으로 작성한다.
    @Test
    public void findUserDto() throws Exception{
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        //age 필드값에는 서브쿼리 넣어주기 -> ExpressionUtils 감싸기
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    //case3. 생성자 사용
    //생성자 매개변수 타입이 일치 해야한다.
    @Test
    public void findDtoByConstructor() throws Exception{
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);

        }
    }

    //case3. 생성자 사용
    //생성자 매개변수 타입이 일치 해야한다.
    //MemberDto 에서 UserDto 로 변경했지만, 필드의 타입이 일치함으로 잘 나온다.
    @Test
    public void findUserDtoByConstructor() throws Exception{
            List<UserDto> result = queryFactory
                    .select(Projections.constructor(UserDto.class,
                            member.username,
                            member.age))
                    .from(member)
                    .fetch();

            for (UserDto userDto : result) {
                System.out.println("userDto = " + userDto);

            }
    }

    @Test
    public void findDtoByQueryProjection() throws Exception{
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    //동적쿼리 - BooleanBuilder 사용
    @Test
    public void dynamicQuery_BooleanBuilder() throws Exception{
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);

    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    //동적쿼리 - Where 다중 파라미터 사용
    @Test
    public void dynamicQuery_WhereParam() throws Exception{
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
//                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        if(usernameCond == null) {
            return null;
        }else{
            return member.username.eq(usernameCond);
        }
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    //** Bulk 연산 **
    //특정 조건의 username 을 수정
    @Test
    public void bulkUpdate(){
        //member1 = 10 -> 비회원
        //member2 = 20 -> 비회원

        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        //벌크연산 수행 후 영속성 컨텍스트 초기화
        em.flush();
        em.clear();
    }

    //모든 나이 +1
    @Test
    public void bulkAdd() throws Exception{
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();

        em.flush();
        em.clear();

    }

    //특정 조건의 해당하는 컬럼 모두 삭제
    @Test
    public void bulkDelete() throws Exception{
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();

    }

    //** SQL Function 사용하기 **
    @Test
    public void sqlFunction() throws Exception{
        List<String> result = queryFactory
                .select(
                        Expressions.stringTemplate(
                                "function('replace', {0}, {1}, {2})",
                                member.username, "member", "M")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    //소문자로 바꾸는 function
    @Test
    public void sqlFunction2() throws Exception{
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
//                .where(
//                        member.username.eq(
//                                Expressions.stringTemplate(
//                                        "function('lower', {0})", member.username
//                                )
//                        )
//                )
                .where(member.username.eq(member.username.lower()))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

}
