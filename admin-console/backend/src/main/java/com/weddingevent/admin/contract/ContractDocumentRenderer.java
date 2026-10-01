package com.weddingevent.admin.contract;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.weddingevent.common.domain.Contract;
import com.weddingevent.common.domain.StudioSettings;
import com.weddingevent.common.util.VndFormatter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Renders the A4 (210 x 297 mm) service contract as XHTML and PDF (FN-ADM-CONTR-01).
 * Payment schedule: 30% on signing, 50% on the shoot/wedding day, 20% on delivery.
 */
@Component
public class ContractDocumentRenderer {

    /** Liberation Sans ships inside PDFBox and covers Vietnamese diacritics */
    static final String FONT_RESOURCE = "/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf";
    private static final String FONT_FAMILY = "Contract Sans";

    private static final Object[][] INSTALLMENTS = {
        {"Đợt 1: Đặt cọc khi ký hợp đồng", 30},
        {"Đợt 2: Thanh toán vào ngày chụp / ngày cưới", 50},
        {"Đợt 3: Thanh toán khi nhận album & ảnh hoàn thiện", 20},
    };

    public String html(Contract c, StudioSettings studio) {
        StringBuilder schedule = new StringBuilder();
        for (Object[] row : INSTALLMENTS) {
            BigDecimal part = c.getTotalAmount().multiply(BigDecimal.valueOf((int) row[1])).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            schedule.append("<tr><td>").append(esc((String) row[0])).append(" (").append(row[1]).append("%)</td><td class=\"r\">")
                    .append(VndFormatter.vnd(part)).append("</td></tr>");
        }
        // Clause 3: scope of work & deliverables = the package's benefit list
        StringBuilder deliverables = new StringBuilder();
        if (c.getServicePackage() != null && !c.getServicePackage().getFeatures().isEmpty()) {
            deliverables.append("<ul class=\"muted\">");
            c.getServicePackage().getFeatures().forEach(f -> deliverables.append("<li>").append(esc(f)).append("</li>"));
            deliverables.append("</ul>");
        }
        String notes = deliverables + (c.getNotes() == null ? "" : "<div class=\"muted\">Ghi chú: " + esc(c.getNotes()) + "</div>");

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml"><head><meta charset="UTF-8"/><style>
                @page { size: A4; margin: 18mm 16mm; }
                body { font-family: '%s', sans-serif; font-size: 11pt; color: #1e293b; }
                .bar { height: 6px; background: #e11d48; margin-bottom: 16px; }
                .head { width: 100%%; border-bottom: 2px solid #f1f5f9; padding-bottom: 12px; margin-bottom: 16px; }
                .brand { font-size: 20pt; color: #0f172a; }
                .tag { font-size: 8pt; color: #64748b; letter-spacing: 2px; }
                h1 { text-align: center; font-size: 15pt; letter-spacing: 1px; margin: 18px 0; }
                h3 { font-size: 11pt; border-bottom: 1px solid #e2e8f0; padding-bottom: 3px; margin: 16px 0 6px; }
                table { width: 100%%; border-collapse: collapse; }
                .grid td { padding: 2px 0; width: 50%%; vertical-align: top; }
                .box td, .box th { border: 1px solid #cbd5e1; padding: 6px; }
                .box th { background: #f8fafc; text-align: left; }
                .r { text-align: right; }
                .muted { color: #64748b; font-size: 9pt; margin-top: 4px; }
                .paid { color: #047857; } .due { color: #e11d48; }
                .sign td { text-align: center; width: 50%%; padding-top: 36px; }
                .pre { white-space: pre-line; }
                </style></head><body>
                <div class="bar"></div>
                <table class="head"><tr>
                  <td><div class="brand">%s</div><div class="tag">LƯU GIỮ KHOẢNH KHẮC</div></td>
                  <td class="r">Số HĐ: %s<br/>Ngày lập: %s</td>
                </tr></table>
                <h1>HỢP ĐỒNG DỊCH VỤ CƯỚI</h1>
                <h3>ĐẠI DIỆN KHÁCH HÀNG (BÊN A)</h3>
                <table class="grid"><tr><td>Họ và tên: %s</td><td>Số điện thoại: %s</td></tr></table>
                <h3>ĐẠI DIỆN STUDIO (BÊN B)</h3>
                <table class="grid">
                  <tr><td>Đơn vị: %s</td><td>Mã số thuế: %s</td></tr>
                  <tr><td>Đại diện: %s</td><td>Địa chỉ: %s</td></tr>
                </table>
                <h3>CHI TIẾT DỊCH VỤ</h3>
                <table class="box"><tr><th>Nội dung</th><th class="r">Thành tiền</th></tr>
                  <tr><td>%s%s</td><td class="r">%s</td></tr></table>
                <h3>ĐIỀU KHOẢN THANH TOÁN</h3>
                <table class="grid">%s
                  <tr><td>Tổng giá trị hợp đồng</td><td class="r">%s</td></tr>
                  <tr class="paid"><td>Đã thanh toán</td><td class="r">%s</td></tr>
                  <tr class="due"><td>Số tiền còn lại</td><td class="r">%s</td></tr>
                </table>
                <div class="muted pre">Thông tin chuyển khoản:
                %s</div>
                <table class="sign"><tr>
                  <td>Đại diện Khách hàng<br/><span class="muted">(Ký và ghi rõ họ tên)</span></td>
                  <td>Đại diện Studio<br/><span class="muted">(Ký và ghi rõ họ tên)</span></td>
                </tr></table>
                </body></html>
                """.formatted(
                FONT_FAMILY,
                esc(studio.getName()), esc(c.getContractNumber()), VndFormatter.date(c.getContractDate()),
                esc(c.getCustomerName()), esc(c.getPhone()),
                esc(studio.getName()), esc(studio.getTaxCode()), esc(studio.getLegalRepresentative()), esc(studio.getAddress()),
                esc(c.getPackageName()), notes, VndFormatter.vnd(c.getTotalAmount()),
                schedule,
                VndFormatter.vnd(c.getTotalAmount()), VndFormatter.vnd(c.getPaidAmount()), VndFormatter.vnd(c.getRemainingAmount()),
                esc(studio.getBankInfo()));
    }

    public byte[] pdf(Contract c, StudioSettings studio) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useFont(ContractDocumentRenderer::openFont, FONT_FAMILY);
            builder.withHtmlContent(html(c, studio), null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Không thể tạo file PDF hợp đồng", e);
        }
    }

    private static InputStream openFont() {
        InputStream font = ContractDocumentRenderer.class.getResourceAsStream(FONT_RESOURCE);
        if (font == null) {
            throw new IllegalStateException("Font not found on classpath: " + FONT_RESOURCE);
        }
        return font;
    }

    private static String esc(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value, "UTF-8");
    }
}
