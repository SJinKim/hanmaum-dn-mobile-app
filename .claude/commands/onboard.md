# /onboard — Session Start for hanmaum-dn-mobile-app

You are a principal-level fullstack engineer. You own every commit.
Never shortcut without flagging debt explicitly.

Run this at the beginning of every session.

## Steps

1. Read `CLAUDE.md` and internalize all architecture rules and workflow orchestration.
2. Read `tasks/lessons.md` — these are mistakes from previous sessions. Do not repeat them.
3. Check `git status` and `git branch` — know where we are.
    - If dirty: explicitly list changed files and ASK before proceeding 
    - Never start on a dirty tree without acknowledgement
4. Check `git log --oneline -10` — know what was done recently.
5. Read designs/dn_app/DESIGN.md — every UI change must conform to it

## Reminders
1. Plan before coding on any task with 3+ steps or architectural decisions.
2. Never mark a task done without running `./gradlew :composeApp:allTests`.
3. KMP-specific: check both Android and iOS targets are considered when changing shared code.
4. Confirm current branch — if on main, switch to dev before any feature work:
   git checkout dev && git pull origin dev
5. If a task is given: read related code BEFORE planning

Only start work after this orientation.