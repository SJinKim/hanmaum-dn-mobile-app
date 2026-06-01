# Code Review Checklist — Mobile KMP

Use this for `/review` or manual pre-PR review.

## Blocking Findings

- Shared `commonMain` code references Android/JVM/iOS-only APIs directly.
- New networking bypasses the injected shared Ktor client without justification.
- Backend bearer token can be attached to third-party hosts.
- Secret, API key, keystore, token, or real user data was committed.
- Navigation uses new string routes instead of type-safe route objects.
- UI ignores `designs/dn_app/DESIGN.md` tokens for colors, typography, shapes, or elevation.
- Behavior changed without tests or a clear verification explanation.
- Error/loading/empty states are missing for async user-facing flows.
- Long-running or blocking work can run on the UI/main thread.

## Strong Suggestions

- Move repeated UI into components when it appears 2+ times.
- Map DTOs to domain models in data layer.
- Prefer immutable UI state and unidirectional event flow.
- Use stable keys for lazy lists.
- Keep expensive calculations out of composable bodies.
- Prefer small commits/PRs with a clear feature slice.

## Final Review Output

Report:

1. PASS/FAIL.
2. Blocking issues with file/line references.
3. Non-blocking improvements.
4. Commands run and results.
5. Residual risk.
