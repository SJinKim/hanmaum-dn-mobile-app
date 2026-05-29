# Design System Specification: DN App — v2.0

## 1. Overview & Creative North Star

**Creative North Star: Warm Premium**

The DN app is a sanctuary in your pocket — calm, intentional, and unmistakably high-quality. The aesthetic is warm lifestyle meets premium polish: think Headspace Pro crossed with a luxury editorial brand. It should feel like the best Korean apps you've used (Toss, Kakao Pay) but with warmth and community at its core.

Two axes define every decision:
- **Warm:** Cream surfaces, espresso darks, terracotta accents. Never cold greys, never clinical whites.
- **Premium:** Pretendard at heavy weights, generous whitespace, tonal depth instead of drop shadows, floating navigation that signals refinement on first open.

**Adaptive by default.** The app follows the system's light/dark preference. Light mode is the primary design surface; dark mode is an equally considered first-class experience, not an afterthought.

> ⚠️ **CI Pending:** Primary, secondary, and accent color tokens below use warm terracotta as a placeholder. All `primary` / `secondary` / `accent` values will be swapped once corporate identity is confirmed. Surface, typography, motion, and shape tokens are final.

---

## 2. Color & Surface Tokens

### Light Mode

| Token | Value | Usage |
|---|---|---|
| `surface` | `#fdf8f4` | Page background |
| `surface_container_low` | `#faf3ed` | Section backgrounds, dividers via shift |
| `surface_container` | `#f5ebe0` | Grouped content backgrounds |
| `surface_container_lowest` | `#ffffff` | Cards, sheets — highest contrast pop |
| `on_surface` | `#2c1a0e` | Primary text |
| `on_surface_variant` | `#5a3a28` | Secondary text, descriptions |
| `muted` | `#c4a882` | Labels, placeholders, inactive icons |
| `outline_variant` | `rgba(196,168,130,0.15)` | Ghost borders (accessibility fallback only) |
| `primary` *(CI pending)* | `#c07a50` | Primary actions, hero gradients |
| `primary_dark` *(CI pending)* | `#8a4a28` | Gradient end, pressed states |
| `on_primary` | `#ffffff` | Text on primary |

### Dark Mode

| Token | Value | Usage |
|---|---|---|
| `surface` | `#1a1208` | Page background |
| `surface_container_low` | `#120d05` | Section backgrounds |
| `surface_container` | `#221508` | Grouped content backgrounds |
| `surface_container_lowest` | `#0d0905` | Cards, sheets |
| `on_surface` | `#f5e6cc` | Primary text |
| `on_surface_variant` | `#c4a070` | Secondary text, descriptions |
| `muted` | `#8a6a3a` | Labels, placeholders, inactive icons |
| `outline_variant` | `rgba(138,106,58,0.18)` | Ghost borders (accessibility fallback only) |
| `primary` *(CI pending)* | `#a0622a` | Primary actions |
| `primary_dark` *(CI pending)* | `#6a3a10` | Gradient end |
| `on_primary` | `#fde8c0` | Text on primary |

### The No-Line Rule
**1px divider lines are strictly prohibited.** Separate content sections by shifting surface tokens — move from `surface` to `surface_container_low` between blocks. This creates a seamless, organic boundary. If an accessibility border is unavoidable, use `outline_variant` at exactly 15% opacity: it must be felt, not seen.

### Hero Gradients
Large CTAs, announcement cards, and hero sections use a diagonal gradient:
```
Brush(135°): primary → primary_dark
```
Add a subtle circular overlay at `rgba(255,255,255,0.07)` offset top-right for depth.

---

## 3. Typography — Pretendard Variable

**Font:** [Pretendard Variable](https://github.com/orioncactus/pretendard) — the Korean premium standard, used by Toss, Kakao Pay, and Coupang. Handles both Korean and Latin with equal precision across the full 100–900 weight range.

### Type Scale

| Role | Weight | Size | Tracking | Line Height |
|---|---|---|---|---|
| `display` | 900 | 32sp | −1.2sp | 1.05 |
| `headline` | 800 | 22sp | −0.8sp | 1.10 |
| `title_large` | 700 | 17sp | −0.4sp | 1.15 |
| `title_medium` | 600 | 14sp | −0.2sp | 1.20 |
| `body_large` | 400 | 14sp | 0sp | 1.60 |
| `body_medium` | 400 | 12sp | 0sp | 1.60 |
| `label` | 700 | 10sp | +2.0sp | 1.00 |

Labels are always UPPERCASE with +2sp tracking. Use them for eyebrow text, section category tags, and status badges.

### Korean-Specific Rules
- **Max tracking for Korean:** −0.5sp. Korean glyphs are wider — over-tightening breaks readability.
- **Body line height for Korean:** 1.65–1.75. Korean characters are taller and need more vertical breathing room.
- **No italic.** Korean has no italic tradition. Do not apply `FontStyle.Italic` to any Korean text.
- **No serif.** Pretendard is the single typeface across all scripts. No mixed-typeface pairings.
- Weight semantics are identical for Korean and Latin — match role to weight, not script.

---

## 4. Shape — Corner Radius

| Tier | Radius | Applied To |
|---|---|---|
| `shape_small` | 6dp | Input fields, filter chips, badges, tooltips |
| `shape_medium` | 14dp | List cards, announcement rows, modals |
| `shape_large` | 20dp | Hero cards, bottom sheets, image containers |
| `shape_full` | `∞` (100dp+) | Buttons, floating pill nav, avatar borders |

**No sharp corners anywhere.** If a shape requires a right angle for layout reasons, clip it with at least `shape_small`.

---

## 5. Elevation & Depth

Depth is expressed through **tonal layering**, not drop shadows.

**The layering principle:** A `surface_container_lowest` card placed on a `surface_container_low` background reads as elevated without any shadow. Stack surfaces intentionally — never place the same surface token on itself.

**Ambient shadow** (floating elements only — pill nav, bottom sheets, modals):
```
shadow: 0px 8px 32px rgba(44, 26, 14, 0.14)
```
The shadow tint is always a deep, warm transparent — never cool grey.

**Ghost border fallback** (accessibility, when tonal contrast is insufficient):
```
border: 1px solid outline_variant  // 15% opacity — felt, not seen
```

---

## 6. Navigation — Floating Pill

The primary navigation is a **floating pill** anchored 16dp above the system bottom safe area inset.

**Anatomy:**
- Container: `rgba(44, 26, 14, 0.86)` with `backdropBlur(24dp)` — same in both modes
- Shape: `shape_full` (pill)
- Shadow: `0px 6px 28px rgba(0, 0, 0, 0.18)`
- Active tab: icon + label, background `rgba(196, 168, 130, 0.18)`
- Inactive tab: icon only, `muted` color

**Tab switching animation:**
- Active indicator slides between tabs with `spring(dampingRatio=0.85, stiffness=280)`
- Screen content cross-fades + 12dp Y-translate on entry

**Placement rule:** Content scrolls beneath the pill. The last list item must have `paddingBottom = 80dp` to avoid being obscured.

---

## 7. Core Components

### Buttons
- **Primary:** Pill shape (`shape_full`), `primary → primary_dark` gradient background, `on_primary` text, `title_medium` weight. Full-width or wrap-content.
- **Secondary:** Pill shape, `primary` color at 10% alpha background, `primary` text, `outline_variant` border. Ghost style.
- **Press state:** Scale to `0.97` with `spring(dampingRatio=0.6, stiffness=400)`. Never color-only feedback.
- **Disabled:** 38% opacity, no press animation.

### Cards

**Hero Card** (primary CTA, featured announcement):
- `primary → primary_dark` gradient, `shape_large` corner
- Circular glare overlay at `rgba(255,255,255,0.07)`, top-right, 120dp diameter
- Internal padding: 18dp
- Title: `title_large`, `on_primary`
- Eyebrow label: `label`, `on_primary` at 55% alpha

**Standard Card** (list items, secondary content):
- `surface_container_lowest` background, `shape_medium` corner
- `outline_variant` border (ghost — accessibility)
- Ambient shadow: `0px 2px 12px rgba(44,26,14,0.05)`
- Internal padding: 14dp

### Input Fields
- Background: `surface_container` (10% primary alpha in focus)
- Corner: `shape_small` (6dp)
- Border: none at rest; `1.5px primary at 50% alpha` on focus
- Placeholder: `muted` color, `body_large` weight 400
- Active text: `on_surface`, `body_large` weight 500
- Label: floats above at `label` style on focus

### Filter Chips
- Unselected: `surface_container_low` background, `muted` text, `shape_full`
- Selected: `primary` at 15% alpha background, `primary` text, `primary` at 30% alpha border
- Press: scale `0.97`, spring

### Avatar
- Shape: `shape_full` (circle)
- Border: `1.5dp outline_variant`
- Fallback: `primary → primary_dark` gradient with initial letter in `on_primary`

---

## 8. Motion — Hybrid Spring System

**Philosophy:** Save expressive motion for navigation moments that deserve it. Use subtle spring for all micro-interactions. Never use linear timing. Never use ease-in-out. Every animation must use a `spring()` spec.

### Spring Specs

| Context | Spec | Notes |
|---|---|---|
| Card → Detail | `spring(dampingRatio=0.75, stiffness=200)` | Shared element — card expands to fill screen |
| Tab switch | `spring(dampingRatio=0.85, stiffness=280)` | Pill indicator slides; screen cross-fades at 200ms |
| Screen push | `spring(dampingRatio=0.80, stiffness=250)` | Slide from right (push) or bottom (modal) |
| Button / card press | `spring(dampingRatio=0.60, stiffness=400)` | Scale 1.0 → 0.97 → 1.0 |
| List stagger | `spring(dampingRatio=0.85, stiffness=260)` | 40ms delay per item; max 5 staggered |
| Fade transitions | `tween(200ms)` + spring on transform | Opacity only: short tween is acceptable |

### Shared Element Transitions (Card → Detail)
When a user taps a card, the card's container morphs into the detail screen's hero. The card background gradient stretches to fill; the title animates into the detail headline position. Use `SharedTransitionScope` in Compose.

### List Stagger
On initial load or navigation arrival, list items animate in:
- Entry: `alpha 0→1` + `translationY 8dp→0`
- Each item delayed by `40ms * index`
- Cap at 5 staggered items — remaining items appear at delay of item 5

### Banned
- `LinearEasing` — banned entirely
- `FastOutSlowInEasing` / `EaseInOut` — banned
- Animations longer than `500ms` total duration
- Bounce scale exceeding `1.05`
- Color-only press feedback (must always pair with scale)

---

## 9. Back Navigation

Both a **swipe gesture** and a **back icon** are required on every detail/sub screen. They are not redundant — they serve different users.

| Mechanism | Who it serves | Platform |
|---|---|---|
| Swipe-to-go-back | Power users, muscle memory | iOS (system-level), Android 14+ predictive back |
| Back icon (`<`) | New users, accessibility, motor impairments | Both |

### Back Icon
- **Glyph:** Chevron left (`<`) — not an arrow, not text
- **Position:** Top-left, vertically centered with the screen title or hero area
- **Color:** `muted` at rest; `on_surface` on press
- **Touch target:** Minimum 44dp × 44dp (icon itself can be 24dp visually)
- **No label.** The icon alone is sufficient — a "Back" text label is visual clutter

### Swipe Gesture
- Support Android 14 Predictive Back — the outgoing screen scales and peeks behind during the swipe, giving physical feedback before committing
- On iOS, the system `UINavigationController` swipe-from-left-edge is active by default — do not intercept or disable it
- Swipe entry animation uses the same spring spec as screen push: `spring(dampingRatio=0.80, stiffness=250)`

### When Both Are Present
The back icon and the swipe gesture must behave identically — they both pop the back stack. Never use a back icon that does something different (e.g. dismiss vs. navigate back). One action, two affordances.

---

## 10. Spacing

Base unit: **4dp**. All spacing is a multiple of 4.

| Token | Value | Usage |
|---|---|---|
| `space_xs` | 4dp | Icon-to-label gap, badge padding |
| `space_sm` | 8dp | Between related elements, mini-card gap |
| `space_md` | 16dp | Horizontal screen padding (baseline), card gap |
| `space_lg` | 24dp | Between content sections |
| `space_xl` | 32dp | Above major headings, between feature blocks |
| `space_bottom_nav` | 80dp | Bottom padding to clear floating pill |

Horizontal screen padding is always `space_md` (16dp) on both sides.

---

## 11. Do's and Don'ts

### Do
- Use surface token shifts to separate content (never lines)
- Use `spring()` for every single animation — no exceptions
- Use Pretendard for all text including Korean
- Use 1.65+ line-height for Korean body text
- Use the floating pill for all primary tab navigation
- Use tonal layering for depth, ambient shadow for floating elements only
- Use hero gradient cards for primary CTAs and featured content
- Use `shape_full` for all interactive pill shapes (buttons, chips, nav)
- Respect system dark/light mode — both modes are first class

### Don't
- No 1px divider lines between content sections
- No pure black (`#000000`) anywhere — use `on_surface` tokens
- No sharp 90° corners — minimum `shape_small` (6dp) on everything
- No `LinearEasing` or `ease-in-out` transitions — spring only
- No drop shadows — use tonal layering and ambient shadows
- No italic or serif fonts — Korean has no italic tradition
- No fixed CI colors for `primary`/`secondary` yet — use placeholder tokens
- No crowded layouts — when a screen feels full, remove an element
- No text at 100% opacity directly on pure white — use surface tokens
- No animations longer than 500ms
