package org.laioffer.planner.user.authentication;

import org.laioffer.planner.user.authentication.model.LoginRequest;
import org.laioffer.planner.user.authentication.model.LoginResponse;
import org.laioffer.planner.user.authentication.model.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

  private final AuthenticationService authenticationService;

  public AuthenticationController(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public void register(@Valid @RequestBody RegisterRequest body) {
    authenticationService.register(body.email(), body.password(), body.role(), body.username(),
        body.firstName(), body.lastName());
  }

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest body) {
    return new LoginResponse(authenticationService.login(body.email(), body.password()));
  }
}
