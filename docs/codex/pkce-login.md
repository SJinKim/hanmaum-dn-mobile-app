# Mobile browser login (#268)

The approved UX is the [PKCE login board in DN App](https://www.figma.com/design/NOVmvVvB7CEXXVemOy5hJa/DN-App?node-id=542-3878).

## Implementation boundaries

- The app shows the DN entry, optional biometric action, permanent registration
  notice, inline retry and callback-loading state. It never collects a login password.
  Its shared background insets the foreground (not the glows): the current NavHost
  is edge-to-edge despite DESIGN.md's older statement about root-owned insets.
- Android uses the external default browser and a lifecycle-tracked callback activity.
  iOS uses `ASWebAuthenticationSession`. Neither uses a WebView or a new dependency.
- Keycloak owns credentials, wrong-password errors, password reset, verification,
  MFA and other required actions. Browser chrome in Figma is illustrative; the app
  cannot draw it. The deployed `hanmaum` web theme remains server-owned.
- A cryptographically random verifier and state exist only in memory. Each callback
  consumes the transaction. Exact redirect, state, duplicate parameters, fragments
  and an optional issuer are checked before exchanging a code. Process death abandons
  login safely; a stale callback cannot create a session.
- `openid offline_access` is requested at authorization. Rotated refresh tokens
  remain in protected storage; an expired biometric grant clears only its unusable
  vault. Cancelling is silent and never prompts again automatically.
- Registration retains the church-specific form, sends its password only when
  creating the account, clears it on success and returns to login with a verification
  notice. There is no post-registration password grant.
- Logout stays device-local, retaining the explicitly armed Face ID vault. The next
  browser authorization uses `prompt=login`, so browser SSO cannot silently undo logout.
  No realm-wide/end-session request is made: that would revoke the retained session.
- Auth code exchange and refresh share `MobileAuthConfig`. All calls use the shared
  network client. Only the backend can trigger bearer re-authorization; every outgoing
  send also strips bearer headers from foreign hosts, including challenge retries.
  Authorization endpoints are excluded from HTTP logs; bearer headers are redacted.

## Environment matrix

| Build | API | Realm | Keycloak |
|---|---|---|---|
| Android dev | `http://10.0.2.2:8080` | `hanmaum` | `http://10.0.2.2:8091` |
| iOS local defaults | `http://localhost:8080` | `hanmaum` | `http://localhost:8091` |
| Android ST / iOS TestFlight | `https://api.staging.graceops.de` | `hanmaum-dn-st` | `https://auth.graceops.de` |
| Android prod / iOS App Store | `https://api.graceops.de` | `hanmaum-dn-prod` | `https://auth.graceops.de` |

Public client: `hanmaum-mobile`. Redirect: `com.hanmaum.dn.mobile:/oauth2redirect`.
Both are BuildKonfig fields (Android delegates to flavor-aware BuildConfig).
iOS environment URLs/realm continue to come from the `.env` values written by the
existing distribution workflows. No secrets are used by this public client.

## Rollout gate

On 2026-10-05, a non-credential ST authorization probe with S256 and this redirect
returned HTTP 200 and rendered username/password fields. This proves the request
and redirect are accepted, **not** a successful end-to-end mobile login.

Before rollout, verify on real ST Android and iOS devices:

1. Login, wrong password, password reset, required email verification, browser close
   and callback return. Route ACTIVE/PENDING/REJECTED from the fetched profile.
2. Double-tap, old/duplicate callback, app process death while in browser, and retry.
3. Offline retry, 401 refresh rotation, rejected refresh and 403 without refresh.
4. Logout, explicit browser reauthentication, Face ID cancellation, rotation and expiry.
5. Dark/light layout and large text against Figma. Browser theme changes, if required,
   belong in the server repo rather than fake app-owned credential screens.

Server #235 still has outstanding production validation. Do not ship to prod or
disable Direct Access Grants until ST end-to-end migration succeeds. This change
does not deploy, modify Keycloak, or dispatch TestFlight.

## Local verification (2026-10-05)

- Focused PKCE, repository, login/registration and network tests: passed.
- `./gradlew :composeApp:testDevDebugUnitTest`: 595 tests, no failures (including the cancellation regression fix).
- `./gradlew lint`: passed; `lint-results-devDebug.xml` has 0 errors.
- `./gradlew :composeApp:assembleDevDebug :composeApp:assembleStDebug`: passed.
- `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:iosSimulatorArm64Test`:
  passed again after the cancellation fix (1m46s).
- `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild build -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO`:
  `BUILD SUCCEEDED`. No app was launched.
  Not repeated for the cancellation-only fix: no Swift-called declaration changed.
- `git diff --check` and the CI unreferenced-TODO gate: clean.

Visual device comparison (dark/light/large text), browser lifecycle behavior and
credentialed ST end-to-end login remain manual rollout gates, not claims of this
local verification. The existing unrelated album specification is untouched.

## Face ID cancellation regression

The review found that a cancelled refresh propagated `CancellationException` but
left `isLoading=true` on a surviving LoginViewModel. The fix releases loading and
the status message before propagating cancellation. It does not clear the vault,
disable Face ID, show an error or automatically authenticate again.

Two new tests cancel a genuinely suspended refresh and prove browser login and an
explicit Face ID retry both work on the same ViewModel. The existing prompt-cancel
test also proves browser login remains available. Both new tests failed before the
fix; all 19 login/Face ID tests now pass, followed by full JVM tests, lint, dev APK,
native iOS tests, diff whitespace and the unreferenced-TODO gate.
