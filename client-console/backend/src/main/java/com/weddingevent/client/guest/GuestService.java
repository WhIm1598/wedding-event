package com.weddingevent.client.guest;

import com.weddingevent.client.user.UserService;
import com.weddingevent.common.domain.Guest;
import com.weddingevent.common.domain.WeddingPlan;
import com.weddingevent.common.domain.enums.GuestStatus;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.exception.ResourceNotFoundException;
import com.weddingevent.common.repository.GuestRepository;
import com.weddingevent.common.repository.WeddingPlanRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Predicate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FN-GUEST-01..02: guests of the caller's active plan (owner or synced partner). Status names are lower case in the API. */
@Service
public class GuestService {

    public record GuestRequest(
            @NotBlank(message = "Tên khách mời không được để trống") @Size(max = 100, message = "Tên khách mời tối đa 100 ký tự") String name,
            @Pattern(regexp = "^$|" + UserService.VN_PHONE, message = "Số điện thoại không hợp lệ") String phone,
            @Size(max = 100, message = "Tên nhóm tối đa 100 ký tự") String group,
            @Pattern(regexp = "(?i)attending|pending|declined", message = "Trạng thái chỉ có thể là attending, pending hoặc declined") String status,
            @Size(max = 500, message = "Ghi chú tối đa 500 ký tự") String notes,
            @Positive(message = "Số bàn phải lớn hơn 0") Integer tableNumber) {}

    public record GuestDto(String id, String name, String phone, String group, String status, String notes, Integer tableNumber) {
        static GuestDto of(Guest g) {
            return new GuestDto(g.getId().toString(), g.getName(), g.getPhone(), g.getGroupName(),
                    g.getStatus().name().toLowerCase(Locale.ROOT), g.getNotes(), g.getTableNumber());
        }
    }

    public record GuestStats(int total, int attending, int pending, int declined) {
        static GuestStats of(List<Guest> guests) {
            return new GuestStats(guests.size(), count(guests, GuestStatus.ATTENDING), count(guests, GuestStatus.PENDING),
                    count(guests, GuestStatus.DECLINED));
        }

        private static int count(List<Guest> guests, GuestStatus status) {
            return (int) guests.stream().filter(g -> g.getStatus() == status).count();
        }
    }

    public record GuestListDto(GuestStats stats, List<GuestDto> guests) {}

    private final GuestRepository guests;
    private final WeddingPlanRepository plans;

    public GuestService(GuestRepository guests, WeddingPlanRepository plans) {
        this.guests = guests;
        this.plans = plans;
    }

    /**
     * FN-GUEST-01. Stats always cover the whole list; filters apply to {@code guests} only. Blank parameters mean
     * "no filter". The name search is a case-insensitive substring match, so {@code %} and {@code _} are literal.
     */
    @Transactional(readOnly = true)
    public GuestListDto list(UUID userId, String status, String group, String search) {
        GuestStatus statusFilter = parseFilter(status);
        WeddingPlan plan = plans.findActiveFor(userId).orElse(null);
        if (plan == null) {
            return new GuestListDto(new GuestStats(0, 0, 0, 0), List.of());
        }
        List<Guest> all = guests.findAllByPlanIdOrderByCreatedAtDesc(plan.getId());
        Predicate<Guest> matches = g -> statusFilter == null || g.getStatus() == statusFilter;
        if (group != null && !group.isBlank()) {
            matches = matches.and(g -> group.trim().equals(g.getGroupName()));
        }
        if (search != null && !search.isBlank()) {
            String keyword = search.trim().toLowerCase(Locale.ROOT);
            matches = matches.and(g -> g.getName().toLowerCase(Locale.ROOT).contains(keyword));
        }
        return new GuestListDto(GuestStats.of(all), all.stream().filter(matches).map(GuestDto::of).toList());
    }

    @Transactional
    public GuestDto create(UUID userId, GuestRequest req) {
        WeddingPlan plan = plans.findActiveFor(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Bạn chưa có kế hoạch cưới"));
        Guest guest = new Guest();
        guest.setPlan(plan);
        apply(guest, req);
        return GuestDto.of(guests.save(guest));
    }

    @Transactional
    public GuestDto update(UUID userId, UUID guestId, GuestRequest req) {
        Guest guest = guests.findById(guestId)
                .filter(g -> plans.findActiveFor(userId).map(p -> p.getId().equals(g.getPlan().getId())).orElse(false))
                .orElseThrow(() -> new ResourceNotFoundException("khách mời", guestId));
        apply(guest, req);
        return GuestDto.of(guest);
    }

    private static void apply(Guest guest, GuestRequest req) {
        guest.setName(req.name().trim());
        guest.setPhone(blankToNull(req.phone()));
        guest.setGroupName(blankToNull(req.group()));
        guest.setStatus(req.status() == null ? GuestStatus.PENDING : GuestStatus.valueOf(req.status().toUpperCase(Locale.ROOT)));
        guest.setNotes(blankToNull(req.notes()));
        guest.setTableNumber(req.tableNumber());
    }

    private static GuestStatus parseFilter(String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("all")) {
            return null;
        }
        try {
            return GuestStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw AppException.invalidField("status", "Trạng thái chỉ có thể là all, attending, pending hoặc declined");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
