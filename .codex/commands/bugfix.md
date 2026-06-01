# Bugfix Command — Root Cause First

Use for bug reports:

1. Reproduce or locate evidence: failing test, stack trace, log, code path, or user flow.
2. Trace from UI/event to ViewModel to repository/network/platform boundary.
3. Identify the root cause and smallest safe fix.
4. Add a regression test when feasible.
5. Implement the fix without unrelated cleanup.
6. Run the failing/relevant test first, then broader checks as needed.
7. Summarize root cause, changed files, verification, and remaining risk.

Never patch symptoms while leaving auth leakage, race conditions, state bugs, or platform-specific crashes unresolved.
