package com.arjun.crm.config;

import com.arjun.crm.entity.User;
import com.arjun.crm.entity.Workspace;
import com.arjun.crm.entity.WorkspaceMember;
import com.arjun.crm.enums.Role;
import com.arjun.crm.enums.UserStatus;
import com.arjun.crm.enums.WorkspaceRole;
import com.arjun.crm.repository.UserRepository;
import com.arjun.crm.repository.WorkspaceRepository;
import com.arjun.crm.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;

/**
 * Initializes test data when the application starts.
 * This allows frontend to test login, workspaces, and other features
 * without having to manually register users first.
 *
 * Controlled by app.seed.enabled (DEMO_SEED_ENABLED). The seeded password comes
 * from app.seed.password (DEMO_SEED_PASSWORD); seeding is skipped when it is blank.
 * Seeded users never get the global ADMIN role, since their credentials may be public.
 */
@Configuration
@Slf4j
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataLoader {

    @Value("${app.seed.password:}")
    private String seedPassword;

    @Bean
    public CommandLineRunner loadData(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            log.info("═══════════════════════════════════════════════════════════════");
            log.info("  INITIALIZING TEST DATA");
            log.info("═══════════════════════════════════════════════════════════════");

            // Check if test users already exist
            if (userRepository.count() > 0) {
                log.info("Test data already exists, skipping initialization");
                return;
            }

            if (seedPassword == null || seedPassword.isBlank()) {
                log.warn("app.seed.password (DEMO_SEED_PASSWORD) is not set, skipping test data");
                return;
            }

            try {
                String encodedPassword = passwordEncoder.encode(seedPassword);

                // Create test users
                User testUser1 = User.builder()
                        .fullName("Test User One")
                        .email("test1@example.com")
                        .password(encodedPassword)
                        .role(Role.USER)
                        .status(UserStatus.ACTIVE)
                        .build();

                User testUser2 = User.builder()
                        .fullName("Test User Two")
                        .email("test2@example.com")
                        .password(encodedPassword)
                        .role(Role.USER)
                        .status(UserStatus.ACTIVE)
                        .build();

                User adminUser = User.builder()
                        .fullName("Admin User")
                        .email("admin@example.com")
                        .password(encodedPassword)
                        .role(Role.USER)
                        .status(UserStatus.ACTIVE)
                        .build();

                testUser1 = userRepository.save(testUser1);
                testUser2 = userRepository.save(testUser2);
                adminUser = userRepository.save(adminUser);

                log.info("✓ Created test users: test1@example.com, test2@example.com, admin@example.com");

                // Create test workspaces
                Workspace workspace1 = Workspace.builder()
                        .name("Marketing Team")
                        .description("Marketing department workspace")
                        .owner(testUser1)
                        .build();

                Workspace workspace2 = Workspace.builder()
                        .name("Development Team")
                        .description("Development department workspace")
                        .owner(testUser2)
                        .build();

                workspace1 = workspaceRepository.save(workspace1);
                workspace2 = workspaceRepository.save(workspace2);

                log.info("✓ Created test workspaces:");
                log.info("  - Marketing Team (ID: {})", workspace1.getId());
                log.info("  - Development Team (ID: {})", workspace2.getId());

                // Add members to workspaces
                WorkspaceMember member1 = WorkspaceMember.builder()
                        .workspace(workspace1)
                        .user(testUser1)
                        .role(WorkspaceRole.OWNER)
                        .build();

                WorkspaceMember member2 = WorkspaceMember.builder()
                        .workspace(workspace1)
                        .user(testUser2)
                        .role(WorkspaceRole.MEMBER)
                        .build();

                WorkspaceMember member3 = WorkspaceMember.builder()
                        .workspace(workspace2)
                        .user(testUser2)
                        .role(WorkspaceRole.OWNER)
                        .build();

                WorkspaceMember member4 = WorkspaceMember.builder()
                        .workspace(workspace2)
                        .user(adminUser)
                        .role(WorkspaceRole.ADMIN)
                        .build();

                workspaceMemberRepository.save(member1);
                workspaceMemberRepository.save(member2);
                workspaceMemberRepository.save(member3);
                workspaceMemberRepository.save(member4);

                log.info("✓ Added workspace members");

                log.info("═══════════════════════════════════════════════════════════════");
                log.info("  ✓ TEST DATA INITIALIZATION COMPLETE");
                log.info("═══════════════════════════════════════════════════════════════");
                log.info("  You can now login with test1@example.com and DEMO_SEED_PASSWORD");
                log.info("═══════════════════════════════════════════════════════════════");

            } catch (Exception e) {
                log.error("Failed to initialize test data", e);
            }
        };
    }
}
