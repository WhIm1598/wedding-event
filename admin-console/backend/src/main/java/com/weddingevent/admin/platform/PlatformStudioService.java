package com.weddingevent.admin.platform;

import com.weddingevent.admin.user.UserAccountService;
import com.weddingevent.admin.user.UserAccountService.CreateUserRequest;
import com.weddingevent.admin.user.UserAccountService.CreatedAccountDto;
import com.weddingevent.common.domain.Studio;
import com.weddingevent.common.domain.StudioSettings;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.domain.enums.StudioStatus;
import com.weddingevent.common.exception.ResourceNotFoundException;
import com.weddingevent.common.repository.StudioRepository;
import com.weddingevent.common.repository.StudioSettingsRepository;
import com.weddingevent.common.repository.UserRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    public record StudioDto(String id, String name, StudioStatus status, LocalDate subscriptionExpiresAt, boolean usable,
            long adminCount, Instant createdAt) {}

    public record CreateStudioRequest(
            @NotBlank @Size(max = 200) String name,
            LocalDate subscriptionExpiresAt,
            @NotBlank @Email @Size(max = 255) String adminEmail,
            @NotBlank @Size(max = 100) String adminName) {}

    public record CreatedStudioDto(StudioDto studio, CreatedAccountDto admin) {}

    /** Both fields are sent; {@code subscriptionExpiresAt = null} means no end date */
    public record UpdateStudioRequest(@NotNull StudioStatus status, LocalDate subscriptionExpiresAt) {}

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
        Studio studio = new Studio();
        studio.setSubscriptionExpiresAt(req.subscriptionExpiresAt());
        studio = studios.saveAndFlush(studio); // flushed so createdAt is set for the response

        StudioSettings profile = new StudioSettings();
        profile.setStudioId(studio.getId());
        profile.setName(req.name().trim());
        profile.setAddress(TO_FILL);
        profile.setTaxCode(TO_FILL);
        profile.setLegalRepresentative(TO_FILL);
        profile.setBankInfo(TO_FILL);
        settings.save(profile);

        CreatedAccountDto admin = accounts.createIn(studio.getId(),
                new CreateUserRequest(req.adminEmail(), req.adminName(), Role.ROLE_ADMIN, null));
        return new CreatedStudioDto(toDto(studio, profile), admin);
    }

    @Transactional
    public StudioDto update(UUID id, UpdateStudioRequest req) {
        Studio studio = require(id);
        studio.setStatus(req.status());
        studio.setSubscriptionExpiresAt(req.subscriptionExpiresAt());
        return toDto(studio, settings.findById(id).orElse(null));
    }

    /** Another admin for a studio, e.g. when its only admin lost access */
    @Transactional
    public CreatedAccountDto createAdmin(UUID id, CreateStudioAdminRequest req) {
        require(id);
        return accounts.createIn(id, new CreateUserRequest(req.email(), req.name(), Role.ROLE_ADMIN, null));
    }

    private Studio require(UUID id) {
        return studios.findById(id).orElseThrow(() -> new ResourceNotFoundException("studio", id));
    }

    private StudioDto toDto(Studio s, StudioSettings profile) {
        return new StudioDto(s.getId().toString(), profile == null ? null : profile.getName(), s.getStatus(),
                s.getSubscriptionExpiresAt(), s.isUsable(LocalDate.now(clock)), users.countByStudioIdAndRole(s.getId(), Role.ROLE_ADMIN),
                s.getCreatedAt());
    }
}
