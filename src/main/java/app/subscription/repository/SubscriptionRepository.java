package app.subscription.repository;

import app.subscription.model.Subscription;
import app.subscription.model.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByStatusAndOwnerId(SubscriptionStatus status, UUID ownerId);
}
