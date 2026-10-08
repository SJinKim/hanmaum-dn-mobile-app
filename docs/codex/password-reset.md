# Password reset (#198)

## Decision

Use the Keycloak reset flow within the existing native PKCE browser session (#268).
The member chooses account sign-in, then **Forgot your password?** on the secure
Keycloak page, enters their email, and opens the emailed reset link. After setting
a new password, they can return to the app and start account sign-in again.

This supersedes the issue's older password-grant/native-form proposal. No extra
native credential/reset screen or new backend endpoint is required. The approved
DN App PKCE board already includes the browser reset and reset-sent states.

The entry's explanation is translated through `LocalStrings` for KO/EN/DE and
points to the reset link. The authorization request passes the chosen language
as `ui_locales`, so the browser login and reset do not depend on the browser's
default language. Keycloak owns the browser translations rather than AppStrings.

## Server companion

Companion PR: [hanmaum-dn-server#288](https://github.com/SJinKim/hanmaum-dn-server/pull/288).

The server theme adds German messages and declares KO/EN/DE. The realm setup
script enables these languages and keeps password reset enabled. These files
must be deployed to ST and the realm configuration applied before German appears
on the live browser page. No configuration or deployment is performed by this PR.

## Verification limits

On 2026-10-07 a read-only ST authorization probe displayed the password-reset
link. This proves discoverability, not mail delivery or password replacement.
Do not submit real addresses or change passwords during automated probes.

Before closing the issue, use a designated test account on ST Android/iOS:

1. Select KO/EN/DE, open account sign-in, and reach the reset page in that language.
2. Request a reset for the test account and a nonexistent address. Both must show
   the same generic confirmation without disclosing account existence.
3. Receive the email, set a new password, then return and sign in from the app.
4. Check expired links, browser cancellation and retry; verify dark/light and large text.

The app does not accept an unvalidated email link as an authorization callback:
the existing PKCE redirect/state/code validation remains mandatory.

Keycloak 26.3.0's [ResetCredentialEmail implementation](https://github.com/keycloak/keycloak/blob/26.3.0/services/src/main/java/org/keycloak/authentication/authenticators/resetcred/ResetCredentialEmail.java)
uses the same EMAIL_SENT confirmation for absent users, successful requests and
email delivery errors. The theme keeps that behavior rather than implementing an
account lookup in the app. This source check does not replace the ST comparison above.
