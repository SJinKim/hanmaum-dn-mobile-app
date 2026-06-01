# Onboard Command — Session Start

Use this prompt at the start of a Codex session:

1. Read `AGENTS.md`.
2. Read `tasks/lessons.md` and list any rules relevant to this task.
3. Run `git status --short --branch` and list dirty files.
4. Run `git log --oneline -10` to understand recent work.
5. If the task involves UI, read `designs/dn_app/DESIGN.md`.
6. Inspect related files before proposing a plan.
7. For 3+ steps or architectural decisions, create a plan before editing.

Output a short orientation summary and wait only if user changes or scope ambiguity would make proceeding risky.
