package app.web;

import app.security.AuthenticationMetaData;
import app.subscription.model.SubscriptionType;
import app.subscription.service.SubscriptionService;
import app.transaction.model.Transaction;
import app.user.model.User;
import app.user.service.UserService;
import app.web.dto.UpgradeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/subscriptions")
public class SubscriptionsController {

    private final UserService userService;
    private final SubscriptionService subscriptionService;

    @Autowired
    public SubscriptionsController(UserService userService, SubscriptionService subscriptionService) {
        this.userService = userService;
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public ModelAndView getUpgradePage(@AuthenticationPrincipal AuthenticationMetaData authenticationMetaData) {
        User user = this.userService.getById(authenticationMetaData.getId());

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("upgrade");
        modelAndView.addObject("user", user);
        modelAndView.addObject("upgradeRequest", UpgradeRequest.builder().build());

        return modelAndView;
    }

    @PostMapping
    public String upgrade(@RequestParam("subscription-type") SubscriptionType subscriptionType, UpgradeRequest upgradeRequest, @AuthenticationPrincipal AuthenticationMetaData authenticationMetaData) {
        User user = this.userService.getById(authenticationMetaData.getId());

        Transaction upgrade = this.subscriptionService.upgrade(user, upgradeRequest, subscriptionType);

        return "redirect:/transactions/" + upgrade.getId();
    }

    @GetMapping("/history")
    public ModelAndView getSubscriptionsPage(@AuthenticationPrincipal AuthenticationMetaData authenticationMetaData) {
        User user = this.userService.getById(authenticationMetaData.getId());

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("subscription-history");
        modelAndView.addObject("user", user);

        return modelAndView;
    }
}
