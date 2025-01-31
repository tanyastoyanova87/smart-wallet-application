package app.web;

import app.user.model.User;
import app.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
@RequestMapping("/subscriptions")
public class SubscriptionsController {

    private final UserService userService;

    @Autowired
    public SubscriptionsController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String getUpgradePage() {
        return "upgrade";
    }

    @GetMapping("/history")
    public ModelAndView getSubscriptionsPage() {
        User user = this.userService.getById(UUID.fromString("d3cf156a-b59e-4ace-9f47-bf694a61dcd7"));

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("subscription-history");
        modelAndView.addObject("user", user);

        return modelAndView;
    }
}
