---
name: kmp-mobile-senior-engineer
description: Use when implementing or modifying Kotlin Multiplatform mobile features for Android and iOS in this repo. Applies senior engineering workflow, KMP source-set boundaries, architecture rules, test strategy, and platform verification.
---

# KMP Mobile Senior Engineer

Use this skill for feature work, refactors, and non-trivial changes in `composeApp`.

## Workflow

1. Read `AGENTS.md`, `tasks/lessons.md`, and relevant feature files.
2. Identify source sets touched: `commonMain`, `androidMain`, `iosMain`, tests.
3. Keep shared code platform-neutral. Put platform APIs behind interfaces or platform source sets.
4. Preserve feature-first clean architecture:
   - domain models and repository interfaces in `domain/`
   - DTOs and implementations in `data/`
   - ViewModels/UI state/screens in `presentation/`
5. Keep UI state immutable and event-driven.
6. Add/update tests before completion.
7. Run the smallest relevant Gradle checks, then broader checks when feasible.

## Output

Final response must include changed files, verification commands/results, and residual risk.
