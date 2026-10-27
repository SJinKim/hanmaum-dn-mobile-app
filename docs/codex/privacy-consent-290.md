# Privacy consent — issue #290

## Status

Design approved by the user on 2026-10-24. The APP_PRIVACY mobile flow is
implemented against the proposed ops #73 contract. Local verification passed on
2026-10-25; the change is prepared for independent review as a draft PR. The server/ops merges, published policy URL and legal content approval
remain integration/release prerequisites. Issue #290 remains open for those
items and the separate COURSE_APPLICATION contract.
Branch: `feature/privacy-consent-290`, based on `origin/main` at `718ea7b`.

Infrastructure clarification, 2026-10-24:

- User confirmed ST hosting at Hetzner in Germany, with ST database and Keycloak
  also operated in Germany. This is operator-provided information, not a live
  infrastructure or contract audit.
- Prod uses a different hosting provider. Its provider and locations remain
  unspecified; do not copy the ST values into the production privacy policy.
- Existing [server #269](https://github.com/SJinKim/hanmaum-dn-server/issues/269)
  already tracks production setup and was extended with provider/region inventory,
  environment separation and a review of the shared-Keycloak decision. No duplicate
  issue was created. No infrastructure or deployments were changed.

Review artifacts:

- [Figma: consent states in Dark and Light](https://www.figma.com/design/NOVmvVvB7CEXXVemOy5hJa/DN-App?node-id=639-5002)
- Full privacy-policy working drafts in [German](privacy-policy.de.draft.md),
  [Korean](privacy-policy.ko.draft.md) and [English](privacy-policy.en.draft.md),
  using the church name, address and contact supplied by the user; not legally
  approved. Each language contains the same nine sections, thirteen subsections,
  two tables and twenty open editorial review items. No language takes precedence.

Localization update, 2026-10-26: all three drafts include the operator's intended
audience (older than 16 / aged 17 and above) and the stated process without
collecting parental consent. The qualification that 17-year-olds remain minors
and the outstanding legal/implementation review are retained in each language.
The operator confirmed that account approvals and course admissions are always
decided by a responsible person. This is now stated in each language; review of
other automated processing (matching, prerequisite/location checks and any
profiling) remains separate.
The local static preview offers a script-free language switch and complete
localized navigation, status notices and review notes. Fifteen browser cases
(three languages, desktop/mobile, light/dark, plus 320px mobile) passed; no page
overflow, broken anchors, scripts or external resource requests were observed.
Source references and technical identifiers match across the three documents.
No public content or App/Store URL was changed by this localization work.

Church-policy update, 2026-10-27: Jin supplied the existing Korean policy from
application.hanmaum.de and confirmed that its church-wide rules apply to this
app. [The source mapping](privacy-policy-church-source-2026-10-27.md) documents
all source articles and their adoption boundaries. Each draft now identifies
프랑크푸르트 한마음교회, internal privacy responsibility (이찬규), enquiry/access
handling (이충만 목사) and the church office telephone. The account-retention
criterion, purpose limitation, request/representation process, deletion approval
and methods, separate statutory retention, and organisational security rules are
also incorporated. These rules are distinguished from verified app implementation.
No formal GDPR data-protection-officer appointment or legal representative was
inferred. Korean legal references/authorities, the historical 2020 effective date,
under-14 rule, blanket non-disclosure, website cookies and low-level formatting
are not adopted as app facts. The course portal and possible family fields are
supported by the supplied source and targeted server/public-page checks; hosting,
necessity and legal basis remain to be confirmed.

The editable Figma board uses DN Theme modes/tokens and the documented font
stand-ins Inter and Noto Sans KR. The latest design contexts include the
registration footer, settings proof and withdrawal link, consent prompt, version
notice, and withdrawal confirmation. Compose reuses DN controls, icons, spacing,
shapes and typography. The board covers unchecked/checked, missing/outdated,
saving, load/save failure, policy loading and withdrawal confirmation states.
The app's device rendering still needs TestFlight visual acceptance; source
comparison and native tests do not establish that visual acceptance.

Integration/release prerequisites:

- The Figma states are approved. The policy itself still needs content approval;
  localized Korean/English UI copy is not a legal approval of that policy.
- Confirmed controller/legal form, applicable privacy law, purpose-specific legal
  bases, actual hosting/providers/regions and remaining retention periods. The
  supplied church policy now identifies internal privacy contacts; a formal DPO
  appointment and legal representative are still unconfirmed.
- A published privacy-policy URL serving the same version announced by the server.
  In-app withdrawal is implemented: record the withdrawal and sign out, without
  deleting the account.
- Merged server #359 and ops #73; a course-submit contract for COURSE_APPLICATION.

## Implemented behavior

- Registration loads the public policy version, starts unchecked, and sends
  `consent: { accepted: true, version }` only after an explicit selection. Policy
  reloads and version conflicts clear acceptance. Rapid submit is guarded.
- A session-scoped guard checks server consent before composing member-content
  destinations, including restored routes and notification targets. The static
  rejected-account screen retains its previous behavior. No local persisted flag
  authorizes access. Account changes invalidate the previous check.
- Settings displays server-provided grant date/version and a separate policy link.
  A failed refresh is shown as unknown/error, never as absent consent.
- Withdrawal requires confirmation and a successful DELETE before logout. Device
  push cleanup is bounded; session tokens, Ktor bearer cache and biometric arming
  are cleared, and geofencing is stopped. This is not account/data deletion.
- Geofence startup is deferred until verified consent and an active destination.
  The existing location opt-in remains independent of the consent checkbox.

## Policy URL configuration

Local builds read `PRIVACY_POLICY_URL` from `.env`. Distribution maps GitHub
repository variables `ST_PRIVACY_POLICY_URL` and `PROD_PRIVACY_POLICY_URL` to that
field in the corresponding Android/iOS lanes. BuildKonfig's common declaration,
Android generated actual and AGP BuildConfig all include it.

Only HTTPS URLs without embedded credentials are accepted. No URL or policy
version is invented. An absent/invalid URL prevents new consent and registration;
an already confirmed server grant can still allow the existing account session.
Set these variables only after the policy is approved and published. Coordinate
policy text/version changes with the server; the current contract returns no URL
or policy text and cannot prove website content consistency by itself.

## Verification

Verified on 2026-10-25 against the final source changes:

| Command | Result |
| --- | --- |
| Focused consent/auth/registration JVM tests | 147 passed before the final no-session regression was added |
| `./gradlew :composeApp:testDevDebugUnitTest` | 727 tests, 0 failures/errors/skips; BUILD SUCCESSFUL in 39s |
| `./gradlew lint` | 0 errors in `lint-results-devDebug.xml`; BUILD SUCCESSFUL in 49s |
| `./gradlew :composeApp:assembleDevDebug` | BUILD SUCCESSFUL in 37s |
| `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:iosSimulatorArm64Test` | 722 tests, 0 failures/errors/skips; BUILD SUCCESSFUL in 3m 26s |
| `grep -rn "TODO" composeApp/src` filtered with `grep -v "TODO("` | No unreferenced TODOs |

Distribution YAML syntax and policy-variable mappings were also checked.
`git diff --check` is clean. This is developer verification, not an independent
code review or QA acceptance. Claude A is the designated independent reviewer.
The repository has no `.team/config.yml`; board synchronization and a board-stage
transition cannot be performed reliably until team onboarding is configured.
No team configuration or project identity is invented in this feature PR.

 No simulator app launch or distribution workflow is part of this PR.
No changed Kotlin declaration is called by the Swift target, so the conditional
Swift interop build is not required. End-to-end server storage, rollout and
TestFlight UI/runtime acceptance remain outstanding.

## Execution plan

1. Inspect the registration/settings flows and the proposed server contract; build
   a reviewable Figma state board in Dark and Light using DN tokens/components.
2. Design approved before application code, as required by issue #290 and
   `tasks/lessons.md` ("Bei Zustandsvielfalt zuerst das Figma-Board, dann der Code").
3. Prepare implementation against ops #73, then compare with the committed ops
   spec after server #359 and ops #73 merge before integration/release:
   consent repository, registration grant, authenticated consent check/re-prompt,
   settings policy link and grant date/version. Wire COURSE_APPLICATION only after
   its submit endpoint accepts the consent in the server contract.
4. Add focused repository/ViewModel tests, run the repository Definition of Done,
   compare UI states against the approved board, and prepare a PR to main.

## Acceptance criteria

- Registration consent starts unchecked and cannot be submitted without explicit
  acceptance of the version that was displayed; the server remains authoritative.
- Public policy versions come from `GET /api/v1/consents/policies`, never a guessed
  or hardcoded version. Loading/failure/outdated-version states are visible.
- Registration sends `consent: { accepted: true, version }`.
- After login, `GET /api/v1/members/me/consents` determines `actionRequired`.
  MISSING and OUTDATED have distinct explanations. No local flag claims a grant.
- Re-prompt submits `POST /api/v1/members/me/consents` with purpose APP_PRIVACY.
  Submission is explicit, errors are retryable, and a 409 refetches the policy and
  clears the old acceptance instead of silently accepting a changed version.
- Settings shows the policy link and server-confirmed grant timestamp/version;
  loading and fetch failure never look like "not consented".
- Korean/English copy, accessibility labels, scalable text and mobile tap targets.
- No new production dependency, server/schema change, deployment or enforcement
  configuration change is included in the mobile work.

## Dependencies and decisions

- Server PR: https://github.com/SJinKim/hanmaum-dn-server/pull/359 — open at inspection.
- Ops PR: https://github.com/SJinKim/hanmaum-dn-ops/pull/73 — open at inspection.
- Proposed policy response has purpose/version/requiredForAccount, no policy URL.
  A published, approved privacy-policy URL is required; requested from the user.
- Consent copy is a UX draft until the church approves it. Do not invent final
  policy content or a policy URL.
- COURSE_APPLICATION has a purpose but its application endpoint is not wired yet,
  per the issue comment; do not send an independent grant that falsely suggests
  the course submission and consent recording are atomic.
- The approved withdrawal UX records the server withdrawal and ends the local
  session. It does not claim account deletion or backend-wide processing cessation.
- Server #359 now requires registration consent without a rollout toggle. The
  earlier APP_CONSENTS_ENFORCED rollout note was superseded. Coordinate backend
  deployment with the compatible mobile release; do not alter server settings here.

## Verification after implementation

Use the exact commands from AGENTS.md: focused JVM tests, all DevDebug unit tests,
the unreferenced-TODO gate, lint (read error count from the XML report),
iosSimulatorArm64Test with DEVELOPER_DIR, and assembleDevDebug. Run the Swift
interop build only if a Kotlin declaration used by Swift changes. Do not launch
the app in a simulator or dispatch a TestFlight build.
