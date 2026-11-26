package com.weddingevent.admin.platform;

import com.weddingevent.admin.settings.StudioLogo;
import com.weddingevent.admin.user.UserAccountService;
import com.weddingevent.admin.user.UserAccountService.CreateUserRequest;
import com.weddingevent.admin.user.UserAccountService.CreatedAccountDto;
import com.weddingevent.common.domain.Studio;
import com.weddingevent.common.domain.StudioSettings;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.domain.enums.StudioStatus;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.exception.ResourceNotFoundException;
import com.weddingevent.common.repository.StudioRepository;
import com.weddingevent.common.repository.StudioSettingsRepository;
import com.weddingevent.common.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FN-PLT-01..03 (ADR-0001): the platform admin creates studios with their first admin, suspends and extends them. */
@Service
public class PlatformStudioService {

    /** Placeholder for the settings the studio's admin fills in (the columns are NOT NULL) */
    static final String TO_FILL = "Chưa cập nhật";

    /** {@code usable}: its people can sign in today (active and not past the subscription) */
    public record StudioDto(String id, String name, String logo, StudioStatus status, LocalDate subscriptionExpiresAt, boolean usable,
            long adminCount, Instant createdAt) {}

    public record NewAdmin(@NotBlank @Email @Size(max = 255) String email, @NotBlank @Size(max = 100) String name) {}

    /** Blank contract details stay "Chưa cập nhật" for the studio's manager to fill in (FN-ADM-SET-01) */
    public record CreateStudioRequest(
            @NotBlank @Size(max = 200) String name,
            LocalDate subscriptionExpiresAt,
            String logo,
            @Size(max = 255) String address,
            @Size(max = 20) String taxCode,
            @Size(max = 100) String legalRepresentative,
            @Size(max = 500) String bankInfo,
            @NotNull @Size(min = 1, max = 10) List<@Valid NewAdmin> admins) {}

    public record CreatedStudioDto(StudioDto studio, List<CreatedAccountDto> admins) {}

    /**
     * {@code status} and {@code subscriptionExpiresAt} are always sent ({@code null} = no end date).
     * {@code name} and {@code logo}: {@code null} keeps them, {@code logo = ""} removes the logo.
     */
    public record UpdateStudioRequest(@NotNull StudioStatus status, LocalDate subscriptionExpiresAt,
            @Size(max = 200) String name, String logo) {}

    public record CreateStudioAdminRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(max = 100) String name) {}

    private final StudioRepository studios;
    private final StudioSettingsRepository settings;
    private final UserRepository users;
    private final UserAccountService accounts;
    private final Clock clock;

    public PlatformStudioService(StudioRepository studios, StudioSettingsRepository settings, UserRepository users,
            UserAccountService accounts, Clock clock) {
        this.studios = studios;
        this.settings = settings;
        this.users = users;
        this.accounts = accounts;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<StudioDto> list() {
        List<Studio> all = studios.findAll().stream().sorted(Comparator.comparing(Studio::getCreatedAt)).toList();
        Map<UUID, StudioSettings> profiles = settings.findAllById(all.stream().map(Studio::getId).toList()).stream()
                .collect(Collectors.toMap(StudioSettings::getStudioId, Function.identity()));
        return all.stream().map(s -> toDto(s, profiles.get(s.getId()))).toList();
    }

    /** Studio, its settings row and its first admin (temporary password) in one transaction */
    @Transactional
    public CreatedStudioDto create(CreateStudioRequest req) {
        Set<String> emails = new HashSet<>();
        for (NewAdmin a : req.admins()) {
            if (!emails.add(a.email().trim().toLowerCase(Locale.ROOT))) {
                throw AppException.invalidField("admins", "Email quản lý bị trùng trong danh sách");
            }
        }
        Studio studio = new Studio();
        studio.setSubscriptionExpiresAt(req.subscriptionExpiresAt());
        studio = studios.saveAndFlush(studio); // flushed so createdAt is set for the response

        StudioSettings profile = new StudioSettings();
        profile.setStudioId(studio.getId());
        profile.setName(req.name().trim());
        profile.setAddress(orToFill(req.address()));
        profile.setTaxCode(orToFill(req.taxCode()));
        profile.setLegalRepresentative(orToFill(req.legalRepresentative()));
        profile.setBankInfo(orToFill(req.bankInfo()));
        StudioLogo.apply(profile, req.logo());
        settings.save(profile);

        // A taken email throws 409 and rolls the whole studio back
        UUID studioId = studio.getId();
        List<CreatedAccountDto> admins = req.admins().stream()
                .map(a -> accounts.createIn(studioId, new CreateUserRequest(a.email(), a.name(), Role.ROLE_ADMIN, null)))
                .toList();
        return new CreatedStudioDto(toDto(studio, profile), admins);
    }

    @Transactional
    public StudioDto update(UUID id, UpdateStudioRequest req) {
        Studio studio = require(id);
        studio.setStatus(req.status());
        studio.setSubscriptionExpiresAt(req.subscriptionExpiresAt());
        if (req.name() == null && req.logo() == null) {
            return toDto(studio, settings.findById(id).orElse(null));
        }
        // Locked: Hibernate writes every column, which would undo a Zalo OA refresh token rotated meanwhile
        StudioSettings profile = settings.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("cấu hình studio", id));
        if (req.name() != null) {
            if (req.name().isBlank()) {
                throw AppException.invalidField("name", "Cần tên studio");
            }
            profile.setName(req.name().trim());
        }
        if (req.logo() != null) {
            StudioLogo.apply(profile, req.logo());
        }
        return toDto(studio, profile);
    }

    /** Another admin for a studio, e.g. when its only admin lost access */
    @Transactional
    public CreatedAccountDto createAdmin(UUID id, CreateStudioAdminRequest req) {
        require(id);
        return accounts.createIn(id, new CreateUserRequest(req.email(), req.name(), Role.ROLE_ADMIN, null));
    }

    private static String orToFill(String value) {
        return value == null || value.isBlank() ? TO_FILL : value.trim();
    }

    private Studio require(UUID id) {
        return studios.findById(id).orElseThrow(() -> new ResourceNotFoundException("studio", id));
    }

    private StudioDto toDto(Studio s, StudioSettings profile) {
        return new StudioDto(s.getId().toString(), profile == null ? null : profile.getName(), StudioLogo.dataUrl(profile), s.getStatus(),
                s.getSubscriptionExpiresAt(), s.isUsable(LocalDate.now(clock)), users.countByStudioIdAndRole(s.getId(), Role.ROLE_ADMIN),
                s.getCreatedAt());
    }
}
