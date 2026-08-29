# Contributing — hanmaum D+N (mobile)

This repo is part of a three-repo project. Read `CLAUDE.md` and `designs/dn_app/DESIGN.md` before touching code, and respect the conventions below. They apply equally to human edits and Claude-assisted edits.

## Team size

Up to 3 developers. Every change goes through PR review — no direct pushes to `main`.

## Branching

- `main` — the only long-lived branch. Protected. Everything lands here by PR.
- `feature/<short-name>` — branched from `main`, PR back to `main`.
- `hotfix/<short-name>` — same shape; the name only signals urgency.

There is no `develop` any more. The branch used to decide which release channel
a build went to; the product flavour (`st` / `prod`) does that now, and every
distribution is a manual run of the "Distribute" workflow.

Before starting work:

```bash
git checkout main && git pull --ff-only
git checkout -b feature/<short-name>
```

Never commit directly to `main` — blocked by lefthook (`lefthook.yml`);
one-time setup: `brew install lefthook && lefthook install`.

## Pull requests

- One feature per PR. If it's too big to review, split it.
- Title uses the commit convention: `type(scope): summary`.
- Description: *why* the change is needed, what was done, how it was tested, screenshots / recordings for UI changes (both Android and iOS when reachable).
- Link the row in `../hanmaum-dn-ops/docs/MVP.md` the PR advances.
- Run `/pr-review` before opening the PR — paste the PASS/FAIL output in the description.
- Require at least one approval before merge. The author does not merge their own PR.
- Rebase on `main` before merging — no merge commits.

## Commit convention

`<type>(<scope>): <summary max 72 chars>`

Types: `feat | fix | refactor | test | chore | docs | perf | revert`

Imperative mood, no trailing period. Body explains *why*, not *what*.

## Design

- **`designs/dn_app/DESIGN.md` is the source of truth** for screens, components, and motion.
- Read it before building any screen.
- Deviations require explicit approval in PR review.

## Claude Code conventions

This project uses Claude Code. To keep all three developers' Claude behavior consistent:

### Public (committed) vs. private (gitignored)

| File | Committed? |
|---|---|
| `.claude/settings.json` | ✅ team defaults |
| `.claude/settings.local.json` | 🚫 per-machine |
| `.claude/commands/*.md` | ✅ shared workflows |
| `.claude/agents/*.md` | ✅ shared subagents |
| `.claude/skills/*/SKILL.md` | ✅ shared skills |
| `CLAUDE.md` | ✅ project AI instructions |
| `CLAUDE.local.md` | 🚫 personal notes |
| `AGENTS.md` | ✅ project Codex instructions |
| `.codex/**` / `.agents/**` | 🚫 per-machine Codex assets |
| `docs/codex/*.md` | ✅ shared Codex playbook + review checklist |
| `dev-log.md` / `dev-log-*.md` | 🚫 personal journaling |

Claude's assets (`.claude/`) are shared and reviewed; Codex's (`.codex/`, `.agents/`)
are local to each developer. Only the two entrypoints — `CLAUDE.md` and `AGENTS.md` —
plus `docs/codex/` are common ground, so anything the whole team must follow belongs
in one of those, never in a per-machine command file.

### Editing `CLAUDE.md` or `.claude/commands/`

These files change how everyone's Claude behaves. **PR review is required** — no solo edits that land directly on `main`. Treat them like code.

### Personal vs. team commands

If a slash command is only useful to you, put it in `~/.claude/commands/` (user-scope), not the repo's `.claude/commands/`. Keep the repo's command palette clean and team-relevant.

### Memory discipline

Cowork / Claude's per-user memory (`~/.claude/…`) is personal. Do not paste its contents into repo docs — it goes stale and confuses teammates. Facts the team needs to know belong in `CLAUDE.md` or a design doc.

## Session rules

- Run `/onboard` as the very first action in every Claude Code session.
- One feature per session — pick one, complete it on both Android and iOS where applicable, do not spread.
- If context/tokens run low: stop cleanly, commit what is done, wait for the next session. **Do not purchase extra tokens.**

## Communication

In PR descriptions, commit bodies, and design docs:

✅ Done / ⚠️ Found / 🔧 Fixed / 📋 Next / 🚫 Blocked

Be direct. File:line references. No filler.
