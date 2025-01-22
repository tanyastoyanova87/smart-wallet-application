package app.user.web;

import app.user.model.User;
import app.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
public class IndexController {

    private final UserService userService;

    @Autowired
    public IndexController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String getIndexPage() {
        return "index";
    }

    @GetMapping("/login")
    public String getLoginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String getRegister() {
        return "register";
    }

    @GetMapping("/home")
    public ModelAndView getHomePage() {
        ModelAndView modelAndView = new ModelAndView();

        User user = this.userService.getById(UUID.fromString("53486350-e1f3-4a93-94d7-43bf4135be41"));
        modelAndView.setViewName("home");
        modelAndView.addObject("user", user);

        return modelAndView;
    }
}
