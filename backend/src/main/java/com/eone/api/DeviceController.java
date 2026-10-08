package com.eone.api;
import com.eone.domain.DeviceToken;
import com.eone.repository.DeviceTokenRepository;
import com.eone.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/devices") @RequiredArgsConstructor @Transactional
public class DeviceController {
    private final DeviceTokenRepository devices;
    public record Registration(@NotBlank @Size(min=20,max=512) String token) {}
    @PostMapping public void register(@Valid @RequestBody Registration request, Authentication auth) {
        var token=devices.findByToken(request.token()).orElseGet(DeviceToken::new);
        token.setUserId(CurrentUser.id(auth)); token.setToken(request.token());
        token.setUpdatedAt(java.time.Instant.now()); devices.save(token);
    }
    @DeleteMapping public void remove(@Valid @RequestBody Registration request, Authentication auth) {
        devices.deleteByUserIdAndToken(CurrentUser.id(auth),request.token());
    }
}
