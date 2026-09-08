# 말씀 기록 — daily QT and recitation streaks

Status: design approved 2026-09-08. Blocked on the verse endpoints
(SJinKim/hanmaum-dn-server#115) before any of it can render real data.

## Problem

Home carries two verse cards. Both were drawn with a `Streak` frame — seven
pills and a `4/7일` counter — and neither has ever had a source. The frame name
is the giveaway: it is a *record of what the member did*, not elapsed time. Four
filled pills on a Thursday says something a calendar cannot.

Two things were conflated until now and are separated here:

- **Counting days is free.** Elapsed days in a week is date arithmetic. No
  endpoint needed. But a bar that says "it is day 4 of the week" tells the
  member what they already know, and the seven pills carry no information.
- **A streak is state.** "I recited on 4 of the 7 days" is per-member,
  per-day, and has to be stored somewhere durable.

The card also has no way to *set* anything today. The interaction the design
implies was never built.

## Decisions taken

| Question | Decision |
|---|---|
| One counter or two? | **Two independent streaks.** QT (read today's passage) on 오늘의 말씀, recitation on 주간 암송. |
| How is a day marked? | **Tap today's pill.** No button; the pills are the interaction. |
| Past days, mis-taps? | **Today only, no undo.** Missed days stay empty for ever. |
| Profile surfacing | **Third `StatTile` row**: `QT 기록` and `암송 기록`, both all-time day counts. |
| Where does the record live? | **Server.** See "Approaches rejected". |
| Sunday on the QT streak | Seven pills, Sunday drawn as a third state and excluded from the denominator → `3/6일`. |

## The Sunday problem

Measured, not assumed: `quiet-time.php` returns `found:false` on Sundays
(checked 2026-09-06, -13, -20, -27). The 오늘의 말씀 card therefore **hides
itself every Sunday**, and a QT streak can never reach 7/7 — on Sunday there is
not even a pill to tap.

Resolution: both bars keep seven pills so the two cards look alike, but on the
QT bar the Sunday pill renders in a third state — neither empty nor filled —
and drops out of the denominator. The counter reads `3/6일`.

Rejected: six pills (Mon–Sat) would make the two bars different lengths on one
screen; seven equal pills would show a day that is never fillable.

## Approaches rejected

**Client-local only** (`multiplatform-settings`, as `AttendancePreferences`
does). Shippable without server work and offline-capable, but the all-time
count silently returns to zero on reinstall or a new device. A number that
resets is worse than no number, and a local streak is trivially forged.

**Local-first with sync.** Durable and offline-capable, at the cost of conflict
resolution and two sources of truth — for seven pills. YAGNI.

## Design

### Server contract

```jsonc
GET /api/v1/verses/records
{"quietTime": {"weekStart":"2026-09-06","days":["2026-09-07","2026-09-08"],
                "todayMarked":true,"todayMarkable":true,"totalDays":84},
 "recitation":{"weekStart":"2026-09-06","days":["2026-09-07"],
                "todayMarked":false,"todayMarkable":true,"totalDays":127}}

POST /api/v1/verses/records   {"kind":"QUIET_TIME"|"RECITATION"}
  201 → the updated block for that kind
  409 → already marked today; the client treats this as success, not an error
```

Three properties of this contract are load-bearing:

- **No `date` in the request body.** The server stamps the day. Otherwise
  "today only" is a client-side rule that a clock change defeats.
- **No `DELETE`.** "No undo" is enforced by the absence of the operation, not
  by the client declining to call it.
- **`todayMarkable`** lets the server say "today cannot be marked" without the
  client re-deriving the reason. For `quietTime` it is false on Sundays and on
  any day the plan has no entry; for `recitation` it is false when no weekly
  verse is set.

`weekStart` for `recitation` is the weekly verse's own week (already in the
#115 contract). For `quietTime` it is the calendar week, Sunday-start, matching
주일 as the start of the church week.

`days` carries **only the current week's** marked dates — the seven pills are
all the client draws. The all-time figure travels separately as `totalDays`, so
a member with years of history never ships years of dates to render a bar.

Storage: insert-only rows of `(member_id, date, kind)` with a unique constraint
on all three. `totalDays` is a count over that member and kind.

### Mobile

No new slice — `features/verse/` grows:

```
features/verse/
  domain/model/VerseStreak.kt          VerseStreak(kind, weekStart, days, todayMarked,
                                                    todayMarkable, totalDays)
  domain/model/VerseRecordKind.kt      QUIET_TIME | RECITATION
  domain/repository/VerseRecordRepository.kt
        suspend fun getRecords(): Result<VerseRecords>
        suspend fun mark(kind: VerseRecordKind): Result<VerseStreak>
  data/model/VerseRecordDtos.kt
  data/repository/VerseRecordRepositoryImpl.kt
  presentation/components/StreakBar.kt
```

`StreakBar` renders seven pills from `weekStart`. Each pill stays visually
24×6 dp but sits inside a **44 dp tall transparent hit area** — the visual
design is unchanged while the touch target meets the guideline. Today's pill is
the only one that is clickable; every other pill is inert.

Pill states, settled against the rendered design rather than in the abstract:

| State | Treatment |
|---|---|
| Marked | fill `accent/amber` |
| Today, markable | fill `accent/amber-dim` **plus a 1 dp `accent/amber` stroke** |
| Empty (future or missed) | fill `bg/surface-3` |
| Not markable (Sunday, QT bar) | fill `bg/surface-2` |

The stroke is not decoration. `accent/amber-dim` alone renders *darker* than
`bg/surface-3` in dark mode, so today's pill read as less prominent than an
empty one — the hierarchy inverted. The stroke restores it, and at 6 dp height
a 1 dp stroke is legible at 1:1 (verified on the rendered frame).

Tapping fills the pill immediately and fires the POST behind it. On failure the
fill reverts and the card shows a short message. Optimistic, because with no
undo the member must see instantly that the tap landed.

`HomeViewModel` holds both streaks and exposes `markQuietTime()` /
`markRecitation()`. `ProfileScreen` gains a third `StatTile` row fed by the
same repository. The value is formatted by
`verseRecordDaysValue(days: Int)` per language, mirroring the existing
`profileTimeTogetherValue(years, months)` pattern.

**Days only, no year/month split.** A half-width tile is ~147 dp of usable
width and `stat` is 28 sp; `12345일` needs ~100 dp. Ten thousand days is
twenty-seven years, so the days form never runs out of room. The split would be
work without an occasion.

Labels: `QT 기록` / `암송 기록` in Korean; "Quiet time" / "Recitation" in
English; "Stille Zeit" / "Auswendiglernen" in German. `QT` is idiomatic in Korean
church usage but reads as an unexplained abbreviation in the other two.

### Card layout changes

**오늘의 말씀** — `개역개정` is removed, `읽기 ↗` moves to the top right beside
the eyebrow, and the freed bottom row carries the streak. The `읽기` link keeps
its single job of opening the reading page; it does **not** mark the day.

**주간 암송 구절** — unchanged except that today's pill takes the outlined,
tappable state.

**Profile** — a new `StatTile` row below the existing ones. The `소속 사역`
placeholder keeps its slot; #160 is untouched. Both values bind to
`accent/amber`: the colour roles are load-bearing in this design system, lime
is action/attendance and blue is information, and a 말씀 record is neither.

One divergence found while building: Figma's profile carries **one row of three**
tiles at 111 dp, while the shipped code renders **2×2 with four** (`소속 그룹`
exists only in code). The new row keeps Figma's 111 dp column rhythm, so the
grid reads as a grid with one empty cell. Aligning the two layouts is a separate
piece of work and is deliberately not bundled here.

Six Figma frames: Home Dark/Light, Home · unten Dark/Light, Profile Dark/Light.

### Testing

- `StreakBar`: today's pill is the only clickable one; a non-markable day is
  inert; the hit area is 44 dp.
- `VerseRecordRepositoryImpl` with `MockEngine`: 201 maps to the new block, 409
  maps to success, 5xx to failure.
- `HomeViewModel`: optimistic fill, revert on failure, an already-marked day
  leaves the pill inert.
- `ProfileScreen` totals: value renders, dash while unloaded or failed.
- Day-count formatting in all three languages.

## Sequencing

1. Weekly-verse selection on the server — the existing blocker; without it
   주간 암송 has no verse to recite.
2. `/api/v1/verses/records` (GET + POST).
3. Figma: the six frames.
4. Mobile: the slice, the cards, the profile row.

Steps 3 and 4 can start before 1 and 2 land; the cards simply stay hidden.

## Out of scope

- Reminders or notifications for an unmarked day.
- Any streak beyond the current week (longest streak, calendar heat map).
- A pastoral or admin view of who recited what.
- Retroactively marking a missed day, in any form.
- The 성경 읽기 plan (`daily-reading.php`); only the QT passage is tracked.

## Acceptance

- Tapping today's pill on either card fills it and survives an app restart.
- A second tap on the same day does nothing; the next day's pill becomes
  tappable.
- On a Sunday the QT bar shows its Sunday pill muted and reads `n/6일`.
- Both profile tiles show a day count that matches the sum of marked days, and
  a dash when the call has not returned.
- With the records endpoint absent, both cards render exactly as they do today
  and Home shows no error.

## Scope note

This adds a server subsystem to a release that already has one open blocker
(the weekly-verse selection). Post-MVP is the calmer place for it. Shipping it
in v1.0.0 is possible and moves the date.
