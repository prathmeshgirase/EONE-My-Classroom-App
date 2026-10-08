package com.eone.api;
import com.eone.service.RecoveryService;
import com.eone.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/v1") @RequiredArgsConstructor
public class RecoveryController {
    private final RecoveryService recovery;
    public record Request(@Email @NotBlank @Size(max=190) String email){}
    public record Reset(@NotBlank @Size(max=128) String token,@NotBlank @Size(max=72) String password){}
    public record Change(@NotBlank @Size(max=72) String currentPassword,@NotBlank @Size(max=72) String password){}
    @PostMapping("/auth/password/request") public Map<String,String> request(@Valid @RequestBody Request request){
        recovery.request(request.email());return Map.of("message","If the account is eligible, recovery instructions will arrive by email.");
    }
    @PostMapping("/auth/password/reset") public void reset(@Valid @RequestBody Reset request){recovery.reset(request.token(),request.password());}
    @PostMapping("/me/password") public void change(@Valid @RequestBody Change request,Authentication auth){recovery.change(CurrentUser.id(auth),request.currentPassword(),request.password());}
}
