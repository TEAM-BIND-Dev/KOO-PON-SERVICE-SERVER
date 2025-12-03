package com.teambind.coupon.adapter.in.web;

import com.teambind.coupon.adapter.in.web.dto.CouponQueryRequest;
import com.teambind.coupon.adapter.in.web.dto.CouponQueryResponse;
import com.teambind.coupon.application.port.in.QueryUserCouponsUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 쿠폰 조회 API 컨트롤러
 * 유저별 쿠폰 조회, 필터링, 커서 기반 페이지네이션
 */
@Slf4j
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponQueryController {

    private final QueryUserCouponsUseCase queryUserCouponsUseCase;

    /**
     * 유저의 쿠폰 목록 조회 (커서 기반 페이지네이션)
     *
     * @param userId 유저 ID
     * @param status 쿠폰 상태 필터 (AVAILABLE, UNUSED, USED, EXPIRED)
     * @param productIds 상품 ID 리스트 (복수 선택 가능)
     * @param cursor 커서 (마지막 쿠폰 ID)
     * @param limit 조회 개수 (기본: 20, 최대: 100)
     * @return 쿠폰 목록 및 페이지네이션 정보
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<CouponQueryResponse> queryUserCoupons(
            @PathVariable Long userId,
            @RequestParam(required = false) CouponQueryRequest.CouponStatusFilter status,
            @RequestParam(required = false) List<Long> productIds,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) int limit) {

        log.info("쿠폰 조회 요청 - userId: {}, status: {}, productIds: {}, cursor: {}, limit: {}",
                userId, status, productIds, cursor, limit);

        CouponQueryRequest request = CouponQueryRequest.builder()
                .status(status)
                .productIds(productIds)
                .cursor(cursor)
                .limit(Math.min(limit, 100))  // 최대 100개 제한
                .build();

        CouponQueryResponse response = queryUserCouponsUseCase.queryUserCoupons(userId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * 유저의 만료 임박 쿠폰 조회
     *
     * @param userId 유저 ID
     * @param days 만료까지 남은 일수 (기본: 7일)
     * @param limit 조회 개수 (기본: 10개)
     * @return 만료 임박 쿠폰 목록
     */
    @GetMapping("/users/{userId}/expiring")
    public ResponseEntity<CouponQueryResponse> queryExpiringCoupons(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "7") @Min(1) int days,
            @RequestParam(defaultValue = "10") @Min(1) int limit) {

        log.info("만료 임박 쿠폰 조회 요청 - userId: {}, days: {}, limit: {}",
                userId, days, limit);

        CouponQueryResponse response = queryUserCouponsUseCase.queryExpiringCoupons(
                userId, days, Math.min(limit, 100)
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 유저의 쿠폰 통계 조회
     *
     * @param userId 유저 ID
     * @return 쿠폰 통계 정보
     */
    @GetMapping("/users/{userId}/statistics")
    public ResponseEntity<QueryUserCouponsUseCase.CouponStatistics> getCouponStatistics(
            @PathVariable Long userId) {

        log.info("쿠폰 통계 조회 요청 - userId: {}", userId);

        QueryUserCouponsUseCase.CouponStatistics statistics =
                queryUserCouponsUseCase.getCouponStatistics(userId);

        return ResponseEntity.ok(statistics);
    }

    /**
     * 쿠폰 조회 요청 DTO 방식 (POST)
     * 복잡한 필터 조건을 Body로 전달할 때 사용
     */
    @PostMapping("/users/{userId}/query")
    public ResponseEntity<CouponQueryResponse> queryUserCouponsWithBody(
            @PathVariable Long userId,
            @Valid @RequestBody CouponQueryRequest request) {

        log.info("쿠폰 조회 요청 (POST) - userId: {}, request: {}", userId, request);

        CouponQueryResponse response = queryUserCouponsUseCase.queryUserCoupons(userId, request);

        return ResponseEntity.ok(response);
    }
}