package app.wallet.service;

import app.exception.DomainException;
import app.subscription.model.Subscription;
import app.subscription.model.SubscriptionType;
import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.transaction.model.TransactionType;
import app.transaction.service.TransactionService;
import app.user.model.User;
import app.wallet.model.Wallet;
import app.wallet.model.WalletStatus;
import app.wallet.repository.WalletRepository;
import app.web.dto.TransferRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class WalletService {

    private static final String SMART_WALLET_LTD = "Smart Wallet Ltd";

    private final WalletRepository walletRepository;
    private final TransactionService transactionService;

    @Autowired
    public WalletService(WalletRepository walletRepository,
                         TransactionService transactionService) {
        this.walletRepository = walletRepository;
        this.transactionService = transactionService;
    }

    public Transaction transferFunds(User sender, TransferRequest transferRequest) {
        Wallet senderWallet = getWalletById(transferRequest.getFromWalletId());

        Optional<Wallet> optionalWallet = this.walletRepository
                .findAllWalletsByOwnerUsername(transferRequest.getUsernameReceiver())
                .stream().filter(wallet -> wallet.getStatus() == WalletStatus.ACTIVE)
                .findFirst();

        String description = "Transfer from %s to %s, for %.2f EUR."
                .formatted(sender.getUsername(), transferRequest.getUsernameReceiver(), transferRequest.getAmount());

        if (optionalWallet.isEmpty()) {
            return transactionService.createNewTransaction(sender,
                    senderWallet.getId().toString(),
                    transferRequest.getUsernameReceiver(),
                    transferRequest.getAmount(),
                    senderWallet.getBalance(),
                    senderWallet.getCurrency(),
                    TransactionType.WITHDRAWAL,
                    TransactionStatus.FAILED,
                    description,
                    "Invalid criteria for transfer");
        }

        Transaction withdrawal = charge(sender, senderWallet.getId(), transferRequest.getAmount(), description);

        if (withdrawal.getStatus() == TransactionStatus.FAILED) {
            return withdrawal;
        }

        Wallet receiverWallet = optionalWallet.get();
        receiverWallet.setBalance(receiverWallet.getBalance().add(transferRequest.getAmount()));
        receiverWallet.setUpdatedOn(LocalDateTime.now());

        this.walletRepository.save(receiverWallet);

        return transactionService.createNewTransaction(receiverWallet.getOwner(),
                senderWallet.getId().toString(),
                receiverWallet.getId().toString(),
                transferRequest.getAmount(),
                receiverWallet.getBalance(),
                receiverWallet.getCurrency(),
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCEEDED,
                description,
                null);
    }

    @Transactional
    public Transaction charge(User user, UUID walletId, BigDecimal amount, String description) {
        Wallet wallet = getWalletById(walletId);

        String failureReason = null;
        boolean transactionFailed = false;
        if (wallet.getStatus() == WalletStatus.INACTIVE) {
            failureReason = "This wallet is inactive";
            transactionFailed = true;
        }

        if (wallet.getBalance().compareTo(amount) < 0) {
            failureReason = "Insufficient funds";
            transactionFailed = true;
        }

        if (transactionFailed) {
            return this.transactionService.createNewTransaction(
                    user,
                    wallet.getId().toString(),
                    SMART_WALLET_LTD,
                    amount,
                    wallet.getBalance(),
                    wallet.getCurrency(),
                    TransactionType.WITHDRAWAL,
                    TransactionStatus.FAILED,
                    description,
                    failureReason
            );
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setUpdatedOn(LocalDateTime.now());
        this.walletRepository.save(wallet);

        return this.transactionService.createNewTransaction(
                user,
                wallet.getId().toString(),
                SMART_WALLET_LTD,
                amount,
                wallet.getBalance(),
                wallet.getCurrency(),
                TransactionType.WITHDRAWAL,
                TransactionStatus.SUCCEEDED,
                description,
                null
        );
    }

    @Transactional
    public Transaction topUp(UUID walletId, BigDecimal amount) {
        Optional<Wallet> optionalWallet = this.walletRepository.findById(walletId);
        if (optionalWallet.isEmpty()) {
            throw new DomainException("Wallet with id [%s] does not exist.".formatted(walletId));
        }

        Wallet wallet = optionalWallet.get();
        String transactionDescription = "Top up %.2f".formatted(amount.doubleValue());

        if (wallet.getStatus() == WalletStatus.INACTIVE) {
            return this.transactionService.createNewTransaction(wallet.getOwner(),
                    SMART_WALLET_LTD,
                    walletId.toString(),
                    amount,
                    wallet.getBalance(),
                    wallet.getCurrency(),
                    TransactionType.DEPOSIT,
                    TransactionStatus.FAILED,
                    transactionDescription,
                    "Inactive wallet");
        }

        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setUpdatedOn(LocalDateTime.now());

        walletRepository.save(wallet);

        return transactionService.createNewTransaction(wallet.getOwner(),
                SMART_WALLET_LTD,
                walletId.toString(),
                amount,
                wallet.getBalance(),
                wallet.getCurrency(),
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCEEDED,
                transactionDescription,
                null);
    }

    public void createNewWallet(User user) {
        List<Wallet> allWallets = this.walletRepository.findAllWalletsByOwnerUsername(user.getUsername());
        Subscription activeSubscription = user.getSubscriptions().get(0);

        boolean isDefaultPlanAndMaxWalletsReached = activeSubscription.getType() == SubscriptionType.DEFAULT && allWallets.size() == 1;
        boolean isPremiumPlanAndMaxWalletsReached = activeSubscription.getType() == SubscriptionType.PREMIUM && allWallets.size() == 2;
        boolean isUltimatePlanAndMaxWalletsReached = activeSubscription.getType() == SubscriptionType.ULTIMATE && allWallets.size() == 3;

        if (isDefaultPlanAndMaxWalletsReached || isPremiumPlanAndMaxWalletsReached || isUltimatePlanAndMaxWalletsReached) {
            throw new DomainException("You reached the wallet creation limit.");
        }

        Wallet wallet = Wallet.builder()
                .owner(user)
                .status(WalletStatus.ACTIVE)
                .balance(new BigDecimal(0))
                .currency(Currency.getInstance("EUR"))
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        this.walletRepository.save(wallet);
    }

    public void createDefaultWallet(User user) {
        List<Wallet> wallets = this.walletRepository.findAllWalletsByOwnerUsername(user.getUsername());
        if (!wallets.isEmpty()) {
            throw new DomainException("User with id [%s] already has wallets. First wallet cannot be initialized.".formatted(user.getId()));
        }
            Wallet wallet = initializeWallet(user);

        this.walletRepository.save(wallet);
        log.info("Successfully created new wallet with id [%s] and balance [%.2f]."
                .formatted(wallet.getId(), wallet.getBalance()));

    }

    private Wallet initializeWallet(User user) {
        return Wallet.builder()
                .owner(user)
                .status(WalletStatus.ACTIVE)
                .balance(new BigDecimal("20.00"))
                .currency(Currency.getInstance("EUR"))
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
    }

    private Wallet getWalletById(UUID walletId) {
        return this.walletRepository.findById(walletId).orElseThrow(() ->
                new DomainException("Wallet with id [%s] does not exist.".formatted(walletId)));
    }

    public Map<UUID, List<Transaction>> getLastFourTransactions(List<Wallet> wallets) {
        Map<UUID, List<Transaction>> transactionsByWallet = new LinkedHashMap<>();

        for (Wallet wallet : wallets) {
            List<Transaction> lastTransactions = this.transactionService.getLastFourTransactionsByWallet(wallet);
            transactionsByWallet.put(wallet.getId(), lastTransactions);
        }

        return transactionsByWallet;
    }

    public void switchStatus(UUID walletId, UUID userId) {
        Optional<Wallet> optionalWallet = this.walletRepository.findByIdAndOwnerId(walletId, userId);
        if (optionalWallet.isEmpty()) {
            throw new DomainException("Wallet does not belong to user with id [%s].".formatted(userId));
        }

        Wallet wallet = optionalWallet.get();
        if (wallet.getStatus() == WalletStatus.ACTIVE) {
            wallet.setStatus(WalletStatus.INACTIVE);
        } else {
            wallet.setStatus(WalletStatus.ACTIVE);
        }

        this.walletRepository.save(wallet);
    }
}
