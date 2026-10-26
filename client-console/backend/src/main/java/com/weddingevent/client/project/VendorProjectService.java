package com.weddingevent.client.project;

import com.weddingevent.common.domain.VendorProject;
import com.weddingevent.common.domain.enums.VendorProjectStatus;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/** FN-VND-04: Vendor Projects, created when a couple accepts a quote (FN-VND-03) */
@Service
public class VendorProjectService {

    public record VendorProjectDto(
            String id, String coupleName, String packageName, LocalDate eventDate, String venue, long agreedPrice,
            VendorProjectStatus status, String quoteRequestId, Instant cancelRequestedAt, String cancelReason) {

        public static VendorProjectDto of(VendorProject p) {
            return new VendorProjectDto(p.getId().toString(), p.getCoupleName(), p.getPackageName(), p.getEventDate(), p.getVenue(),
                    p.getAgreedPrice().longValueExact(), p.getStatus(), p.getQuoteRequest().getId().toString(),
                    p.getCancelRequestedAt(), p.getCancelReason());
        }
    }
}
