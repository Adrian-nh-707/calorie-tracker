package com.group19.calorietrack.config;

import javax.sql.DataSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.group19.calorietrack.user.AppUser;
import com.group19.calorietrack.user.UserRepository;
import com.group19.calorietrack.user.UserRole;

@Component
@Profile("local-goals")
public class LocalGoalDemoData implements ApplicationRunner {
    private final DataSource dataSource;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public LocalGoalDemoData(DataSource dataSource, UserRepository users, PasswordEncoder encoder) {
        this.dataSource = dataSource;
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        // Defense in depth: do not seed demo credentials into any remote database.
        try (var connection = dataSource.getConnection()) {
            if (!connection.getMetaData().getURL().startsWith("jdbc:h2:file:")) {
                throw new IllegalStateException("local-goals requires an isolated H2 file database.");
            }
        }
        AppUser trainer = findOrCreate("trainer@local.example", "Demo", "Trainer", UserRole.TRAINER);
        AppUser user = findOrCreate("user@local.example", "Demo", "User", UserRole.USER);
        if (user.getAssignedTrainer() == null) {
            user.setAssignedTrainer(trainer);
            users.save(user);
        }
    }

    private AppUser findOrCreate(String email, String first, String last, UserRole role) {
        return users.findByEmail(email).orElseGet(() -> {
            AppUser user = new AppUser();
            user.setEmail(email);
            user.setFirstName(first);
            user.setLastName(last);
            user.setRole(role);
            user.setPassword(encoder.encode("LocalDemoOnly123!"));
            return users.save(user);
        });
    }
}
