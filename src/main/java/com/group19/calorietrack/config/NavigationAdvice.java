
package com.group19.calorietrack.config;

import com.group19.calorietrack.account.AccountSettingsController;
import com.group19.calorietrack.messaging.MessageController;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = {
        AccountSettingsController.class,
        MessageController.class
})
public class NavigationAdvice {

    @ModelAttribute("navigationRole")
    public String navigationRole(Authentication authentication) {

        if (authentication == null) {
            return "";
        }

        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(role -> role.equals("ROLE_USER")
                        || role.equals("ROLE_TRAINER")
                        || role.equals("ROLE_ADMIN"))
                .map(role -> role.substring(5))
                .findFirst()
                .orElse("");
    }
}
