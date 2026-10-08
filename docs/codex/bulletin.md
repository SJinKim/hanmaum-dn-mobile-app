# Weekly bulletin (#167)

The Home quick-menu's first chip opens the current published bulletin. A dated
Home card appears when a published edition can be loaded. The detail screen
renders the order of worship and sermon-sharing blocks as native Compose text.
The calendar action opens a paginated selector for previous published editions.

## Contract

Member requests use the injected shared client:

- `GET /api/v1/bulletins/current`
- `GET /api/v1/bulletins?date=YYYY-MM-DD`
- `GET /api/v1/bulletins/history?page=0&size=20`

The contract is the server's `BulletinEditionResponse` from server PR #294 and
the synchronized ops OpenAPI spec. Fields are camel-case. The server chooses the
current edition in Europe/Berlin; the client does not infer a Sunday or require
the coming Sunday's edition. In particular, an older current edition is valid.
`serviceName`, `serviceStartTime`, the four section titles, VOL and publication
time come from the response. Songs, notices and sharing blocks retain server
order. Questions are numbered only among QUESTION blocks.

404 is an empty/unavailable state. Other failed requests show retry. Malformed
200 responses are errors, not empty states. Only `PUBLISHED` content without a
withdrawal timestamp is accepted. Unknown sharing block types are skipped.
No HTML, WebView, remote attachments, admin endpoints or production dependencies
are introduced: this version of the server contract contains text blocks only.

## Cache

One bounded Settings record stores at most 20 editions, reserving a slot for the
current edition. Its identity combines the backend environment, JWT issuer and
member subject; tokens themselves are not stored here. A different/no session
cannot read a previous member's cached content. Cache write failures do not
discard a successfully loaded response.

Only network/timeouts and 5xx failures may use a copy saved within the last
24 hours. Its saved date and retry action remain visible. A successful refresh
replaces the copy. A 404 removes the affected edition; 401/403 clears the cache,
including when returned by history. An observed unpublished/withdrawn response
also invalidates the affected copy. Cancellation propagates.

An offline device cannot discover a withdrawal until it contacts the server
again. The saved-copy label and expiry make that limitation explicit; this is
not a guarantee of instantaneous revocation while disconnected.

## Design and runtime review

Source: [DN App section 23](https://www.figma.com/design/NOVmvVvB7CEXXVemOy5hJa/DN-App?node-id=561-3870).
The Home chip was added in the issue's later Figma revision (`584:3986`).
The implementation reuses the existing Pretendard styles, mode-aware theme,
glass controls and background rendering. `AppScreen` gains an opt-in compact
header; the default header keeps its existing behavior. Layout follows the
repository's spacing/shape tokens, chevron-back rule, and surface separation
instead of reproducing the mock's thin horizontal dividers. No mock status-bar
artwork is rendered: the system owns it. Light-mode accent text uses the
existing accessible ink/colors.

The history selector composes the existing themed Material bottom-sheet/list
patterns. The unavailable and sharing-empty copy handles optional/missing data.
Editorial content stays in its original language; app chrome supports Korean,
English and German. No reading-duration estimate is invented from sample data.

Visual/runtime validation on Android and TestFlight is still required: open
the Home chip, switch both tabs, open an older edition, retry an error, verify
the saved-copy banner offline, and check long Korean/English content with large
text in both themes. No iOS app launch or TestFlight dispatch is performed here.
