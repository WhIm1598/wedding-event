package com.weddingevent.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ContractFinanceApiTest extends IntegrationTest {

    /** "Today" in the studio's business time zone (app.zone) */
    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));

    private String createContract(String admin, String phone, long total, long deposit) throws Exception {
        String pkgs = mvc.perform(authed(get("/api/v1/admin/packages"), admin)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String pkgId = read(pkgs, "$.data[0].id");
        String body = mvc.perform(authed(post("/api/v1/admin/contracts"), admin).content(json("""
                        {'customerName':'Phạm Thu Hà','phone':'%s','servicePackageId':'%s','totalAmount':%d,'depositAmount':%d}
                        """.formatted(phone, pkgId, total, deposit))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return read(body, "$.data.id");
    }

    @Test
    void depositAndPaymentsDriveContractDebtAndStatus() throws Exception {
        String admin = adminToken();
        String contractId = createContract(admin, uniquePhone(), 20_000_000, 6_000_000);

        // Deposit is booked into the cash book on creation (FN-ADM-FIN-01)
        mvc.perform(authed(get("/api/v1/admin/contracts"), admin))
                .andExpect(jsonPath("$.data[?(@.id=='%s')].status".formatted(contractId), hasItem("DEPOSITED")))
                .andExpect(jsonPath("$.data[?(@.id=='%s')].remainingAmount".formatted(contractId), hasItem(14_000_000)));
        mvc.perform(authed(get("/api/v1/admin/financials/transactions"), admin))
                .andExpect(jsonPath("$.data[*].contractId", hasItem(contractId)));

        // Paying more than the remaining debt is rejected
        mvc.perform(authed(post("/api/v1/admin/financials/transactions"), admin).content(income(15_000_000, contractId, TODAY)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAYMENT_EXCEEDS_REMAINING"));

        // A partial payment is announced in the notification bell
        mvc.perform(authed(post("/api/v1/admin/financials/transactions"), admin).content(income(4_000_000, contractId, TODAY)))
                .andExpect(status().isCreated());
        mvc.perform(authed(get("/api/v1/admin/notifications"), admin))
                .andExpect(jsonPath("$.data[*].title", hasItem("Đã thu tiền 💰")));

        // Paying the rest completes the contract
        mvc.perform(authed(post("/api/v1/admin/financials/transactions"), admin).content(income(10_000_000, contractId, TODAY)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").isString());
        mvc.perform(authed(get("/api/v1/admin/contracts"), admin))
                .andExpect(jsonPath("$.data[?(@.id=='%s')].status".formatted(contractId), hasItem("COMPLETED")))
                .andExpect(jsonPath("$.data[?(@.id=='%s')].remainingAmount".formatted(contractId), hasItem(0)));

        // A completed contract can't take more money
        mvc.perform(authed(post("/api/v1/admin/financials/transactions"), admin).content(income(1_000, contractId, TODAY)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONTRACT_NOT_PAYABLE"));
    }

    @Test
    void transactionDateCannotBeInTheFuture() throws Exception {
        mvc.perform(authed(post("/api/v1/admin/financials/transactions"), adminToken()).content(json("""
                        {'type':'EXPENSE','amount':100000,'category':'Chi phí vận hành','description':'Ghi trước','transactionDate':'%s'}
                        """.formatted(TODAY.plusDays(1)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    private static String income(long amount, String contractId, LocalDate date) {
        return json("""
                {'type':'INCOME','amount':%d,'category':'Doanh thu HĐ','description':'Thanh toán','contractId':'%s','transactionDate':'%s'}
                """.formatted(amount, contractId, date));
    }

    @Test
    void depositCannotExceedTotal() throws Exception {
        String admin = adminToken();
        String pkgs = mvc.perform(authed(get("/api/v1/admin/packages"), admin)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        mvc.perform(authed(post("/api/v1/admin/contracts"), admin).content(json("""
                        {'customerName':'X','phone':'%s','servicePackageId':'%s','totalAmount':1000000,'depositAmount':2000000}
                        """.formatted(uniquePhone(), read(pkgs, "$.data[0].id")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DEPOSIT_EXCEEDS_TOTAL"));
    }

    @Test
    void contractExportsAsA4PdfWithVietnameseText() throws Exception {
        String admin = adminToken();
        String contractId = createContract(admin, uniquePhone(), 12_500_000, 0);

        byte[] pdf = mvc.perform(authed(get("/api/v1/admin/contracts/" + contractId + "/export-pdf"), staffToken()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".pdf")))
                .andReturn().getResponse().getContentAsByteArray();

        try (PDDocument doc = Loader.loadPDF(pdf)) {
            // A4 = 595 x 842 pt
            assertThat(doc.getPage(0).getMediaBox().getWidth()).isCloseTo(595f, org.assertj.core.data.Offset.offset(1f));
            assertThat(doc.getPage(0).getMediaBox().getHeight()).isCloseTo(842f, org.assertj.core.data.Offset.offset(1f));
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("HỢP ĐỒNG DỊCH VỤ CƯỚI", "Phạm Thu Hà", "12.500.000đ", "Đợt 1: Đặt cọc khi ký hợp đồng");
            // Clause 3: the package's deliverables (packages[0] = most expensive demo package, "Gói Kim Cương")
            assertThat(text).contains("1 Quay Phim");
        }
    }

    @Test
    void staffCannotCreateContracts() throws Exception {
        mvc.perform(authed(post("/api/v1/admin/contracts"), staffToken()).content("{}"))
                .andExpect(status().isForbidden());
    }
}
