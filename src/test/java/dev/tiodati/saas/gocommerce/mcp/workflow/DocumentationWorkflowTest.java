package dev.tiodati.saas.gocommerce.mcp.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests to verify the Specs-Driven Development (SDD) documentation workflow
 * and Git integration for the MCP service.
 * 
 * This test suite validates that:
 * 1. Core documentation files exist and are properly structured
 * 2. README.md references all required documentation
 * 3. Git workflow conventions are properly followed
 */
@DisplayName("Documentation Workflow Tests")
class DocumentationWorkflowTest {

    private final Path projectRoot = Paths.get("").toAbsolutePath();

    @Test
    @DisplayName("Core documentation files should exist")
    void shouldHaveCoreDocumentationFiles() {
        // Given: SDD workflow requires specific documentation files
        Path warpFile = projectRoot.resolve("WARP.md");
        Path planFile = projectRoot.resolve("PLAN.md");
        Path tasksFile = projectRoot.resolve("TASKS.md");
        Path readmeFile = projectRoot.resolve("README.md");

        // Then: All core documentation files should exist
        assertTrue(Files.exists(warpFile), "WARP.md should exist");
        assertTrue(Files.exists(planFile), "PLAN.md should exist");
        assertTrue(Files.exists(tasksFile), "TASKS.md should exist");
        assertTrue(Files.exists(readmeFile), "README.md should exist");

        // And: Files should be readable
        assertTrue(Files.isReadable(warpFile), "WARP.md should be readable");
        assertTrue(Files.isReadable(planFile), "PLAN.md should be readable");
        assertTrue(Files.isReadable(tasksFile), "TASKS.md should be readable");
        assertTrue(Files.isReadable(readmeFile), "README.md should be readable");
    }

    @Test
    @DisplayName("PLAN.md should contain required sections")
    void planShouldContainRequiredSections() throws IOException {
        // Given: PLAN.md file exists
        Path planFile = projectRoot.resolve("PLAN.md");
        assumeTrue(Files.exists(planFile), "PLAN.md must exist for this test");

        // When: Reading PLAN.md content
        String content = Files.readString(planFile);

        // Then: Should contain all required sections from SDD methodology
        assertAll("PLAN.md sections",
            () -> assertTrue(content.contains("Executive Summary"), 
                "Should contain Executive Summary section"),
            () -> assertTrue(content.contains("System Architecture Overview"), 
                "Should contain System Architecture Overview section"),
            () -> assertTrue(content.contains("Data Models and Entities"), 
                "Should contain Data Models and Entities section"),
            () -> assertTrue(content.contains("API Specifications"), 
                "Should contain API Specifications section"),
            () -> assertTrue(content.contains("Implementation Phases"), 
                "Should contain Implementation Phases section"),
            () -> assertTrue(content.contains("Multi-Tenancy"), 
                "Should contain Multi-Tenancy information"),
            () -> assertTrue(content.contains("Quarkus"), 
                "Should reference Quarkus framework"),
            () -> assertTrue(content.contains("PostgreSQL"), 
                "Should reference PostgreSQL database"),
            () -> assertTrue(content.contains("Keycloak"), 
                "Should reference Keycloak authentication")
        );
    }

    @Test
    @DisplayName("TASKS.md should contain task checklist format")
    void tasksShouldContainChecklistFormat() throws IOException {
        // Given: TASKS.md file exists
        Path tasksFile = projectRoot.resolve("TASKS.md");
        assumeTrue(Files.exists(tasksFile), "TASKS.md must exist for this test");

        // When: Reading TASKS.md content
        String content = Files.readString(tasksFile);

        // Then: Should be formatted as markdown checklist
        assertAll("TASKS.md format",
            () -> assertTrue(content.contains("- [ ]"), 
                "Should contain unchecked checklist items"),
            () -> assertTrue(content.contains("Git Sync and Housekeeping"), 
                "Should contain Git workflow tasks"),
            () -> assertTrue(content.contains("branch:"), 
                "Should suggest branch names for tasks"),
            () -> assertTrue(content.contains("commit:"), 
                "Should suggest commit messages for tasks"),
            () -> assertTrue(content.contains("feat:"), 
                "Should use conventional commit prefixes"),
            () -> assertTrue(content.contains("chore:"), 
                "Should include housekeeping tasks"),
            () -> assertTrue(content.contains("test:"), 
                "Should include testing tasks")
        );
    }

    @Test
    @DisplayName("README.md should reference core documentation")
    void readmeShouldReferenceCoreDocumentation() throws IOException {
        // Given: README.md file exists
        Path readmeFile = projectRoot.resolve("README.md");
        assumeTrue(Files.exists(readmeFile), "README.md must exist for this test");

        // When: Reading README.md content
        String content = Files.readString(readmeFile);

        // Then: Should reference all core documentation files
        assertAll("README.md documentation references",
            () -> assertTrue(content.contains("WARP.md"), 
                "Should reference WARP.md"),
            () -> assertTrue(content.contains("PLAN.md"), 
                "Should reference PLAN.md"),
            () -> assertTrue(content.contains("TASKS.md"), 
                "Should reference TASKS.md"),
            () -> assertTrue(content.contains("Specs-Driven Development"), 
                "Should mention SDD methodology"),
            () -> assertTrue(content.contains("Core Documentation"), 
                "Should have Core Documentation section"),
            () -> assertTrue(content.contains("Development Approach"), 
                "Should explain development approach")
        );
    }

    @Test
    @DisplayName("Git workflow should follow conventional commits")
    void shouldFollowConventionalCommitFormat(@TempDir Path tempDir) throws IOException {
        // Given: Example commit messages from TASKS.md
        String[] expectedCommitPrefixes = {
            "feat:", "fix:", "docs:", "style:", "refactor:", 
            "test:", "chore:", "perf:", "ci:", "build:"
        };

        Path tasksFile = projectRoot.resolve("TASKS.md");
        assumeTrue(Files.exists(tasksFile), "TASKS.md must exist for this test");
        
        String tasksContent = Files.readString(tasksFile);

        // Then: Should contain conventional commit examples
        boolean hasConventionalCommits = false;
        for (String prefix : expectedCommitPrefixes) {
            if (tasksContent.contains("commit: \"" + prefix)) {
                hasConventionalCommits = true;
                break;
            }
        }
        
        assertTrue(hasConventionalCommits, 
            "TASKS.md should contain conventional commit examples");
    }

    @Test
    @DisplayName("Project structure should support multi-phase development")
    void shouldSupportMultiPhaseDevelopment() throws IOException {
        // Given: PLAN.md contains implementation phases
        Path planFile = projectRoot.resolve("PLAN.md");
        assumeTrue(Files.exists(planFile), "PLAN.md must exist for this test");

        String planContent = Files.readString(planFile);

        // Then: Should describe multiple implementation phases
        assertAll("Multi-phase development",
            () -> assertTrue(planContent.contains("Phase 1") || planContent.contains("Phase 1:"), 
                "Should contain Phase 1"),
            () -> assertTrue(planContent.contains("Phase 2") || planContent.contains("Phase 2:"), 
                "Should contain Phase 2"),
            () -> assertTrue(planContent.contains("Phase 3") || planContent.contains("Phase 3:"), 
                "Should contain Phase 3"),
            () -> assertTrue(planContent.contains("Implementation Phase") || 
                           planContent.contains("Core Infrastructure"), 
                "Should describe implementation phases")
        );
    }

    /**
     * Helper method to skip tests when prerequisites don't exist
     */
    private void assumeTrue(boolean condition, String message) {
        if (!condition) {
            org.junit.jupiter.api.Assumptions.assumeTrue(condition, message);
        }
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
