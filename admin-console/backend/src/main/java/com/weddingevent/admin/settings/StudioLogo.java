package com.weddingevent.admin.settings;

import com.weddingevent.common.domain.StudioSettings;
import com.weddingevent.common.exception.AppException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The studio logo travels in JSON as a data URL; PNG/JPEG only, because the contract PDF (PDFBox) can't draw WebP. */
public final class StudioLogo {

    public static final int MAX_BYTES = 204_800;
    private static final Pattern DATA_URL = Pattern.compile("^data:(image/png|image/jpeg);base64,(.+)$");

    private StudioLogo() {}

    /** {@code null} or {@code ""} removes the logo */
    public static void apply(StudioSettings s, String dataUrl) {
        if (dataUrl == null || dataUrl.isEmpty()) {
            s.setLogo(null);
            s.setLogoContentType(null);
            return;
        }
        Matcher m = DATA_URL.matcher(dataUrl);
        if (!m.matches()) {
            throw invalid();
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(m.group(2));
        } catch (IllegalArgumentException e) {
            throw invalid();
        }
        if (bytes.length > MAX_BYTES) {
            throw invalid();
        }
        s.setLogo(bytes);
        s.setLogoContentType(m.group(1));
    }

    public static String dataUrl(StudioSettings s) {
        if (s == null || s.getLogo() == null) {
            return null;
        }
        return "data:" + s.getLogoContentType() + ";base64," + Base64.getEncoder().encodeToString(s.getLogo());
    }

    private static AppException invalid() {
        return AppException.invalidField("logo", "Logo phải là ảnh PNG hoặc JPEG, tối đa 200 KB");
    }
}
