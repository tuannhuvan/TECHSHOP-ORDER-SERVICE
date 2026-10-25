package com.fptaptech.techshop.order.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final OrderRepository orderRepository;

    public ReportService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public DailyRevenueReport getDailyRevenueReport(String fromStr, String toStr) {
        LocalDate toDate = (toStr != null && !toStr.isBlank()) ? LocalDate.parse(toStr) : LocalDate.now();
        LocalDate fromDate = (fromStr != null && !fromStr.isBlank()) ? LocalDate.parse(fromStr) : toDate.minusDays(30);

        Instant fromInstant = fromDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toInstant = toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(fromInstant) && o.getCreatedAt().isBefore(toInstant))
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .toList();

        Map<String, List<Order>> ordersByDate = new TreeMap<>(Comparator.reverseOrder());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

        for (Order o : orders) {
            String dateKey = formatter.format(o.getCreatedAt());
            ordersByDate.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(o);
        }

        BigDecimal grandTotal = BigDecimal.ZERO;
        long totalOrdersCount = 0;
        List<DailyRevenueReport.DailyRevenueItem> items = new ArrayList<>();

        for (Map.Entry<String, List<Order>> entry : ordersByDate.entrySet()) {
            String date = entry.getKey();
            List<Order> dayOrders = entry.getValue();
            long count = dayOrders.size();
            BigDecimal dayRevenue = dayOrders.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            grandTotal = grandTotal.add(dayRevenue);
            totalOrdersCount += count;

            items.add(new DailyRevenueReport.DailyRevenueItem(date, count, dayRevenue));
        }

        return new DailyRevenueReport(
                fromDate.toString(),
                toDate.toString(),
                grandTotal,
                totalOrdersCount,
                items
        );
    }
}
