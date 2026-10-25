package com.fptaptech.techshop.order.order;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;

    public DataInitializer(
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            OrderRepository orderRepository,
            OrderEventRepository orderEventRepository) {
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            return;
        }

        // Seed Products
        Product p1 = productRepository.save(new Product(
                "Bàn phím cơ Keychron K8 Pro",
                "Switch Gateron Brown, layout 87 phím, Bluetooth 5.1 & Type-C, RGB LED",
                new BigDecimal("1590000.00"),
                "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=500&q=80"
        ));

        Product p2 = productRepository.save(new Product(
                "Chuột không dây Logitech MX Master 3S",
                "Cảm biến Darkfield 8000 DPI, cuộn MagSpeed siêu tốc, kết nối đa thiết bị",
                new BigDecimal("2490000.00"),
                "https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=500&q=80"
        ));

        Product p3 = productRepository.save(new Product(
                "Tai nghe Sony WH-1000XM5",
                "Chống ồn chủ động đỉnh cao, thời lượng pin 30h, âm thanh Hi-Res LDAC",
                new BigDecimal("6990000.00"),
                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&q=80"
        ));

        Product p4 = productRepository.save(new Product(
                "Màn hình Dell UltraSharp 27 inch 4K (U2723QE)",
                "Tấm nền IPS Black độ tương phản 2000:1, 98% DCI-P3, Type-C 90W PD",
                new BigDecimal("12500000.00"),
                "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=500&q=80"
        ));

        Product p5 = productRepository.save(new Product(
                "Laptop Gaming ASUS ROG Zephyrus G16",
                "Intel Core Ultra 9, RTX 4070 8GB, 32GB RAM, 1TB SSD, Màn hình OLED 240Hz",
                new BigDecimal("45990000.00"),
                "https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=500&q=80"
        ));

        // Seed Customers
        Customer c1 = customerRepository.save(new Customer("Nguyễn Văn An", "customer@techshop.vn", "0901234567"));
        Customer c2 = customerRepository.save(new Customer("Trần Thị Bích", "bich.tran@example.com", "0912345678"));

        // Seed Sample Orders
        // 1. Order CONFIRMED
        Order o1 = new Order();
        o1.setCode("ORD-2026-000101");
        o1.setCustomerId(c1.getId());
        o1.setStatus(OrderStatus.CONFIRMED);
        o1.setShippingAddress("Số 8 Tôn Thất Thuyết, Mỹ Đình 2, Cầu Giấy, Hà Nội");
        o1.setPayment(Payment.card("Visa", "4242"));
        o1.setCurrency("VND");
        o1.setVersion(1L);
        o1.addItem(new OrderItem(p1.getId(), p1.getName(), p1.getPrice(), 1));
        o1.addItem(new OrderItem(p2.getId(), p2.getName(), p2.getPrice(), 1));
        o1.recalculateTotalAmount();
        o1 = orderRepository.save(o1);
        orderEventRepository.save(new OrderEvent(o1, OrderStatus.PENDING, OrderStatus.PENDING, OrderAction.CONFIRM, "customer@techshop.vn", "Khởi tạo đơn"));
        orderEventRepository.save(new OrderEvent(o1, OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderAction.CONFIRM, "admin@techshop.vn", "Xác nhận thanh toán thẻ"));

        // 2. Order DELIVERED (within 7 days)
        Order o2 = new Order();
        o2.setCode("ORD-2026-000102");
        o2.setCustomerId(c1.getId());
        o2.setStatus(OrderStatus.DELIVERED);
        o2.setShippingAddress("Tòa Landmark 81, 720A Điện Biên Phủ, Bình Thạnh, TP.HCM");
        o2.setPayment(Payment.cod());
        o2.setCurrency("VND");
        o2.setVersion(3L);
        o2.setDeliveredAt(Instant.now().minus(2, ChronoUnit.DAYS));
        o2.addItem(new OrderItem(p3.getId(), p3.getName(), p3.getPrice(), 1));
        o2.recalculateTotalAmount();
        o2 = orderRepository.save(o2);
        orderEventRepository.save(new OrderEvent(o2, OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderAction.CONFIRM, "admin@techshop.vn", "Xác nhận đơn COD"));
        orderEventRepository.save(new OrderEvent(o2, OrderStatus.CONFIRMED, OrderStatus.SHIPPING, OrderAction.SHIP, "partner@techshop.carrier", "Giao bưu tá"));
        orderEventRepository.save(new OrderEvent(o2, OrderStatus.SHIPPING, OrderStatus.DELIVERED, OrderAction.DELIVER, "partner@techshop.carrier", "Giao thành công"));

        // 3. Order PENDING
        Order o3 = new Order();
        o3.setCode("ORD-2026-000103");
        o3.setCustomerId(c2.getId());
        o3.setStatus(OrderStatus.PENDING);
        o3.setShippingAddress("Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội");
        o3.setPayment(Payment.bankTransfer("VCB", "REF-20261025-99"));
        o3.setCurrency("VND");
        o3.setVersion(0L);
        o3.addItem(new OrderItem(p4.getId(), p4.getName(), p4.getPrice(), 1));
        o3.recalculateTotalAmount();
        o3 = orderRepository.save(o3);
        orderEventRepository.save(new OrderEvent(o3, OrderStatus.PENDING, OrderStatus.PENDING, OrderAction.CONFIRM, "bich.tran@example.com", "Chờ chuyển khoản"));
    }
}
