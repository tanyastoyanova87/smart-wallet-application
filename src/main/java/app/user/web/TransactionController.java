package app.user.web;

import app.transaction.model.Transaction;
import app.transaction.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ModelAndView getTransactionsPage() {
        ModelAndView modelAndView = new ModelAndView();

        List<Transaction> transactions = this.transactionService.getAllByOwnerId(UUID.fromString("53486350-e1f3-4a93-94d7-43bf4135be41"));
        modelAndView.setViewName("transactions");
        modelAndView.addObject("transactions", transactions);

        return modelAndView;
    }
}
