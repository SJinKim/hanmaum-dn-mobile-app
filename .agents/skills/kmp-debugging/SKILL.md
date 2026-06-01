---
name: kmp-debugging
description: Use for bug reports, failing tests, crashes, regressions, network/auth problems, platform-specific Android/iOS behavior, or unexplained KMP app failures.
---

# KMP Debugging

Use this skill for root-cause debugging.

## Workflow

1. Collect evidence: logs, failing command, stack trace, reproduction path, recent diff.
2. Trace the path through UI → ViewModel → repository → network/storage/platform boundary.
3. Determine whether the issue is shared KMP, Android-specific, iOS-specific, backend contract, config, or data.
4. Add a regression test where feasible.
5. Fix the smallest root cause, not a cosmetic symptom.
6. Run the failing/relevant test first, then broader checks.
7. Document root cause and verification.

## Common Risk Areas

- Token attachment rules in `NetworkClient.kt`.
- BuildKonfig environment mismatch.
- ViewModel coroutine dispatching and test scheduler behavior.
- Nullable/changed backend DTO fields.
- Platform permission/lifecycle assumptions.
- Compose recomposition from unstable or expensive state.
