package com.weddingevent.client.notification;

import com.weddingevent.common.domain.DeviceToken;
import com.weddingevent.common.domain.enums.DevicePlatform;
import com.weddingevent.common.repository.DeviceTokenRepository;
import com.weddingevent.common.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FN-NOTI-02: the devices that receive a user's pushes */
@Service
public class DeviceService {

    static final int MAX_DEVICES = 10;

    public record RegisterDeviceRequest(
            @NotBlank(message = "Thiếu FCM token") @Size(max = 4096, message = "FCM token quá dài") String token,
            @NotNull(message = "Vui lòng chọn nền tảng ANDROID hoặc IOS") DevicePlatform platform) {}

    private final DeviceTokenRepository devices;
    private final UserRepository users;

    public DeviceService(DeviceTokenRepository devices, UserRepository users) {
        this.devices = devices;
        this.users = users;
    }

    /** Upsert by token: a device that switched accounts now belongs to the caller. Keeps the newest 10 per user. */
    @Transactional
    public void register(UUID userId, RegisterDeviceRequest req) {
        DeviceToken device = devices.findByToken(req.token()).orElseGet(DeviceToken::new);
        device.setUser(users.getReferenceById(userId));
        device.setToken(req.token());
        device.setPlatform(req.platform());
        devices.save(device);
        List<DeviceToken> mine = devices.findAllByUserIdOrderByUpdatedAtAsc(userId);
        mine.stream().filter(d -> !d.getToken().equals(req.token())).limit(Math.max(0, mine.size() - MAX_DEVICES)).forEach(devices::delete);
    }

    /** Only the caller's own token; anything else is silently ignored */
    @Transactional
    public void unregister(UUID userId, String token) {
        devices.deleteOwn(userId, token);
    }
}
