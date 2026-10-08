package com.eone.service;

import com.eone.api.ApiException;
import com.eone.api.Dtos;
import com.eone.domain.Department;
import com.eone.domain.User;
import com.eone.domain.UserStatus;
import com.eone.repository.DepartmentRepository;
import com.eone.repository.UserRepository;
import com.eone.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final DepartmentRepository departments;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;

    @Transactional
    public Dtos.UserView register(Dtos.RegisterRequest req) {

        String email = req.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (users.findByEmailIgnoreCase(email).isPresent()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Email is already registered"
            );
        }

        Department department = departments
                .findByCodeIgnoreCase(req.departmentCode())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Unknown department code"
                ));

        User user = new User();

        user.setEmail(email);
        user.setPasswordHash(
                passwords.encode(req.password())
        );
        user.setFirstName(
                req.firstName().trim()
        );
        user.setLastName(
                req.lastName().trim()
        );
        user.setPhone(req.phone());
        user.setAccountType(req.accountType());
        user.setStatus(UserStatus.PENDING);
        user.setDepartment(department);

        return DtoMapper.user(
                users.save(user)
        );
    }

    @Transactional
    public Dtos.AuthResponse login(Dtos.LoginRequest req) {

        String email = req.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = users
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"
                ));

        if (!passwords.matches(
                req.password(),
                user.getPasswordHash()
        )) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "Account is not active. Ask your department HOD for approval."
            );
        }

        return tokens(user);
    }

    @Transactional
    public Dtos.AuthResponse refresh(
            String rawRefreshToken
    ) {

        User user = refreshTokens.consume(
                rawRefreshToken
        );

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "Account is not active"
            );
        }

        return tokens(user);
    }

    private Dtos.AuthResponse tokens(User user) {

        String accessToken =
                jwt.createAccessToken(
                        user.getId()
                );

        String refreshToken =
                refreshTokens.issue(user);

        Dtos.UserView userView =
                DtoMapper.user(user);

        return new Dtos.AuthResponse(
                accessToken,
                refreshToken,
                userView
        );
    }
}