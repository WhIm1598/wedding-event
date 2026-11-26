package com.weddingevent.admin.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.weddingevent.common.domain.Contract;
import com.weddingevent.common.domain.StudioSettings;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ContractDocumentRendererTest {

    private final ContractDocumentRenderer renderer = new ContractDocumentRenderer();

    private static Contract contract() {
        Contract c = new Contract();
        c.setContractNumber("HD-001");
        c.setCustomerName("Khách A");
        c.setPhone("0901234567");
        c.setPackageName("Gói cưới");
        c.setTotalAmount(BigDecimal.valueOf(1_000_000));
        c.setRemainingAmount(BigDecimal.valueOf(1_000_000));
        c.setContractDate(LocalDate.of(2026, 11, 26));
        return c;
    }

    private static StudioSettings studio(boolean withLogo) throws Exception {
        StudioSettings s = new StudioSettings();
        s.setName("Studio A");
        s.setAddress("Q1");
        s.setTaxCode("0101");
        s.setLegalRepresentative("Anh A");
        s.setBankInfo("VCB");
        if (withLogo) {
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "png", png);
            s.setLogo(png.toByteArray());
            s.setLogoContentType("image/png");
        }
        return s;
    }

    @Test
    void theLogoIsInTheContractWhenTheStudioHasOne() throws Exception {
        assertThat(renderer.html(contract(), studio(true))).contains("src=\"data:image/png;base64,");
    }

    @Test
    void noLogoNoImage() throws Exception {
        assertThat(renderer.html(contract(), studio(false))).doesNotContain("<img");
    }

    @Test
    void thePdfRendersWithTheLogo() throws Exception {
        byte[] pdf = renderer.pdf(contract(), studio(true));
        assertThat(new String(Arrays.copyOf(pdf, 4), StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }
}
