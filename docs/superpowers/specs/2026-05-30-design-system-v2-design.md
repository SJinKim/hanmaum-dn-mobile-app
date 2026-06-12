---
title: DN App Design System v2.0
date: 2026-05-30
status: approved
---

# DN App Design System v2.0 — Brainstorming Spec

## Summary of Decisions

| Pillar | Decision | Rationale |
|---|---|---|
| Mood | Warm Premium | Warm Lifestyle direction + premium polish layer |
| Mode | Adaptive (light + dark) | Gold standard for premium apps; both modes first-class |
| Navigation | Floating Pill Nav | 2024–25 premium standard; frees content area |
| Typography | Pretendard Variable | Korean-first, geometric, luxury; used by Toss/Kakao Pay |
| Motion | Hybrid Spring | Expressive for nav transitions, subtle for micro-interactions |
| CI Colors | Pending | Placeholder: warm terracotta (#c07a50 / #8a4a28) |

## Questions Asked & Answers

1. **Overall design direction?** → Warm Lifestyle (C), but with premium feel added
2. **Light, dark, or adaptive?** → Adaptive (C) — follows system setting
3. **Navigation pattern?** → Floating Pill (A) — trending premium standard
4. **Typography system?** → Pretendard (A) — minimal, round, Korean-luxury
5. **Animation philosophy?** → Hybrid (C) — expressive for nav, subtle for micro

## Canonical Design Reference

The authoritative design spec lives at:
```
designs/dn_app/DESIGN.md
```

This file is the single source of truth for all UI implementation. All screens must reference it.

## Implementation Scope

The design system overhaul touches:
- `core/presentation/theme/` — MaterialTheme tokens, color scheme, typography
- All screen composables — update surface colors, corner radii, spacing
- Navigation — replace bottom nav bar with floating pill component
- Font assets — add Pretendard Variable OTF to `composeResources/font/`
- Motion — add shared element transition infrastructure

## CI Integration Point

When CI is confirmed, update only the `primary`, `primary_dark`, `secondary`, and `accent` tokens in the theme. All surface, typography, shape, and motion tokens are final and CI-independent.
