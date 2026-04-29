# /onboard — Session Start for hanmaum-dn-mobile-app

Run this at the beginning of every session.

## Steps

1. Read `CLAUDE.md` and internalize all architecture rules and workflow orchestration.
2. Read `tasks/lessons.md` — these are mistakes from previous sessions. Do not repeat them.
3. Check `git status` and `git branch` — know where we are.
4. Check `git log --oneline -5` — know what was done recently.
5. Report a brief status using this format:

```
✅ Branch: <current branch>
✅ Lessons loaded: <count> entries
📋 Recent commits: <last 2-3 summaries>
📋 Ready to work
```

## Reminders

- Plan before coding on any task with 3+ steps or architectural decisions.
- After ANY correction from the user, append to `tasks/lessons.md` before continuing.
- Never mark a task done without running `./gradlew :composeApp:allTests`.
- KMP-specific: check both Android and iOS targets are considered when changing shared code.
