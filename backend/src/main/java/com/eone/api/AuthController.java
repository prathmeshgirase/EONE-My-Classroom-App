package com.eone.api;

import com.eone.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/auth") @RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;
    private final RefreshTokenService refresh;

    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public Dtos.UserView register(@Valid @RequestBody Dtos.RegisterRequest req) { return auth.register(req); }

    @PostMapping("/login")
    public Dtos.AuthResponse login(@Valid @RequestBody Dtos.LoginRequest req) { return auth.login(req); }

    @PostMapping("/refresh")
    public Dtos.AuthResponse refresh(@Valid @RequestBody Dtos.RefreshRequest req) { return auth.refresh(req.refreshToken()); }

    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody Dtos.RefreshRequest req) { refresh.revoke(req.refreshToken()); }
}
