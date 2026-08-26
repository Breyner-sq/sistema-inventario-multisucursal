package com.inventario.multisucursal.auth;

import com.inventario.multisucursal.users.User;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * docs/API_DESIGN.md, sección 7.1: {@code POST /auth/login} (público) y
 * {@code GET /auth/me} (requiere JWT válido, protegido por la regla
 * {@code anyRequest().authenticated()} de {@link SecurityConfig} — no
 * necesita ninguna anotación de autorización adicional en este método).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException ex) {
            // BadCredentialsException (password incorrecta), UsernameNotFoundException
            // (email inexistente) y DisabledException (cuenta inactiva) se tratan
            // todas igual a propósito - ver InvalidCredentialsException.
            throw new InvalidCredentialsException();
        }

        User user = ((AppUserDetails) authentication.getPrincipal()).getUser();
        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(user.getId(), user.getName(), user.getEmail(), user.getRoleCode(), user.getBranchId());
        String token = jwtService.generateToken(authenticatedUser);

        return new LoginResponse(token, jwtService.getExpirationSeconds(), UserSummaryResponse.from(user));
    }

    @GetMapping("/me")
    public UserSummaryResponse me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return UserSummaryResponse.from(currentUser);
    }
}
