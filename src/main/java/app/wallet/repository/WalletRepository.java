package app.wallet.repository;

import app.wallet.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    List<Wallet> findAllWalletsByOwnerUsername(String username);

    Optional<Wallet> findByIdAndOwnerId(UUID walletId, UUID ownerId);
}
