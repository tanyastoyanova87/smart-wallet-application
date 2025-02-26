package app.web;

import app.security.AuthenticationMetaData;
import app.transaction.model.Transaction;
import app.user.model.User;
import app.user.service.UserService;
import app.wallet.service.WalletService;
import app.web.dto.TransferRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/transfers")
public class TransferController {

    private final UserService userService;
    private final WalletService walletService;

    @Autowired
    public TransferController(UserService userService, WalletService walletService) {
        this.userService = userService;
        this.walletService = walletService;
    }

    @GetMapping
    public ModelAndView getTransfersPage(@AuthenticationPrincipal AuthenticationMetaData authenticationMetaData) {
        User user = this.userService.getById(authenticationMetaData.getId());

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("transfer");
        modelAndView.addObject("user", user);
        modelAndView.addObject("transferRequest", TransferRequest.builder().build());
        return modelAndView;
    }

    @PostMapping
    public ModelAndView initiateTransfer(@Valid TransferRequest transferRequest, BindingResult bindingResult, @AuthenticationPrincipal AuthenticationMetaData authenticationMetaData) {
        User user = this.userService.getById(authenticationMetaData.getId());

        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView();

            modelAndView.setViewName("transfer");
            modelAndView.addObject("user", user);
            modelAndView.addObject("transferRequest", TransferRequest.builder().build());

            return modelAndView;
        }

        Transaction transaction = this.walletService.transferFunds(user, transferRequest);

        return new ModelAndView("redirect:/transactions/" + transaction.getId());
    }
}
