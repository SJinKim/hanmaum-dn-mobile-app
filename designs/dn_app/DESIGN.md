# DN App — Design v2

Figma is the design. This file is not a spec and does not restate values —
it says **where the truth lives** and carries the handful of rules that
neither Figma nor the token files can enforce on their own.

**Figma:** [DN App](https://www.figma.com/design/NOVmvVvB7CEXXVemOy5hJa/DN-App)
· page `01 · Foundations` (the system) · `03 · Components`.

## Who owns what

| Question | Ask | Never ask |
|---|---|---|
| What does the screen look like? | Figma | this file |
| What is the exact colour / size / weight? | `core/presentation/theme/` (`DnColors`, `DnTypography`, `AppShapes`, `AppSpacing`, `AppMotion`) | Figma, this file |
| *Why* is this element lime and not blue? | this file, §Colour roles | — |

A number written into prose goes stale and then gets quoted back as fact.
That is why there are no hex values, dp values or font sizes below.

## The language in one paragraph

Dark-first. A near-black canvas (`canvas`) with two or three very soft,
strongly blurred colour glows behind the content; surfaces step up in three
tonal levels (`surface` → `surface2` → `surface3`) instead of using shadows or
1 px dividers. Line icons, Pretendard, generous whitespace. Light mode is the
same system with the mode switched, not a second design.

## Colour roles — the load-bearing rule

Lime is the *action* colour, not the decoration colour. If everything glows
green, green stops meaning anything.

| Token | Owns | Examples |
|---|---|---|
| `lime` | action & attendance | check-in slider, active tab, attendance figures |
| `amber` | word & devotion | 오늘의 말씀, 주간 암송 구절, sermon content — warm, calm, never interactive |
| `blue` | information & dates | 공지, calendar, times, places — informs rather than asks |
| `red` | notification & error | unread counts, form errors, destructive actions — never decorative |

**Rule of thumb: at most one dominant lime element per screen height.**
Everything else stays neutral. Each accent has a matching `on*` foreground and
a `*Dim` background variant — use the dim token, never an alpha on the accent
(see `DnColors`).

## Glass

Glass is a translucent fill + backdrop blur + a 1 px glass edge, and it is
**invisible on a flat surface** — it only reads because of the colour glow
behind the screen. So: glass belongs to the navigation layer only (nav bars,
the floating dock, overlay buttons). Content cards, sheets and dialogs are
opaque `surface`.

## Deliberate divergences from Figma

These are decisions, not drift. Change them only on purpose.

1. **No backdrop blur.** `backdropBlurSupported()` is `false` on both
   platforms (`core/presentation/glass/`), so every glass surface renders as a
   pre-composited opaque stand-in (`glassFillOpaque`). Flipping the flag is
   what "add real blur" means — don't scatter blur modifiers.
2. **One font, not two.** Figma uses Inter + Noto Sans KR as a stand-in
   because Pretendard is not available there. The app ships Pretendard for
   both scripts, so the `en/*` and `ko/*` styles collapse into one
   `DnTypography` scale.

## Non-negotiables for a screen

Also enforced in review — see `CLAUDE.md` §7 for the full checklist.

- Every screen renders through the shared scaffold and top bar in
  `core/presentation/components/`. Opting out needs a comment saying why.
- **A literal colour, `dp`, radius or font size in a screen file is a review
  failure.** If a value is missing from the scale, extend the scale.
- Sections separate by a surface-token shift. No 1 px dividers, no shadows.
- Every animation is `spring()` (`AppMotion`); opacity-only fades may be a
  short `tween`. Press feedback is a scale change, never colour alone.
- Back is a chevron-left with a 44 dp target, and system swipe-back must pop
  the same route.
- Korean body text needs its full line height; labels are UPPERCASE `label`.
- Light **and** dark are both first-class. Check both before calling it done.
- Don't apply window insets in a screen — `App.kt` already does.

## History

This file previously described two earlier systems ("Luminous Sanctuary", then
a light-first "Warm Premium" v2.0 in cream and terracotta). Both were
discarded before shipping. The design that actually ships is the dark-first
system above, landed on `main` in #106. If you find a doc, comment or Figma
frame describing cream surfaces or terracotta accents, it is dead.
