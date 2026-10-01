package com.weddingevent.admin.asset;

import com.weddingevent.common.domain.Asset;
import com.weddingevent.common.domain.AssetReservation;
import com.weddingevent.common.domain.Booking;
import com.weddingevent.common.domain.enums.AssetCategory;
import com.weddingevent.common.domain.enums.AssetStatus;
import com.weddingevent.common.domain.enums.ReservationStatus;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.exception.ResourceNotFoundException;
import com.weddingevent.common.repository.AssetRepository;
import com.weddingevent.common.repository.AssetReservationRepository;
import com.weddingevent.common.repository.BookingRepository;
import com.weddingevent.common.util.VndFormatter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Wardrobe & equipment with the maintenance-buffer lock (FN-ADM-ASSET-01). */
@Service
public class AssetService {

    public record AssetDto(
            String id, String code, String name, AssetCategory category, String size,
            AssetStatus status, LocalDate nextBookingDate, int maintenanceBufferDays) {}

    public record AssetConflictDto(String assetCode, String assetName, LocalDate date, String message) {}

    public record ReservationDto(String id, String assetCode, LocalDate startDate, LocalDate endDate, LocalDate lockEndDate) {}

    public record CreateAssetRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z]{2,5}-\\d{3,}$", message = "Mã sản phẩm có dạng VAY-001") String code,
            @NotBlank @Size(max = 200) String name,
            @NotNull AssetCategory category,
            @Size(max = 20) String size,
            @Min(0) @Max(30) int maintenanceBufferDays) {}

    /** {@code assetId} accepts either the UUID or the asset code (spec example uses "VAY-001") */
    public record ReserveRequest(
            @NotBlank String assetId,
            @NotNull LocalDate rentalStartDate,
            @NotNull LocalDate rentalEndDate,
            UUID bookingId) {}

    private final AssetRepository assets;
    private final AssetReservationRepository reservations;
    private final BookingRepository bookings;
    private final Clock clock;

    public AssetService(AssetRepository assets, AssetReservationRepository reservations, BookingRepository bookings, Clock clock) {
        this.assets = assets;
        this.reservations = reservations;
        this.bookings = bookings;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AssetDto> list(AssetCategory category) {
        LocalDate today = LocalDate.now(clock);
        Map<UUID, List<AssetReservation>> byAsset = activeReservations(today);
        List<Asset> items = category == null ? assets.findAllByOrderByCodeAsc() : assets.findByCategoryOrderByCodeAsc(category);
        return items.stream().map(a -> toDto(a, byAsset.getOrDefault(a.getId(), List.of()), today)).toList();
    }

    /**
     * Status derived from rentals: inside [start, end] -> IN_USE, inside (end, lockEnd] -> MAINTENANCE,
     * otherwise the manually set status.
     */
    public static AssetStatus effectiveStatus(Asset asset, List<AssetReservation> active, LocalDate today) {
        for (AssetReservation r : active) {
            if (!today.isBefore(r.getStartDate()) && !today.isAfter(r.getEndDate())) {
                return AssetStatus.IN_USE;
            }
            if (today.isAfter(r.getEndDate()) && !today.isAfter(r.getLockEndDate())) {
                return AssetStatus.MAINTENANCE;
            }
        }
        return asset.getStatus();
    }

    /** Pairs of reservations of the same item whose lock windows overlap (e.g. imported or legacy data) */
    @Transactional(readOnly = true)
    public List<AssetConflictDto> conflicts() {
        List<AssetConflictDto> result = new ArrayList<>();
        activeReservations(LocalDate.now(clock)).forEach((assetId, list) -> {
            for (int i = 1; i < list.size(); i++) {
                AssetReservation prev = list.get(i - 1);
                AssetReservation next = list.get(i);
                if (!next.getStartDate().isAfter(prev.getLockEndDate())) {
                    Asset a = next.getAsset();
                    result.add(new AssetConflictDto(a.getCode(), a.getName(), next.getStartDate(),
                            "Sản phẩm \"" + a.getCode() + " (" + a.getName() + ")\" đang được xếp cho 2 lịch thuê chồng lấn: "
                                    + VndFormatter.date(prev.getStartDate()) + " và " + VndFormatter.date(next.getStartDate())
                                    + " (bảo dưỡng đến " + VndFormatter.date(prev.getLockEndDate()) + "). Vui lòng kiểm tra lại lịch."));
                }
            }
        });
        return result;
    }

    @Transactional
    public AssetDto create(CreateAssetRequest req) {
        String code = req.code().toUpperCase(Locale.ROOT);
        if (assets.existsByCodeIgnoreCase(code)) {
            throw AppException.conflict("ASSET_CODE_EXISTS", "Mã sản phẩm " + code + " đã tồn tại");
        }
        Asset a = new Asset();
        a.setCode(code);
        a.setName(req.name().trim());
        a.setCategory(req.category());
        a.setSize(req.size() == null || req.size().isBlank() ? "N/A" : req.size().trim());
        a.setMaintenanceBufferDays(req.maintenanceBufferDays());
        return toDto(assets.save(a), List.of(), LocalDate.now(clock));
    }

    /**
     * FN-ADM-ASSET-01: lock range = [rentalStart, rentalEnd + buffer]; 409 ASSET_NOT_AVAILABLE on overlap.
     * The asset row is locked (SELECT ... FOR UPDATE) so concurrent rentals are serialized.
     */
    @Transactional
    public ReservationDto reserve(ReserveRequest req) {
        if (req.rentalEndDate().isBefore(req.rentalStartDate())) {
            throw AppException.badRequest("BAD_REQUEST", "Ngày trả phải sau hoặc bằng ngày nhận");
        }
        Asset asset = assets.findByIdForUpdate(resolveAssetId(req.assetId()))
                .orElseThrow(() -> new ResourceNotFoundException("sản phẩm", req.assetId()));
        LocalDate lockEnd = req.rentalEndDate().plusDays(asset.getMaintenanceBufferDays());

        List<AssetReservation> overlapping =
                reservations.findOverlapping(asset.getId(), req.rentalStartDate(), lockEnd, ReservationStatus.CONFIRMED);
        if (!overlapping.isEmpty()) {
            LocalDate busyUntil = overlapping.getFirst().getLockEndDate();
            throw AppException.conflict("ASSET_NOT_AVAILABLE",
                    "Trang phục " + asset.getCode() + " đang vướng lịch bảo dưỡng giặt hấp đến ngày "
                            + VndFormatter.date(busyUntil) + ", vui lòng chọn ngày khác hoặc trang phục khác.");
        }

        Booking booking = req.bookingId() == null ? null
                : bookings.findById(req.bookingId()).orElseThrow(() -> new ResourceNotFoundException("lịch hẹn", req.bookingId()));
        AssetReservation r = new AssetReservation();
        r.setAsset(asset);
        r.setBooking(booking);
        r.setStartDate(req.rentalStartDate());
        r.setEndDate(req.rentalEndDate());
        r.setLockEndDate(lockEnd);
        r = reservations.save(r);
        return new ReservationDto(r.getId().toString(), asset.getCode(), r.getStartDate(), r.getEndDate(), r.getLockEndDate());
    }

    private UUID resolveAssetId(String idOrCode) {
        try {
            return UUID.fromString(idOrCode);
        } catch (IllegalArgumentException notUuid) {
            return assets.findByCodeIgnoreCase(idOrCode.trim())
                    .map(Asset::getId)
                    .orElseThrow(() -> new ResourceNotFoundException("sản phẩm", idOrCode));
        }
    }

    private Map<UUID, List<AssetReservation>> activeReservations(LocalDate today) {
        return reservations.findByStatusAndLockEndDateGreaterThanEqualOrderByStartDateAsc(ReservationStatus.CONFIRMED, today)
                .stream()
                .collect(Collectors.groupingBy(r -> r.getAsset().getId()));
    }

    private static AssetDto toDto(Asset a, List<AssetReservation> active, LocalDate today) {
        LocalDate next = active.stream()
                .map(AssetReservation::getStartDate)
                .filter(d -> !d.isBefore(today))
                .min(Comparator.naturalOrder())
                .orElse(null);
        return new AssetDto(a.getId().toString(), a.getCode(), a.getName(), a.getCategory(), a.getSize(),
                effectiveStatus(a, active, today), next, a.getMaintenanceBufferDays());
    }
}
