package com.weddingevent.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

        // Paying the rest settles the debt but does not complete the contract: nothing has been delivered yet (§1.6)
        mvc.perform(authed(post("/api/v1/admin/financials/transactions"), admin).content(income(10_000_000, contractId, TODAY)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").isString());
        assertThat(contractField(admin, contractId, "status")).isEqualTo("DEPOSITED");
        assertThat(contractField(admin, contractId, "remainingAmount")).isEqualTo(0);
        mvc.perform(authed(get("/api/v1/admin/notifications"), admin))
                .andExpect(jsonPath("$.data[*].title", hasItem("Hợp đồng đã thanh toán đủ ✅")));

        // Nothing is left to pay
        mvc.perform(authed(post("/api/v1/admin/financials/transactions"), admin).content(income(1_000, contractId, TODAY)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAYMENT_EXCEEDS_REMAINING"));
    }

    @Test
    void customerStartsWorkWhenTheDepositArrivesNotWhenTheDraftIsWritten() throws Exception {
        String admin = adminToken();
        String phone = uniquePhone();
        String leadId = createLead(admin, phone);
        mvc.perform(authed(patch("/api/v1/admin/crm/customers/" + leadId + "/stage"), admin).content(json("{'stage':'AWAITING_DEPOSIT'}")))
                .andExpect(status().isOk());

        // A draft contract (no deposit) leaves the customer waiting for the deposit
        String contractId = createContract(admin, phone, 10_000_000, 0);
        assertThat(contractField(admin, contractId, "status")).isEqualTo("DRAFT");
        assertThat(leadStage(admin, leadId)).isEqualTo("AWAITING_DEPOSIT");

        // The first payment makes the contract effective and the customer "in progress"
        mvc.perform(authed(post("/api/v1/admin/financials/transactions"), admin).content(income(3_000_000, contractId, TODAY)))
                .andExpect(status().isCreated());
        assertThat(contractField(admin, contractId, "status")).isEqualTo("DEPOSITED");
        assertThat(leadStage(admin, leadId)).isEqualTo("IN_PROGRESS");
    }

    @Test
    void contractCreatedWithADepositMovesTheCustomerRightAway() throws Exception {
        String admin = adminToken();
        String phone = uniquePhone();
        String leadId = createLead(admin, phone);
        createContract(admin, phone, 10_000_000, 3_000_000);
        assertThat(leadStage(admin, leadId)).isEqualTo("IN_PROGRESS");
    }

    @Test
    void contractCompletesOnlyWhenPaidInFullAndDelivered() throws Exception {
        String admin = adminToken();
        String phone = uniquePhone();
        String leadId = createLead(admin, phone);
        String contractId = createContract(admin, phone, 10_000_000, 4_000_000);

        // Delivery needs work in progress: a DEPOSITED contract can't be delivered yet
        mvc.perform(authed(post("/api/v1/admin/contracts/" + contractId + "/deliver"), admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        moveBooking(admin, createContractBooking(admin, contractId), "IN_PROGRESS");
        mvc.perform(authed(post("/api/v1/admin/financials/transactions"), admin).content(income(6_000_000, contractId, TODAY)))
                .andExpect(status().isCreated());
        assertThat(contractField(admin, contractId, "status")).isEqualTo("IN_PROGRESS");

        // Paid in full + delivered -> completed, and so is the customer
        mvc.perform(authed(post("/api/v1/admin/contracts/" + contractId + "/deliver"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.deliveredAt").isString());
        assertThat(leadStage(admin, leadId)).isEqualTo("COMPLETED");

        mvc.perform(authed(post("/api/v1/admin/contracts/" + contractId + "/deliver"), admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
        mvc.perform(authed(post("/api/v1/admin/financials/transactions"), admin).content(income(1_000, contractId, TODAY)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONTRACT_NOT_PAYABLE"));
    }

    @Test
    void deliveredWithDebtCompletesOnTheFinalPayment() throws Exception {
        String admin = adminToken();
        String contractId = createContract(admin, uniquePhone(), 10_000_000, 4_000_000);
        moveBooking(admin, createContractBooking(admin, contractId), "IN_PROGRESS");

        // Delivered but still owing: the contract stays in progress and shows the delivery
        mvc.perform(authed(post("/api/v1/admin/contracts/" + contractId + "/deliver"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.remainingAmount").value(6_000_000))
                .andExpect(jsonPath("$.data.deliveredAt").isString());

        mvc.perform(authed(post("/api/v1/admin/financials/transactions"), admin).content(income(6_000_000, contractId, TODAY)))
                .andExpect(status().isCreated());
        assertThat(contractField(admin, contractId, "status")).isEqualTo("COMPLETED");
    }

    @Test
    void customerStaysInProgressWhileAnotherContractIsOpen() throws Exception {
        String admin = adminToken();
        String phone = uniquePhone();
        String leadId = createLead(admin, phone);
        String done = createContract(admin, phone, 5_000_000, 5_000_000);
        createContract(admin, phone, 8_000_000, 1_000_000);

        moveBooking(admin, createContractBooking(admin, done), "IN_PROGRESS");
        mvc.perform(authed(post("/api/v1/admin/contracts/" + done + "/deliver"), admin))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        assertThat(leadStage(admin, leadId)).isEqualTo("IN_PROGRESS");
    }

    @Test
    void shootBeforeTheDepositStartsTheContractWhenTheDepositArrives() throws Exception {
        String admin = adminToken();
        String contractId = createContract(admin, uniquePhone(), 10_000_000, 0);
        moveBooking(admin, createContractBooking(admin, contractId), "IN_PROGRESS");
        assertThat(contractField(admin, contractId, "status")).isEqualTo("DRAFT");

        // The work already started, so the deposit makes the contract effective AND in progress: it can be delivered
        mvc.perform(authed(post("/api/v1/admin/financials/transactions"), admin).content(income(10_000_000, contractId, TODAY)))
                .andExpect(status().isCreated());
        assertThat(contractField(admin, contractId, "status")).isEqualTo("IN_PROGRESS");
        mvc.perform(authed(post("/api/v1/admin/contracts/" + contractId + "/deliver"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void anAbandonedDraftDoesNotKeepTheCustomerFromCompleting() throws Exception {
        String admin = adminToken();
        String phone = uniquePhone();
        String leadId = createLead(admin, phone);
        createContract(admin, phone, 3_000_000, 0); // drafted, never signed
        String signed = createContract(admin, phone, 5_000_000, 5_000_000);

        moveBooking(admin, createContractBooking(admin, signed), "IN_PROGRESS");
        mvc.perform(authed(post("/api/v1/admin/contracts/" + signed + "/deliver"), admin))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        assertThat(leadStage(admin, leadId)).isEqualTo("COMPLETED");
    }

    @Test
    void staffCannotDeliverContracts() throws Exception {
        String contractId = createContract(adminToken(), uniquePhone(), 1_000_000, 0);
        mvc.perform(authed(post("/api/v1/admin/contracts/" + contractId + "/deliver"), staffToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void transactionDateCannotBeInTheFuture() throws Exception {
        mvc.perform(authed(post("/api/v1/admin/financials/transactions"), adminToken()).content(json("""
                        {'type':'EXPENSE','amount':100000,'category':'Chi phí vận hành','description':'Ghi trước','transactionDate':'%s'}
                        """.formatted(TODAY.plusDays(1)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
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
