package org.laioffer.planner.user.authentication;

import org.laioffer.planner.entity.UserEntity;
import org.laioffer.planner.repository.UserRepository;
import org.laioffer.planner.user.model.UserRole;
import org.laioffer.planner.user.security.JwtHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

  private final AuthenticationManager authenticationManager;
  private final JwtHandler jwtHandler;
  private final PasswordEncoder passwordEncoder;
  private final UserRepository userRepository;

  public AuthenticationService(AuthenticationManager authenticationManager, JwtHandler jwtHandler,
      PasswordEncoder passwordEncoder, UserRepository userRepository) {
    this.authenticationManager = authenticationManager;
    this.jwtHandler = jwtHandler;
    this.passwordEncoder = passwordEncoder;
    this.userRepository = userRepository;
  }

  public UserEntity register(String email, String password, UserRole role, String username,
      String firstName, String lastName) throws UserAlreadyExistException {
    if (userRepository.existsByEmail(email)) {
      throw new UserAlreadyExistException();
    }

    UserEntity userEntity = new UserEntity(role, email, username,
        passwordEncoder.encode(password));
    return userRepository.save(userEntity);
  }

  public String login(String email, String password) {
    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
    UserEntity user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("User not found"));
    return jwtHandler.generateToken(email);
  }
}
