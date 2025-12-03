package com.teambind.coupon.application.port.in;

import com.teambind.coupon.adapter.in.web.dto.CouponQueryRequest;
import com.teambind.coupon.adapter.in.web.dto.CouponQueryResponse;

/**
 * 유저 쿠폰 조회 UseCase
 */
public interface QueryUserCouponsUseCase {

    /**
     * 유저의 쿠폰 목록을 조회
     *
     * @param userId 유저 ID
     * @param request 조회 조건 (필터, 커서, 페이지 크기 등)
     * @return 쿠폰 목록 및 페이지네이션 정보
     */
    CouponQueryResponse queryUserCoupons(Long userId, CouponQueryRequest request);

    /**
     * 유저의 곧 만료될 쿠폰 조회
     *
     * @param userId 유저 ID
     * @param daysUntilExpiry 만료까지 남은 일수 (기본: 7일)
     * @param limit 조회 개수 (기본: 10개)
     * @return 만료 임박 쿠폰 목록
     */
    CouponQueryResponse queryExpiringCoupons(Long userId, int daysUntilExpiry, int limit);

    /**
     * 유저의 쿠폰 통계 조회
     *
     * @param userId 유저 ID
     * @return 쿠폰 통계 정보
     */
    CouponStatistics getCouponStatistics(Long userId);

    /**
     * 쿠폰 통계 정보
     */
    @lombok.Value
    @lombok.Builder
    class CouponStatistics {
        Long totalCoupons;      // 전체 쿠폰 수
        Long availableCoupons;  // 사용 가능 쿠폰 수
        Long usedCoupons;       // 사용 완료 쿠폰 수
        Long expiredCoupons;    // 만료된 쿠폰 수
        Long expiringCoupons;   // 7일 내 만료 예정 쿠폰 수
    }
}