package com.group19.calorietrack.user;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/register")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "register";
    }

    @PostMapping
    public String registerUser(
            @Valid @ModelAttribute("registrationForm")
            RegistrationForm registrationForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            userService.registerUser(
                    registrationForm.getFirstName(),
                    registrationForm.getLastName(),
                    registrationForm.getEmail(),
                    registrationForm.getPassword()
            );
        } catch (IllegalArgumentException exception) {
            bindingResult.rejectValue(
                    "email",
                    "email.exists",
                    exception.getMessage()
            );
            return "register";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Your account was successfully created. You can now login to your account."
        );

        return "redirect:/login";
    }
}
