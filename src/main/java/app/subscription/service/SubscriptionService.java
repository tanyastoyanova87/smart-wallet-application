package app.subscription.service;

import app.exception.DomainException;
import app.subscription.model.Subscription;
import app.subscription.model.SubscriptionPeriod;
import app.subscription.model.SubscriptionStatus;
import app.subscription.model.SubscriptionType;
import app.subscription.repository.SubscriptionRepository;
import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.user.model.User;
import app.wallet.service.WalletService;
import app.web.dto.UpgradeRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final WalletService walletService;

    @Autowired
    public SubscriptionService(SubscriptionRepository subscriptionRepository, WalletService walletService) {
        this.subscriptionRepository = subscriptionRepository;
        this.walletService = walletService;
    }

    public void createDefaultSubscription(User user) {
        Subscription subscription = initializeSubscription(user);

        this.subscriptionRepository.save(subscription);
        log.info("Successfully created new subscription with id [%s] and type [%s]."
                .formatted(subscription.getId(), subscription.getType().name()));
    }

    private Subscription initializeSubscription(User user) {
        LocalDateTime now = LocalDateTime.now();

        return Subscription.builder()
                .owner(user)
                .status(SubscriptionStatus.ACTIVE)
                .period(SubscriptionPeriod.MONTHLY)
                .type(SubscriptionType.DEFAULT)
                .price(new BigDecimal("0.00"))
                .renewalAllowed(true)
                .createdOn(now)
                .completedOn(now.plusMonths(1))
                .build();
    }

    @Transactional
    public Transaction upgrade(User user, UpgradeRequest upgradeRequest, SubscriptionType subscriptionType) {
        Optional<Subscription> optionalSubscription = this.subscriptionRepository.findByStatusAndOwnerId(SubscriptionStatus.ACTIVE, user.getId());
        if (optionalSubscription.isEmpty()) {
            throw new DomainException("No active subscription found for user with id [%s]".formatted(user.getId()));
        }

        Subscription currentSubscription = optionalSubscription.get();

        SubscriptionPeriod subscriptionPeriod = upgradeRequest.getSubscriptionPeriod();
        BigDecimal subscriptionPrice = getSubscriptionPrice(subscriptionPeriod, subscriptionType);

        String subscriptionTypeFormatted = subscriptionType.name().substring(0, 1).toUpperCase() + subscriptionType.name().substring(1).toLowerCase();
        String subscriptionPeriodFormatted = subscriptionPeriod.name().substring(0, 1).toUpperCase() + subscriptionPeriod.name().substring(1).toLowerCase();

        String chargeDescription = "Purchase of %s %s subscription".formatted(subscriptionPeriodFormatted, subscriptionTypeFormatted);
        Transaction charge = this.walletService.charge(user, upgradeRequest.getWalletId(), subscriptionPrice, chargeDescription);

        if (charge.getStatus() == TransactionStatus.FAILED) {
            log.warn("Charge for subscription failed for user %s".formatted(user.getId()));
            return charge;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime completedOn = null;
        if (subscriptionPeriod == SubscriptionPeriod.MONTHLY) {
            completedOn = now.plusMonths(1);
        } else if (subscriptionPeriod == SubscriptionPeriod.YEARLY){
            completedOn = now.plusYears(1);
        }

        Subscription newSubscription = Subscription.builder()
                .owner(user)
                .status(SubscriptionStatus.ACTIVE)
                .period(subscriptionPeriod)
                .type(subscriptionType)
                .price(subscriptionPrice)
                .renewalAllowed(subscriptionPeriod == SubscriptionPeriod.MONTHLY)
                .createdOn(now)
                .completedOn(completedOn)
                .build();

        currentSubscription.setStatus(SubscriptionStatus.COMPLETED);
        currentSubscription.setCompletedOn(now);

        this.subscriptionRepository.save(currentSubscription);
        this.subscriptionRepository.save(newSubscription);

        return charge;
    }

    private BigDecimal getSubscriptionPrice(SubscriptionPeriod subscriptionPeriod, SubscriptionType subscriptionType) {
        if (subscriptionType == SubscriptionType.DEFAULT) {
            return BigDecimal.ZERO;
        } else if (subscriptionType == SubscriptionType.PREMIUM && subscriptionPeriod == SubscriptionPeriod.MONTHLY) {
            return new BigDecimal("19.99");
        } else if (subscriptionType == SubscriptionType.PREMIUM && subscriptionPeriod == SubscriptionPeriod.YEARLY) {
            return new BigDecimal("199.99");
        } else if (subscriptionType == SubscriptionType.ULTIMATE && subscriptionPeriod == SubscriptionPeriod.MONTHLY) {
            return new BigDecimal("49.99");
        } else {
            return new BigDecimal("499.99");
        }
    }
}
