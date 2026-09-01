# OpenAPI contract drift detection across the three repos

**Date:** 2026-09-01
**Status:** Spec — defaults chosen, awaiting confirmation
**Repos affected:** `hanmaum-dn-server`, `hanmaum-dn-ops`, `hanmaum-dn-mobile-app`, `hanmaum-dn-web-app`

## Problem

Changing an endpoint on the server means opening every consumer repo by hand to
work out what it breaks. There is no mechanism that answers "I renamed
`/ministries/{id}/registrations` — who called it?"

This is not hypothetical. Measured on 2026-09-01, **11 of 23 relative paths the
mobile app calls have no matching path in the committed spec**:

| Mobile call site | In `ops/api/openapi.yaml`? | Server controller exists? |
|---|---|---|
| `me/notifications` (+5 more under it) | no | **yes** — `NotificationController.kt` |
| `me/device-tokens`, `me/device-tokens/{token}` | no | **yes** — same controller |
| `me/notification-settings` | no | **yes** — same controller |
| `church/location` | no | **yes** — `@RequestMapping("/church")` + `@GetMapping("/location")` |
| `ministries/{id}/registrations`, `.../registrations/me` | no | spec has `/members`, not `/registrations` |
| `events/rsvps/{id}/response` | no | — |

`NotificationController.kt` alone declares 12 mappings under `@RequestMapping("/me")`,
none of which appear in the spec. **The spec is stale; the mobile app is correct.**
Nobody noticed because nothing checks.

### Why graphify cannot solve this

Investigated and ruled out on 2026-09-01. Graphify indexes *declarations and
calls*, never string literals — no URL path is a node in any of the three graphs
(verified: 1830 mobile nodes, zero path nodes). It therefore has no way to
connect `@GetMapping("/notifications")` in the server to `client.get("me/notifications")`
in the mobile app.

Its cross-repo linking (`same_type_as`) needs an identical namespace **and**
type name. These codebases share no types at all — `com.hanmaum.dn.mobile.*`
against `com.hanmaum.dn.app.*`, each declaring its own DTOs and talking JSON over
HTTP. Renaming packages to force a match would be a large, risky diff for zero
gain, since there is no shared type to link in the first place.

Graphify stays what it is good for: navigation inside one repo (19.3x / 11.7x /
5.8x measured, 30.9x on the merged graph). Contract drift is a different problem
and needs a different tool.

## Root cause of the current drift

The pieces already exist and are well designed:

- `hanmaum-dn-ops/api/openapi.yaml` (53 KB, committed, 35 paths) is the shared contract.
- ADR 0003 centralises the version prefix behind one `api.prefix` property.
- `build.gradle.kts` has a `syncOpenApiToOps` task that copies the generated
  spec into `../hanmaum-dn-ops/api`.

What is missing is that **nothing runs it**. `syncOpenApiToOps` depends on
`generateOpenApiDocs`, which boots the application against
`http://localhost:8080/v3/api-docs.yaml` and therefore needs Docker (Postgres +
Keycloak) up. No workflow in `.github/workflows/` mentions openapi. So the sync
is a manual step on a developer machine, and it has not been run since the
notification and church-location work landed.

## Proposed design

Two layers, deliberately separated by cost.

### Layer 1 — the cheap gate: static path extraction

No app boot, no Docker, runs in seconds on every PR.

**Producer side (server).** Walk `src/main/kotlin/**/api/**/*Controller.kt`,
combine each class's `@RequestMapping` with its method-level
`@Get/Post/Put/Patch/DeleteMapping`, and emit a sorted list of
`METHOD /path` under the `api.prefix`. This is pure text extraction — the same
regex that produced the evidence above found all 95 mappings.

**Consumer side.** Extract the path literals each consumer actually calls:

- *mobile* — `(get|post|put|patch|delete)("…")` in `composeApp/src/commonMain`.
  Found 26 literals cleanly; paths are relative because `NetworkClient` injects
  the `/api/v1/` prefix, and the three absolute external URLs (pCloud,
  openplzapi) correctly bypass it and must be excluded.
- *web-app* — `/v1/…` literals in `src`. Found 16, but note the consumer uses a
  `basePath` field (`basePath = '/v1/events/rsvps'`) concatenated at the call
  site, so extraction must resolve one level of template concatenation or it
  will under-report.

**The check.** Every consumer path must exist in the producer list. A consumer
path with no producer match fails the consumer's PR; a producer path removed
while a consumer still calls it fails the server's PR.

### Layer 2 — the full spec, on a slower cadence

Keep `openapi.yaml` as the schema-level truth (request/response bodies, which
Layer 1 says nothing about) and regenerate it in CI with Postgres and Keycloak
as workflow services, on merge to `main` rather than on every PR. Commit the
result to `hanmaum-dn-ops` so the spec stops drifting silently.

Layer 1 catches the renames and removals — the common case, cheaply. Layer 2
catches field-level changes and keeps the published contract honest.

## Defaults chosen — confirm or override

| # | Question | Default chosen | Why |
|---|---|---|---|
| 1 | Where does the producer path list live? | `hanmaum-dn-ops/api/endpoints.txt`, generated | The ops repo is already the contract's home (ADR 0003, `syncOpenApiToOps`); a flat sorted list diffs readably in a PR, unlike 53 KB of YAML |
| 2 | Does a drift finding fail the build or only warn? | **Fail** on the consumer side, **warn** on the producer side | A consumer calling a nonexistent endpoint is a runtime 404 for a user. A server removing an endpoint may be deliberate and the consumer PR follows later |
| 3 | Layer 1 or both layers first? | Layer 1 only | It needs no Docker, no CI services, and would already have caught all 11 findings above. Layer 2 is a bigger CI change and can follow |
| 4 | Who owns the check script? | `hanmaum-dn-ops`, invoked by each repo's CI | One implementation, three consumers. Duplicating it per repo guarantees the copies drift |
| 5 | The 11 existing findings — fix in this work or separately? | **Separately, first** | Turning on a failing gate before fixing the backlog blocks every PR. Regenerate the spec, land it, then enable the gate |
| 6 | Web-app `basePath` concatenation | Resolve one level | Without it the check silently under-reports and looks green while missing endpoints |

## Open questions — need your decision

1. **Is `ops` the right home for a script?** It has 3 tracked files today and
   reads as a documentation repo. A `scripts/` directory there is a change of
   character. The alternative is a small published package, which is heavier.
2. **Does the web-app's `dev` branch change the gate's placement?** Web-app PRs
   target `dev`, mobile and server target `main`. The gate should run on the
   branch the PR targets, but "the contract" is whatever is on `ops/main`.
   A consumer feature branch may legitimately call an endpoint whose server
   PR has not merged yet — the gate must not deadlock that ordering.
3. **Should Layer 1 check HTTP methods, or paths only?** Methods catch a
   `@PostMapping` turning into `@PutMapping`. Paths alone are simpler and cover
   the observed drift. Methods are the stricter choice.

## Out of scope

- Generating mobile or web-app clients from the spec. That is a much larger
  change to how DTOs are authored, and the repos deliberately hand-write DTOs
  today (CLAUDE.md §3).
- Runtime contract testing (Pact or similar).
- Anything touching graphify, which is settled and unrelated.

## Acceptance

- [ ] The 11 findings above are resolved — spec regenerated and committed to ops
- [ ] Producer list generated from server controllers, no app boot required
- [ ] Mobile and web-app consumer extraction verified against the known call
      counts (26 and 16 respectively) so a silent under-report is ruled out
- [ ] A deliberately broken call site fails its repo's PR check (proven, not assumed)
- [ ] The ordering problem from open question 2 has a stated answer
