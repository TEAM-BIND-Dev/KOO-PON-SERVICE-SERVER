# ADR-001: 쿠폰 조회 기능의 쿼리 전략 결정

## 상태
승인됨 (Accepted)

## 날짜
2024-12-02

## 컨텍스트

쿠폰 서비스에서 유저별 쿠폰 조회 기능을 구현해야 한다. 주요 요구사항은:
- 유저ID별 쿠폰 리스트 조회
- 커서 기반 페이지네이션 (무한스크롤)
- 복잡한 필터링 (상태, 복수 상품ID)
- 대용량 데이터 처리 성능

### 검토한 옵션들

#### 옵션 1: QueryDSL 도입
**장점:**
- 타입 세이프한 쿼리 작성
- 동적 쿼리 구성 용이
- IDE 자동완성 지원
- 데이터베이스 독립적

**단점:**
- 추가 의존성 및 설정 필요
- Q클래스 생성 등 빌드 복잡도 증가
- 학습 곡선 존재

#### 옵션 2: JPA Specification
**장점:**
- 추가 의존성 불필요
- Spring Data JPA 표준 기능

**단점:**
- 복잡한 쿼리 작성 어려움
- 가독성 떨어짐
- PostgreSQL 특화 기능 활용 어려움

#### 옵션 3: PostgreSQL Native Query
**장점:**
- PostgreSQL 고급 기능 활용 (배열 연산, Window Function)
- 최적화된 성능
- 명확한 SQL 쿼리
- 추가 의존성 불필요

**단점:**
- 데이터베이스 종속적
- 타입 세이프하지 않음

## 결정

**PostgreSQL Native Query 사용 (옵션 3)**

## 이유

1. **제한적인 쿼리 패턴**
   - 쿠폰 서비스의 복잡한 쿼리는 2-3개로 제한적
   - 대시보드 통계 (이미 구현)
   - 유저별 쿠폰 조회 (구현 예정)

2. **비용 대비 효과**
   - 2-3개 쿼리를 위한 QueryDSL 도입은 오버엔지니어링
   - Native Query로 충분히 관리 가능한 복잡도

3. **PostgreSQL 최적화**
   - 이미 PostgreSQL을 사용 중 (데이터베이스 변경 계획 없음)
   - 배열 연산자(&&)로 복수 상품ID 필터링 최적화
   - Window Function으로 커서 페이징 성능 향상

4. **팀 역량**
   - 팀이 PostgreSQL과 SQL에 익숙함
   - QueryDSL 학습 비용 절감

## 구현 예시

```java
@Repository
public interface CouponIssueQueryRepository {

    @Query(value = """
        SELECT ci.*, cp.coupon_name, cp.discount_value
        FROM coupon_issues ci
        JOIN coupon_policies cp ON ci.policy_id = cp.id
        WHERE ci.user_id = :userId
          AND (:status IS NULL OR ci.status = :status::coupon_status)
          AND (:productIds IS NULL OR cp.applicable_product_ids && :productIds::bigint[])
          AND (:cursor IS NULL OR ci.id < :cursor)
          AND ci.expires_at > CURRENT_TIMESTAMP
        ORDER BY ci.expires_at ASC, ci.id DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<CouponIssueProjection> findUserCouponsWithCursor(
        @Param("userId") Long userId,
        @Param("status") String status,
        @Param("productIds") String productIds,
        @Param("cursor") Long cursor,
        @Param("limit") int limit
    );
}
```

## 결과

### 긍정적 결과
- 빠른 구현 가능
- 최적화된 성능
- 유지보수 복잡도 감소
- 불필요한 의존성 없음

### 부정적 결과
- PostgreSQL 종속성 (수용 가능)
- 타입 세이프티 부족 (테스트로 보완)

## 향후 고려사항

만약 다음과 같은 상황이 발생하면 재검토:
- 복잡한 동적 쿼리가 10개 이상으로 증가
- 데이터베이스 변경 필요성 발생
- 여러 개발자가 복잡한 쿼리 작성 필요

## 참고자료
- [PostgreSQL Array Operators](https://www.postgresql.org/docs/current/functions-array.html)
- [Cursor-based Pagination](https://use-the-index-luke.com/no-offset)
- [Spring Data JPA Native Query](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.at-query)