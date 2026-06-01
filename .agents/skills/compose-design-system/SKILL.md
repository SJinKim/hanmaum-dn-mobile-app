---
name: compose-design-system
description: Use for Compose Multiplatform UI work in this repo, especially new screens, screen redesigns, component extraction, spacing/color/typography fixes, and visual consistency work against designs/dn_app/DESIGN.md.
---

# Compose Design System

Use this skill for UI and visual changes.

## Rules

1. Read `designs/dn_app/DESIGN.md` before editing UI.
2. Reuse tokens from `core/presentation/theme/` for colors, typography, shapes, and motion.
3. Prefer existing shared components in `core/presentation/components/`.
4. New screen from scratch: use lazyweb quick references when the tool is available; otherwise state fallback.
5. Significant redesign: use lazyweb design improve with a current screenshot when available; otherwise state fallback.
6. Minor padding/text fixes: apply design tokens directly.
7. Animations must use spring specs; never linear/ease-in-out.
8. Avoid expensive computation in composables. Use ViewModel, `remember`, stable lazy keys, and `derivedStateOf` appropriately.
9. Check accessibility: labels/content descriptions, tap target size, text scaling, contrast.

## Verification

For UI changes, run relevant compile/build checks and summarize visual/design assumptions if screenshots/emulator verification were not possible.
