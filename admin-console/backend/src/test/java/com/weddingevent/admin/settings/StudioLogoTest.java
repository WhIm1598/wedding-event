package com.weddingevent.admin.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.weddingevent.admin.TestImages.dataUrl;
import static com.weddingevent.admin.TestImages.image;
import static com.weddingevent.admin.TestImages.png;

import com.weddingevent.common.api.ApiResponse;
import com.weddingevent.common.domain.StudioSettings;
import com.weddingevent.common.exception.AppException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class StudioLogoTest {

    @Test
    void pngRoundTrips() {
        var s = new StudioSettings();
        StudioLogo.apply(s, png(100));
        assertThat(s.getLogoContentType()).isEqualTo("image/png");
        assertThat(s.getLogo()).hasSize(100);
        assertThat(StudioLogo.dataUrl(s)).isEqualTo(png(100));
    }

    @Test
    void jpegIsAccepted() {
        var s = new StudioSettings();
        StudioLogo.apply(s, dataUrl("image/jpeg", image("jpg", 2, 2)));
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

    /** Final review: a small file must not make every contract PDF decode a huge raster on the shared server */
    @Test
    void bytesThatAreNotAnImageAreRejected() {
        assertThatThrownBy(() -> StudioLogo.apply(new StudioSettings(), dataUrl("image/png", new byte[100])))
                .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.getErrors())
                        .extracting(ApiResponse.FieldError::field).containsExactly("logo"));
    }

    @Test
    void anImageWiderOrTallerThan1024PxIsRejected() {
        assertThatThrownBy(() -> StudioLogo.apply(new StudioSettings(), dataUrl("image/png", image("png", 1025, 1))))
                .isInstanceOf(AppException.class);
        assertThatThrownBy(() -> StudioLogo.apply(new StudioSettings(), dataUrl("image/png", image("png", 1, 1025))))
                .isInstanceOf(AppException.class);
        StudioLogo.apply(new StudioSettings(), dataUrl("image/png", image("png", 1024, 1)));
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
