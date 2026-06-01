# Feature Command — KMP Feature Slice

Use for adding or changing a feature:

1. Identify affected feature folder and platform targets.
2. Read existing domain/repository/presentation patterns in nearby features.
3. Plan the smallest shippable slice.
4. Add/update domain models and repository interfaces first.
5. Implement data layer with safe DTO mapping.
6. Implement ViewModel state/events.
7. Implement Compose UI with `designs/dn_app/DESIGN.md` tokens.
8. Register Koin bindings and routes only if needed.
9. Add/update tests.
10. Run relevant Gradle checks, then summarize results and residual risk.

Do not add production dependencies, migrations, or broad refactors without explicit approval.
