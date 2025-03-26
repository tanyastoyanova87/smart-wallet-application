package app.transaction.service;

import app.exception.DomainException;
import app.notification.service.NotificationService;
import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.transaction.model.TransactionType;
import app.transaction.repository.TransactionRepository;
import app.user.model.User;
import app.wallet.model.Wallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository, NotificationService notificationService) {
        this.transactionRepository = transactionRepository;
        this.notificationService = notificationService;
    }

    public Transaction createNewTransaction(User owner, String sender, String receiver, BigDecimal amount, BigDecimal balanceLeft, Currency currency, TransactionType transactionType, TransactionStatus transactionStatus, String transactionDescription, String failureReason) {
        Transaction transaction = Transaction.builder()
                .owner(owner)
                .sender(sender)
                .receiver(receiver)
                .amount(amount)
                .balanceLeft(balanceLeft)
                .currency(currency)
                .type(transactionType)
                .status(transactionStatus)
                .description(transactionDescription)
                .failureReason(failureReason)
                .createdOn(LocalDateTime.now())
                .build();

        String emailBody = "%s transaction was successfully processed for you with amount %.2f EUR!".formatted(transaction.getType(), transaction.getAmount());
        notificationService.sendNotification(transaction.getOwner().getId(), "Money Transfer", emailBody);

        this.transactionRepository.save(transaction);
        return transaction;
    }

    public List<Transaction> getAllByOwnerId(UUID ownerId) {
        return this.transactionRepository.findAllByOwnerIdOrderByCreatedOnDesc(ownerId);
    }

    public Transaction getById(UUID id) {
        return this.transactionRepository.findById(id).orElseThrow(() -> new DomainException(
                "Transaction with [%s] does not exist.".formatted(id)));
    }

    public List<Transaction> getLastFourTransactionsByWallet(Wallet wallet) {
        return this.transactionRepository
                .findAllBySenderOrReceiverOrderByCreatedOnDesc(wallet.getId().toString(), wallet.getId().toString())
                .stream()
                .filter(t -> t.getOwner().getId() == wallet.getOwner().getId())
                .filter(t -> t.getStatus() == TransactionStatus.SUCCEEDED)
                .limit(4)
                .toList();
    }
}
