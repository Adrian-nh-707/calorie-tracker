package com.group19.calorietrack.account;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
public class AccountSettingsController {

    private final AccountSettingsService accountSettingsService;

    private final SecurityContextLogoutHandler logoutHandler =
            new SecurityContextLogoutHandler();

    public AccountSettingsController(
            AccountSettingsService accountSettingsService) {

        this.accountSettingsService = accountSettingsService;
    }

    @GetMapping("/settings")
    public String showSettings(
            Authentication authentication,
            Model model) {

        String email = authentication.getName();

        model.addAttribute(
                "profileForm",
                accountSettingsService.getProfile(email));

        model.addAttribute(
                "passwordForm",
                new PasswordChangeForm());

        addAccountInformation(model, email);

        return "account-settings";
    }

    @PostMapping("/settings")
    public String updateSettings(
            @Valid @ModelAttribute("profileForm")
            ProfileUpdateForm profileForm,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {

        String email = authentication.getName();

        if (bindingResult.hasErrors()) {
            model.addAttribute("passwordForm", new PasswordChangeForm());
            addAccountInformation(model, email);
            return "account-settings";
        }

        try {
            boolean emailChanged =
                    accountSettingsService.updateProfile(
                            email,
                            profileForm);

            if (emailChanged) {

                logoutHandler.logout(
                        request,
                        response,
                        authentication);

                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Your email was updated successfully. Please log in using your new email address.");

                return "redirect:/login?emailChanged";
            }

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Your profile was updated successfully.");

            return "redirect:/account/settings";

        } catch (IllegalArgumentException exception) {

            bindingResult.reject(
                    "profile.update",
                    exception.getMessage());

            model.addAttribute("passwordForm", new PasswordChangeForm());
            addAccountInformation(model, email);

            return "account-settings";
        }
    }

    @PostMapping("/settings/password")
    public String changePassword(
            @Valid @ModelAttribute("passwordForm")
            PasswordChangeForm passwordForm,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {

        String email = authentication.getName();

        if (bindingResult.hasErrors()) {

            model.addAttribute(
                    "profileForm",
                    accountSettingsService.getProfile(email));

            addAccountInformation(model, email);

            return "account-settings";
        }

        try {
            accountSettingsService.changePassword(
                    email,
                    passwordForm);

            logoutHandler.logout(
                    request,
                    response,
                    authentication);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Your password was changed successfully. Please log in using your new password.");

            return "redirect:/login?passwordChanged";

        } catch (IllegalArgumentException exception) {

            bindingResult.reject(
                    "password.change",
                    exception.getMessage());

            model.addAttribute(
                    "profileForm",
                    accountSettingsService.getProfile(email));

            addAccountInformation(model, email);

            return "account-settings";
        }
    }

    private void addAccountInformation(
            Model model,
            String email) {

        model.addAttribute(
                "accountInformation",
                accountSettingsService.getAccountInformation(email));
    }
}