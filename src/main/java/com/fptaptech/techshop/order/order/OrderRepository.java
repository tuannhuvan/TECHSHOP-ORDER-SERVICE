package com.fptaptech.techshop.order.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByCode(String code);

    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    @Query("SELECT FUNCTION('DATE', o.createdAt) as reportDate, COUNT(o) as orderCount, SUM(o.totalAmount) as totalRevenue " +
           "FROM Order o " +
           "WHERE o.createdAt >= :fromDate AND o.createdAt <= :toDate " +
           "GROUP BY FUNCTION('DATE', o.createdAt) " +
           "ORDER BY FUNCTION('DATE', o.createdAt) DESC")
    List<Object[]> findDailyRevenueNative(@Param("fromDate") Instant fromDate, @Param("toDate") Instant toDate);
}
