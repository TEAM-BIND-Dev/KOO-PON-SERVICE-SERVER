package com.teambind.coupon.application.service;

import com.teambind.coupon.adapter.in.web.dto.CouponQueryRequest;
import com.teambind.coupon.adapter.in.web.dto.CouponQueryResponse;
import com.teambind.coupon.adapter.out.persistence.projection.CouponIssueProjection;
import com.teambind.coupon.adapter.out.persistence.repository.CouponIssueQueryRepository;
import com.teambind.coupon.application.port.in.QueryUserCouponsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 쿠폰 조회 서비스
 * 유저별 쿠폰 조회, 필터링, 커서 기반 페이지네이션 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponQueryService implements QueryUserCouponsUseCase {

    private final CouponIssueQueryRepository couponIssueQueryRepository;

    @Override
    public CouponQueryResponse queryUserCoupons(Long userId, CouponQueryRequest request) {
        log.info("쿠폰 조회 시작 - userId: {}, status: {}, productIds: {}, cursor: {}, limit: {}",
                userId, request.getStatus(), request.getProductIds(), request.getCursor(), request.getLimit());

        // limit + 1개 조회하여 다음 페이지 존재 여부 확인
        int fetchLimit = request.getLimit() + 1;

        List<CouponIssueProjection> coupons = couponIssueQueryRepository.findUserCouponsWithCursor(
                userId,
                request.getStatusForQuery(),
                request.getProductIdsAsPostgresArray(),
                request.getCursor(),
                fetchLimit
        );

        CouponQueryResponse response = CouponQueryResponse.of(coupons, request.getLimit());

        log.info("쿠폰 조회 완료 - userId: {}, 조회 개수: {}, hasNext: {}",
                userId, response.getCount(), response.isHasNext());

        return response;
    }

    @Override
    public CouponQueryResponse queryExpiringCoupons(Long userId, int daysUntilExpiry, int limit) {
        log.info("만료 임박 쿠폰 조회 - userId: {}, daysUntilExpiry: {}, limit: {}",
                userId, daysUntilExpiry, limit);

        List<CouponIssueProjection> expiringCoupons = couponIssueQueryRepository.findExpiringCoupons(
                userId, daysUntilExpiry, limit
        );

        CouponQueryResponse response = CouponQueryResponse.of(expiringCoupons, limit);

        log.info("만료 임박 쿠폰 조회 완료 - userId: {}, 조회 개수: {}",
                userId, response.getCount());

        return response;
    }

    @Override
    @Cacheable(value = "couponStatistics", key = "#userId", cacheManager = "cacheManager")
    public CouponStatistics getCouponStatistics(Long userId) {
        log.info("쿠폰 통계 조회 - userId: {}", userId);

        // 각 상태별 쿠폰 개수 조회
        Long totalCoupons = couponIssueQueryRepository.countUserCoupons(userId, null, null);
        Long availableCoupons = couponIssueQueryRepository.countUserCoupons(userId, "AVAILABLE", null);
        Long usedCoupons = couponIssueQueryRepository.countUserCoupons(userId, "USED", null);
        Long expiredCoupons = couponIssueQueryRepository.countUserCoupons(userId, "EXPIRED", null);

        // 7일 내 만료 예정 쿠폰 조회
        List<CouponIssueProjection> expiringCoupons = couponIssueQueryRepository.findExpiringCoupons(
                userId, 7, Integer.MAX_VALUE
        );

        CouponStatistics statistics = CouponStatistics.builder()
                .totalCoupons(totalCoupons)
                .availableCoupons(availableCoupons)
                .usedCoupons(usedCoupons)
                .expiredCoupons(expiredCoupons)
                .expiringCoupons((long) expiringCoupons.size())
                .build();

        log.info("쿠폰 통계 조회 완료 - userId: {}, total: {}, available: {}, used: {}, expired: {}",
                userId, totalCoupons, availableCoupons, usedCoupons, expiredCoupons);

        return statistics;
    }
}