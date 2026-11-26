package com.weddingevent.admin.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.weddingevent.common.api.ApiResponse;
import com.weddingevent.common.domain.StudioSettings;
import com.weddingevent.common.exception.AppException;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class StudioLogoTest {

    private static String png(int bytes) {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(new byte[bytes]);
    }

    @Test
    void pngRoundTrips() {
        var s = new StudioSettings();
        StudioLogo.apply(s, png(10));
        assertThat(s.getLogoContentType()).isEqualTo("image/png");
        assertThat(s.getLogo()).hasSize(10);
        assertThat(StudioLogo.dataUrl(s)).isEqualTo(png(10));
    }

    @Test
    void jpegIsAccepted() {
        var s = new StudioSettings();
        StudioLogo.apply(s, "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(new byte[3]));
        assertThat(s.getLogoContentType()).isEqualTo("image/jpeg");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void nullOrEmptyClears(String value) {
        var s = new StudioSettings();
        StudioLogo.apply(s, png(5));
        StudioLogo.apply(s, value);
        assertThat(s.getLogo()).isNull();
        assertThat(s.getLogoContentType()).isNull();
        assertThat(StudioLogo.dataUrl(s)).isNull();
    }

    @Test
    void exactly200KbIsAccepted() {
        var s = new StudioSettings();
        StudioLogo.apply(s, png(204_800));
        assertThat(s.getLogo()).hasSize(StudioLogo.MAX_BYTES);
    }

    @ParameterizedTest
    @ValueSource(strings = {"data:image/webp;base64,AAAA", "data:image/svg+xml;base64,AAAA", "data:IMAGE/PNG;base64,AAAA",
        "data:image/png;base64,AA\nAA", "data:image/png;base64,***", "image/png;base64,AAAA", "data:image/png,AAAA"})
    void rejected(String value) {
        assertThatThrownBy(() -> StudioLogo.apply(new StudioSettings(), value))
                .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.getErrors())
                        .extracting(ApiResponse.FieldError::field).containsExactly("logo"));
    }

    @Test
    void over200KbIsRejected() {
        assertThatThrownBy(() -> StudioLogo.apply(new StudioSettings(), png(204_801))).isInstanceOf(AppException.class);
    }

    @Test
    void noSettingsNoLogo() {
        assertThat(StudioLogo.dataUrl(null)).isNull();
    }
}
