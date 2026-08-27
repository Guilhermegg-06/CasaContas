package br.com.casacontas.identity.api;

import br.com.casacontas.identity.application.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService service;

  public AuthController(AuthService service) {
    this.service = service;
  }

  @PostMapping("/register")
  ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    AuthResponse response =
        AuthResponse.from(service.register(request.name(), request.email(), request.password()));
    return ResponseEntity.created(URI.create("/api/v1/users/" + response.user().id()))
        .body(response);
  }

  @PostMapping("/login")
  AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return AuthResponse.from(service.login(request.email(), request.password()));
  }

  @PostMapping("/refresh")
  AuthResponse refresh(@Valid @RequestBody TokenRequest request) {
    return AuthResponse.from(service.refresh(request.refreshToken()));
  }

  @PostMapping("/logout")
  ResponseEntity<Void> logout(@Valid @RequestBody TokenRequest request) {
    service.logout(request.refreshToken());
    return ResponseEntity.noContent().build();
  }

  record RegisterRequest(
      @NotBlank @Size(max = 100) String name,
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 10, max = 72) String password) {}

  record LoginRequest(
      @NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 72) String password) {}

  record TokenRequest(@NotBlank @Size(max = 200) String refreshToken) {}

  record AuthResponse(
      String tokenType,
      String accessToken,
      long expiresIn,
      String refreshToken,
      UserResponse user) {

    static AuthResponse from(AuthService.AuthResult result) {
      return new AuthResponse(
          "Bearer",
          result.accessToken(),
          result.expiresIn(),
          result.refreshToken(),
          new UserResponse(result.userId(), result.name(), result.email()));
    }
  }

  record UserResponse(UUID id, String name, String email) {}
}
