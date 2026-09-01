# /onboard — Session Start for hanmaum-dn-mobile-app

You are a principal-level fullstack engineer. You own every commit.
Never shortcut without flagging debt explicitly.

**The board is the plan.** Work is tracked in GitHub issues on the
**DN-Mobile-App** board (project `6`, owner `SJinKim`) — nowhere else. Do not
create local task files, checklists or TODO lists to track work. One card at a
time, pulled from the board, moved across it, closed by its PR.

The exceptions are deliberate and few: `tasks/lessons.md` (mistakes, not
tasks) and `docs/superpowers/specs|plans/` (design thinking too big for an
issue body — and each one still has an issue).

---

## A. Orient (fast — one screen of output, no file dumps)

```bash
git status --short --branch && git log --oneline -5
```

- Dirty tree with work you didn't make → **list the files and ask** before
  touching anything.
- Not on `main` with a stale branch → note it; branch decisions come in §C.

Then read, in this order, and only what you haven't read this session:

1. `CLAUDE.md` — §3 architecture invariants, §6 the named mistakes, §8 when to
   ask. It is the operating manual; this file does not restate it.
2. `tasks/lessons.md` — every entry cost hours once. Fast path:
   `grep -n "^#\{2,3\} " tasks/lessons.md` for the headings, then read in full
   the entries that touch tonight's area.
3. `designs/dn_app/DESIGN.md` — only before UI work. It is a short pointer
   file; Figma is the design.

**Do not read a doc to learn a number or a gate.** Baselines drift; the tool
is authoritative. `verifying-kmp-changes` owns the verification ladder and the
flavored task names — everything else that quotes them is a stale copy.

## B. Read the board

`/issues` owns the queries and the project field IDs — use it rather than
re-deriving them, and never dump raw `--format json` (it costs ~11k tokens
unfiltered).

Read the board and reconcile it with reality before choosing anything:

- A card in **In Review** whose PR is merged → it is Done.
- A card in **Done** whose issue is still OPEN → close it.
- A **Todo** card blocked on `hanmaum-dn-server` → check whether that server
  issue has since closed. Several here were unblocked without anyone noticing;
  a `TODO(hanmaum-dn-server#N)` in the code is a claim about the past, not the
  present.

Fix the board as you find it. A board nobody trusts is worse than no board.

## C. Pull exactly one card

State which card you propose and why it is next, then wait for a nod. Rank by:
blocking others > user-visible bug > unblocked feature > chore. Prefer a card
whose fix teaches the codebase something the next three cards need.

Once agreed:

```bash
git checkout main && git pull --ff-only && git checkout -b <type>/<short-name>
```

Move the card to **In Progress** (`/issues`). Never work on `main` — lefthook
blocks the commit anyway.

If the task is not on the board, it is not tracked. File the issue first,
even for a one-line fix; that issue body is where the "why" survives.

## D. Work it

- 3+ steps or an architectural decision → plan first. Spec/plan under
  `docs/superpowers/`, linked from the issue.
- Read the related code **before** planning. `graphify explain "<Symbol>"` and
  `graphify affected "<Symbol>"` beat grepping (CLAUDE.md §9).
- Shared code → Android **and** iOS both considered, every time.
- Scope is the card. Something else broken on the way → new issue, not a
  bigger diff.

## E. Close it out

Run the ladder from `verifying-kmp-changes` — it decides which gates this
change actually needs — and show the output. Then `/commit` and open the PR.

The PR body **must** carry `Closes #<n>` — English keyword, always. It is what
creates the issue↔PR link that fills the board's *Linked pull requests*
column; a German „Behebt #n" links nothing and closes nothing. Verify the link
landed (`/issues` has the query), then move the card to **In Review**.

Merge on green, never on hope: `--auto` does not wait here.

```bash
gh pr checks <n> --watch --fail-fast && gh pr merge <n> --squash --delete-branch
```

Then confirm the issue actually closed and the card actually moved to **Done**
— GitHub does not always pull it across.

## F. Stop conditions

Stop and re-plan rather than pushing through when: the same symptom survives
two distinct fixes · CI is red while local is green · reality contradicts the
task description · context is running low (then: commit clean work, summarize,
stop). Details and the escalation list: `CLAUDE.md` §8.

After any user correction, append Mistake → Rule to `tasks/lessons.md` in the
same session.
