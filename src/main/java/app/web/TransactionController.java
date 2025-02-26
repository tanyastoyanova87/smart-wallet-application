package app.web;

import app.security.AuthenticationMetaData;
import app.transaction.model.Transaction;
import app.transaction.service.TransactionService;
import app.user.model.User;
import app.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserService userService;

    @Autowired
    public TransactionController(TransactionService transactionService, UserService userService) {
        this.transactionService = transactionService;
        this.userService = userService;
    }

    @GetMapping
    public ModelAndView getTransactionsPage(@AuthenticationPrincipal AuthenticationMetaData authenticationMetaData) {
        ModelAndView modelAndView = new ModelAndView();
        User user = this.userService.getById(authenticationMetaData.getId());

        List<Transaction> transactions = this.transactionService.getAllByOwnerId(authenticationMetaData.getId());
        modelAndView.setViewName("transactions");
        modelAndView.addObject("transactions", transactions);
        modelAndView.addObject("user", user);

        return modelAndView;
    }

    @GetMapping("/{id}")
    public ModelAndView getTransactionById(@PathVariable UUID id, @AuthenticationPrincipal AuthenticationMetaData authenticationMetaData) {
        Transaction transaction = this.transactionService.getById(id);
        User user = this.userService.getById(authenticationMetaData.getId());

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("transaction-result");
        modelAndView.addObject("transaction", transaction);
        modelAndView.addObject("user", user);


        return modelAndView;
    }
}
