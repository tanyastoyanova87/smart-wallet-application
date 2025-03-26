package app.user.service;

import app.exception.DomainException;
import app.notification.service.NotificationService;
import app.security.AuthenticationMetaData;
import app.subscription.service.SubscriptionService;
import app.user.model.User;
import app.user.model.UserRole;
import app.user.repository.UserRepository;
import app.web.dto.EditRequest;
import app.web.dto.RegisterRequest;
import app.wallet.service.WalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SubscriptionService subscriptionService;
    private final WalletService walletService;
    private final NotificationService notificationService;

    @Autowired
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       SubscriptionService service,
                       WalletService walletService, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.subscriptionService = service;
        this.walletService = walletService;
        this.notificationService = notificationService;
    }

    @Transactional
    public User register(RegisterRequest registerRequest) {
        Optional<User> optionalUser = this.userRepository.findByUsername(registerRequest.getUsername());
        if (optionalUser.isPresent()) {
            throw new DomainException("Username [%s] already exist."
                    .formatted(registerRequest.getUsername()));
        }

        User user = initializeUser(registerRequest);
        this.userRepository.save(user);

        this.subscriptionService.createDefaultSubscription(user);
        this.walletService.createDefaultWallet(user);

        notificationService.saveNotificationPreference(user.getId(), false, null);

        log.info("Successfully created new user account for username [%s] and [%s]."
                .formatted(user.getUsername(), user.getId()));

        return user;
    }

    public void editProfile(UUID userId, EditRequest editRequest) {
        User user = getById(userId);

        user.setFirstName(editRequest.getFirstName());
        user.setLastName(editRequest.getLastName());
        user.setEmail(editRequest.getEmail());
        user.setProfilePicture(editRequest.getProfilePicture());

        String email = user.getEmail();
        if (email.isEmpty()) {
            user.setEmail(null);
        }

        if (!editRequest.getEmail().isBlank()) {
            notificationService.saveNotificationPreference(userId, true, editRequest.getEmail());
        } else {
            notificationService.saveNotificationPreference(userId, false, null);
        }

        this.userRepository.save(user);
    }

    private User initializeUser(RegisterRequest registerRequest) {
        return User.builder()
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(UserRole.USER)
                .isActive(true)
                .country(registerRequest.getCountry())
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
    }

    public List<User> getAllUsers() {
        return this.userRepository.findAll();
    }

    public User getById(UUID id) {
        return this.userRepository.findById(id).orElseThrow(() ->
                new DomainException("User with id [%s] does not exist.".formatted(id)));
    }

    public void switchStatus(UUID id) {
        User user = getById(id);

        user.setActive(!user.isActive());
        this.userRepository.save(user);
    }

    public void switchRole(UUID id) {
        User user = getById(id);

        if (user.getRole() == UserRole.USER) {
            user.setRole(UserRole.ADMIN);
        } else {
            user.setRole(UserRole.USER);
        }

        this.userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = this.userRepository.findByUsername(username).orElseThrow(() -> new DomainException("User with this username does not exist."));

        return new AuthenticationMetaData(user.getId(), user.getUsername(), user.getPassword(),
                user.getRole(), user.isActive());
    }
}
