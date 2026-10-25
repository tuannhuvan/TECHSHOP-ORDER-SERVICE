package com.fptaptech.techshop.order.order;

import com.fptaptech.techshop.order.common.BadRequestException;
import com.fptaptech.techshop.order.common.OrderNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;

    public InvoiceService(InvoiceRepository invoiceRepository, OrderRepository orderRepository) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
    }

    public InvoiceResponse uploadInvoice(String orderCode, MultipartFile file) {
        Order order = orderRepository.findByCode(orderCode)
                .orElseThrow(() -> new OrderNotFoundException(orderCode));

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Tập tin hóa đơn không được để trống");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BadRequestException("Kích thước tệp vượt quá 5MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new BadRequestException("Hệ thống chỉ chấp nhận định dạng PDF");
        }

        try {
            byte[] fileBytes = file.getBytes();
            Invoice invoice = invoiceRepository.findByOrderCode(orderCode)
                    .orElse(new Invoice());

            invoice.setOrderCode(orderCode);
            invoice.setFileName(originalFilename);
            invoice.setContentType("application/pdf");
            invoice.setFileData(fileBytes);
            invoice.setFileSize(file.getSize());

            Invoice saved = invoiceRepository.save(invoice);
            return new InvoiceResponse(saved.getOrderCode(), saved.getFileName(), saved.getFileSize(), saved.getUploadedAt());
        } catch (IOException e) {
            throw new BadRequestException("Không thể đọc tệp PDF: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public byte[] getInvoicePdf(String orderCode) {
        Order order = orderRepository.findByCode(orderCode)
                .orElseThrow(() -> new OrderNotFoundException(orderCode));

        Optional<Invoice> invoiceOpt = invoiceRepository.findByOrderCode(orderCode);
        if (invoiceOpt.isPresent()) {
            return invoiceOpt.get().getFileData();
        }

        // Dynamically generate a clean minimal PDF receipt for the order if not uploaded yet
        return generateDynamicReceiptPdf(order);
    }

    private byte[] generateDynamicReceiptPdf(Order order) {
        // Minimal valid PDF structure
        String content = "TechShop E-Commerce Invoice\n"
                + "Order Code: " + order.getCode() + "\n"
                + "Status: " + order.getStatus() + "\n"
                + "Shipping Address: " + order.getShippingAddress() + "\n"
                + "Total: " + order.getTotalAmount() + " " + order.getCurrency() + "\n"
                + "Created: " + (order.getCreatedAt() != null ? order.getCreatedAt().toString() : "N/A") + "\n";

        StringBuilder sb = new StringBuilder();
        sb.append("%PDF-1.4\n");
        sb.append("1 0 obj <</Type /Catalog /Pages 2 0 R>> endobj\n");
        sb.append("2 0 obj <</Type /Pages /Kids [3 0 R] /Count 1>> endobj\n");
        sb.append("3 0 obj <</Type /Page /Parent 2 0 R /Resources <</Font <</F1 4 0 R>>>> /MediaBox [0 0 612 792] /Contents 5 0 R>> endobj\n");
        sb.append("4 0 obj <</Type /Font /Subtype /Type1 /BaseFont /Helvetica>> endobj\n");

        String streamText = "BT /F1 14 Tf 50 720 Td (TECHSHOP ORDER INVOICE) Tj ET\n"
                + "BT /F1 11 Tf 50 690 Td (Order Code: " + order.getCode() + ") Tj ET\n"
                + "BT /F1 11 Tf 50 670 Td (Total Amount: " + order.getTotalAmount() + " " + order.getCurrency() + ") Tj ET\n"
                + "BT /F1 11 Tf 50 650 Td (Status: " + order.getStatus() + ") Tj ET\n"
                + "BT /F1 11 Tf 50 630 Td (Payment Method: " + (order.getPayment() != null ? order.getPayment().getMethod() : "COD") + ") Tj ET\n"
                + "BT /F1 11 Tf 50 610 Td (Thank you for shopping at TechShop!) Tj ET\n";

        sb.append("5 0 obj <</Length ").append(streamText.length()).append(">> stream\n");
        sb.append(streamText);
        sb.append("endstream\nendobj\n");
        sb.append("xref\n0 6\n0000000000 65535 f \n0000000009 00000 n \n0000000058 00000 n \n0000000115 00000 n \n0000000262 00000 n \n0000000339 00000 n \n");
        sb.append("trailer <</Size 6 /Root 1 0 R>>\nstartxref\n500\n%%EOF");

        return sb.toString().getBytes(StandardCharsets.ISO_8859_1);
    }
}
