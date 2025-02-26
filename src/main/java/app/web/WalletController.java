package app.web;

import app.security.AuthenticationMetaData;
import app.transaction.model.Transaction;
import app.transaction.service.TransactionService;
import app.user.model.User;
import app.user.service.UserService;
import app.wallet.service.WalletService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/wallets")
public class WalletController {

    private final UserService userService;
    private final WalletService walletService;

    public WalletController(UserService userService, WalletService walletService) {
        this.userService = userService;
        this.walletService = walletService;
    }

    @GetMapping
    public ModelAndView getWalletsPage(@AuthenticationPrincipal AuthenticationMetaData authenticationMetaData) {
        User user = this.userService.getById(authenticationMetaData.getId());
        Map<UUID, List<Transaction>> lastFourTransactions = this.walletService.getLastFourTransactions(user.getWallets());

        ModelAndView modelAndView = new ModelAndView();

        modelAndView.setViewName("wallets");
        modelAndView.addObject("user", user);
        modelAndView.addObject("lastFourTransactions", lastFourTransactions);

        return modelAndView;
    }

    @PostMapping
    public String createNewWallet(@AuthenticationPrincipal AuthenticationMetaData authenticationMetaData) {
        User user = this.userService.getById(authenticationMetaData.getId());

        this.walletService.createNewWallet(user);
        return "redirect:/wallets";
    }

    @PutMapping("/{id}/status")
    public String switchWalletStatus(@PathVariable UUID id, @AuthenticationPrincipal AuthenticationMetaData authenticationMetaData) {
        this.walletService.switchStatus(id, authenticationMetaData.getId());

        return "redirect:/wallets";
    }

    @PutMapping("/{id}/top-up")
    public String topUp(@PathVariable UUID id) {
        Transaction transaction = this.walletService.topUp(id, new BigDecimal(20));

        return "redirect:/transactions/" + transaction.getId();
    }
}
