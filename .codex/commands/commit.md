# Commit Command — Commit Prep

Commit message format:

```text
<type>(<scope>): <imperative summary max 72 chars>
```

Types: `feat`, `fix`, `refactor`, `test`, `chore`, `docs`, `perf`, `revert`.

Before committing:

1. Run relevant tests/checks.
2. Confirm `git status --short` contains only intended files.
3. Inspect `git diff --cached` after staging.
4. Ensure no secrets, hardcoded env URLs, commented-out code, or unrelated edits.
5. Never add `Co-Authored-By:` trailers.
