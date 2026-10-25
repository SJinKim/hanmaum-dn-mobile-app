# Design Spec — Album 2.0: pCloud→S3 Migration + Photo Hub (IG-style Grid + Calendar) + Upload

**Date:** 2026-07-02
**Status:** Draft — awaiting review
**Repos affected:** `hanmaum-dn-server` (storage + API), `hanmaum-dn-mobile-app` (redesign + upload)
**Design references:** `.lazyweb/quick-references/album-grid-photo-upload-2026-07-02/report.html`

---

## 0. Decisions & Open Questions

Confirmed by user (2026-07-02):
- Surface: **KMP mobile app**.
- Sequence: **Album Grid first**; Calendar Story Archive is a separate later feature that reuses the photo-upload capability built here.
- Scope (revised per user 2026-07-02): **a complete transformation, not a restyle** — "it's actually completely new; the album feature should be transformed into this kind of feature." The pCloud album browser is replaced by an Instagram-style **photo hub**: one shared photo pool viewed two ways — a grid tab of named album tiles and a calendar tab of photos by date — plus member upload. The old screens are deleted, not reskinned.
- Storage: **migrate photos "and everything" from pCloud to an S3 bucket before the app work starts**. Files (not just photos) need a storage home — the S3 layout below reserves a namespace for non-photo files.

**Target design (added later on 2026-07-02):** user provided two Instagram screenshots in `designs/` — `IMG_4777.jpeg` (annotated profile photo grid) and `IMG_4780.PNG` (스토리 보관함 / story-archive calendar) — with the instruction that the screens "should be exactly like that or almost like that". This supersedes the event-card shelf concept in the first draft of §5. Reference collection updated: `.lazyweb/quick-references/album-grid-photo-upload-2026-07-02/`.

Defaults chosen while user was away — **please confirm or override**:

| # | Decision | Default chosen | Alternatives |
|---|----------|----------------|--------------|
| D1 | S3 flavor | AWS S3 in prod, **MinIO container for local dev** | AWS-only; Cloudflare R2; self-hosted MinIO |
| D2 | Upload mechanics | **Presigned PUT/GET URLs**, app talks to S3 directly | Backend multipart proxy |
| D3 | Upload permission | **All ACTIVE members** may upload to any album | Admins only; per-album flag |
| D4 | Session sequencing | Design doc first (this document), then backend migration, then mobile | Backend-first implementation now |
| D5 | Grid tab content | ✅ **CONFIRMED by user:** album tiles, **one album per 순 (ChurchGroup)**. Uploads are auto-classified: a photo uploaded by a member of 순 A lands in 순 A's album — no album picker at upload. Backend support exists: `ChurchGroup` entity (`division`, `name`) + `Member.group` FK. | — |
| D6 | Calendar tab scope | ✅ **CONFIRMED by user:** month view only, refreshed per month. Read as: one month displayed at a time, current month by default, **prev/next month navigation** to see past pictures ("if user wants a past picture one can use the calendar view"). | — |
| D7 | Upload entry | **"+" icon in the top bar** (IG create-button pattern), visible on both tabs | Icon in the tab row; FAB over grid |

D1 update: user confirmed storage **will be an S3 bucket, set up later in the backend** — design against the S3 API now (flavor/account details deferred); MinIO for local dev remains the working default.

### Open questions — ✅ all answered by user 2026-07-02

| # | Question | Answer |
|---|----------|--------|
| Q1 | Legacy pCloud albums | **Don't migrate at all.** pCloud is retired completely; the hub starts fresh. |
| Q2 | No-순 members | **Cannot upload** — they can watch and **save**. This introduces a new feature: photo save/bookmark (스크랩), like Instagram's bookmark, available to all members. |
| Q3 | Visibility | **Everyone sees all** 순 albums and the church-wide calendar. |
| Q4 | Deletion | **Uploader may delete their own photos only** (in-app). No deleting others' photos. |

D3 refined by Q2: upload = ACTIVE members **who have a 순**; everyone else (and everyone, additionally) gets view + save.

**Save/bookmark (스크랩) — new requirement from Q2.** Assumed design (flag if wrong): a bookmark toggle on each photo (in the photo viewer), and a **third icon tab** in the tab row — "저장됨" — showing the member's privately saved photos (saves are per-member, not visible to others), mirroring Instagram's saved collection.

---

## 1. Current State

### Backend (`hanmaum-dn-server`)
- `Album` entity: `name`, `pcloudCode` (unique), `displayOrder`. No photo entity — photos live only in pCloud.
- Single endpoint: `GET /api/v1/albums` → `[{publicId, name, pcloudCode, displayOrder}]`.

### Mobile (`hanmaum-dn-mobile-app`, shipped in MVP v1.0.0)
- `features/album/`: `AlbumsScreen` (2–4-col card grid) → `AlbumDetailScreen` (3–5-col tight grid) → `PhotoViewerScreen`.
- The app calls **pCloud's public API directly** (`PCloudFolderResponse`, `PCloudDownloadResponse`, `AlbumCacheRepository` for CDN-URL caching). Read-only; no upload anywhere.
- DESIGN.md violations in the current screens: default Material `TopAppBar`, no spring/stagger motion, no press feedback, bare centered-text empty/error states, plain `CircularProgressIndicator` loading.

---

## 2. Storage — S3

### Bucket layout
```
hanmaum-dn-{env}/
  albums/{albumPublicId}/{photoId}.{ext}        # originals
  albums/{albumPublicId}/thumbs/{photoId}.webp  # server-generated thumbnails (grid size)
  files/...                                     # reserved: future non-photo files ("everything")
```

- `photoId` = ULID generated by the backend at presign time (sortable by upload time).
- Thumbnails generated by the backend after upload confirmation (Java `ImageIO`/Thumbnailator, longest edge ≈ 640px, WebP). Grid loads thumbs; `PhotoViewerScreen` loads originals.
- Bucket is **private**; all access via presigned GET URLs (D2). No public ACLs.

### Local dev
`minio` service added to the backend's docker compose; backend config gets `storage.s3.endpoint / bucket / access-key / secret-key` (endpoint override makes AWS SDK v2 work against MinIO unchanged).

### Migration: none (Q1)
pCloud is retired entirely — no photo migration. The old `albums` rows/`pcloud_code` column are dropped in a schema migration; the hub launches empty and fills as 순 members upload. Old app versions' album tab stops working once the v1 endpoint changes shape — acceptable (TestFlight audience, force-update).

---

## 3. Backend API v2

**Photos become first-class** (the hub's two tabs are just two views over one photo pool). New `AlbumPhoto` entity: `album` (FK), `photoId` (ULID, unique), `originalKey`, `thumbKey`, `contentType`, `sizeBytes`, `takenAt` (EXIF capture date if present, else upload time — drives the calendar), `uploadedByMemberId`, `status` (`PENDING` → `ACTIVE`), timestamps from `BaseEntity`.

`Album` becomes 순-bound: `group: ChurchGroup?` (unique FK; one album auto-provisioned per active 순 — created lazily on first request/upload). `group == null` covers legacy event albums (Q1) and a possible common album (Q2). Keeps `name` (= 순 name for group albums), `displayOrder`, gains `coverPhotoId?` (defaults to newest photo).

| Endpoint | Purpose |
|---|---|
| `GET /albums` | Tiles for the grid tab: `{publicId, name, groupPublicId?, photoCount, coverThumbUrl}` (presigned) |
| `GET /albums/{publicId}/photos` | Paged list: `{photoId, thumbUrl, originalUrl, takenAt, uploadedBy}` — presigned GETs, ~7-day expiry |
| `POST /photos/presign` | **No album parameter — the target album is derived from the uploader's 순** (`member.group`, D5). Body: `[{fileName, contentType, sizeBytes}]` (≤ 30/request, ≤ 20 MB each; types jpeg/png/webp — **HEIC rejected, the app transcodes picker output to JPEG**, decided 2026-07-03) → `[{photoId, uploadUrl}]`; creates `PENDING` rows. No-순 behavior per Q2. |
| `POST /photos/confirm` | Body: `[photoId]` → backend verifies objects exist (HEAD), reads EXIF `takenAt`, generates thumbs, flips to `ACTIVE` |
| `GET /photos/calendar?month=YYYY-MM` | Church-wide per-day aggregation across all albums: `[{date, photoCount, coverThumbUrl}]` — feeds the calendar tab |
| `GET /photos?date=YYYY-MM-DD` | All photos of one day across albums (calendar day tap) |
| `DELETE /photos/{photoId}` | Only when `uploadedByMemberId == requester` (Q4) → removes row + S3 objects |
| `PUT /photos/{photoId}/save` / `DELETE .../save` | Toggle bookmark (스크랩); new `PhotoSave` entity (`member` + `photo`, unique pair) |
| `GET /photos/saved` | Paged list of the requester's saved photos (private, Q2) |

- Auth: existing Keycloak bearer; any ACTIVE member may presign/confirm (D3). `PENDING` rows older than 24 h are garbage-collected along with any orphaned objects.
- `GET /albums` keeps its old shape working during migration (`pcloudCode` field retained, deprecated) so v1.0.0 apps don't break; removed after the mobile release ships.

---

## 4. Mobile — Data Layer

- **Delete** the pCloud stack: `PCloudFolderResponse`, `PCloudDownloadResponse`, `AlbumDetailRepository(+Impl)`, `AlbumCacheRepository(+Impl)` and their Koin bindings. URL-resolution state in `AlbumDetailViewModel` goes away — the API returns ready URLs.
- `AlbumsRepository.getAlbums()` maps the v2 response (drops `pcloudCode`, gains `eventDate`, `photoCount`, `coverThumbUrl`).
- New `AlbumPhotosRepository`: `getPhotos(albumId, page)`, `getCalendar(month)`, `getPhotosByDate(date)`, `getSavedPhotos(page)`, `presignUploads(files)`, `uploadToS3(url, bytes, onProgress)` (plain Ktor PUT, no bearer — presigned), `confirmUploads(photoIds)`, `deletePhoto(photoId)`, `setSaved(photoId, saved)`.
- New `PhotoPicker` (`expect`/`actual`): Android → `PickMultipleVisualMedia` photo picker; iOS → `PHPickerViewController`. Returns picked images as bytes + metadata. (Alternative: `calf-file-picker` library — decide in plan phase.)
- Routes: `AlbumDetailRoute` params change from `(pcloudCode, albumName)` to `(albumPublicId, albumName)`.

## 5. Mobile — Screen Redesign (target: Instagram, per user screenshots; tokens from DESIGN.md)

Structure copies Instagram exactly (D5–D7); colors/typography/motion come from DESIGN.md (adaptive light/dark — the IG screenshots are dark mode; DN renders the same layout in its own surface tokens both modes).

**Album tab — IG-profile-style container** (target: `IMG_4777.jpeg`; ref: `instagram-profile-grid-tabs.png`)
- Top bar: screen title + **"+" upload icon** (D7) — shown only if the signed-in member has a 순 (Q2; the app needs the member's group from the profile/me API — verify it's exposed, else add it).
- Icon tab row beneath (IG-style, active tab underlined): **grid tab**, **calendar tab**, **saved tab** (bookmark icon — the member's private 스크랩 collection as a tight photo grid).
- **Grid tab:** 3-col edge-to-edge square tiles, ~2 dp gutters, no cards/padding — each tile is an **album** (cover photo, name overlaid on a bottom scrim, e.g. 소망/믿음/사랑). Tap → that album's photo grid (below). Photo count from the API may show under the name.
- Tile press scale 0.97 spring; grid stagger on entry (cap 5).

**Album photo grid (detail)** — unchanged structure from current `AlbumDetailScreen`: tight 3/4/5-col grid, 2 dp gutters, square crops, thumbs from `thumbUrl`; chevron back + album name in the top bar, "+" upload icon stays. New photos animate in with staggered spring after upload.

**Calendar tab — IG 스토리 보관함 archive calendar** (target: `IMG_4780.PNG`; DB sibling: `nytgames-archive-calendar.png`)
- Month section header ("7월 2026"), 7-col weekday row (월–일), then the day grid.
- A day **with photos** renders a circular photo thumbnail (that day's first/cover photo) with the day number overlaid in white; a day **without photos** renders just the number in `muted`.
- Tap a photo-day → that day's photos (photo grid filtered by date, or viewer if a single photo).
- One month at a time (D6): current month by default, refreshed monthly; chevron/dropdown month navigation to view past months' pictures (no infinite scroll).
- Data: **church-wide, across all albums** — one photo pool, grouped by `takenAt` (`GET /photos/calendar?month=`); the day's cover thumbnail is that day's first photo.
- Empty month: plain number grid with a small invitation line (ref: Shine/Dropbox).

**Empty album state:** invitation card with upload CTA (ref: Shine/Dropbox), not bare text.

**Upload flow** (refs: Google Maps, Instagram, Sora)
1. FAB → system multi-photo picker (cap 30/batch to match presign limit).
2. Bottom sheet (`shape_large`): "n장 업로드 중" + overall progress bar + per-item ticks + cancel. Sheet persists across the presign → PUT (client-side ~85% of the bar) → confirm sequence.
3. Success: sheet dismisses with spring; grid refreshes; failures per-item with retry ("n장 실패 · 다시 시도").
4. ViewModel-scoped uploads; leaving the screen mid-upload warns (ref: Edits).

**PhotoViewerScreen** — receives `originalUrl` directly; gains a **bookmark toggle** (save/스크랩, spring press feedback) and, on the member's own photos only, a **delete action** (confirm dialog → `DELETE /photos/{photoId}` → grid updates).

## 6. Testing

- Backend: presign/confirm service tests (MinIO testcontainer), 순-derivation test (member with/without group), own-photo-only delete authorization test, save-toggle idempotency test, orphan-GC test.
- Mobile: `AlbumsViewModelTest` (v2 mapping), new `AlbumPhotosViewModelTest` (paging, upload success / partial failure / cancel), fake `PhotoPicker`. Run `./gradlew :composeApp:allTests` + compile the iOS test target (see `tasks/lessons.md` re: test names).

## 7. Out of Scope (this feature)

- Calendar Story Archive = the **calendar tab** described in §5 (`IMG_4780.PNG` is its exact target). Per the confirmed sequencing it ships as the second feature: the Album Grid release includes the tab row but only the grid tab active; the calendar tab lands next, reusing `AlbumPhotosRepository` + picker + upload sheet.
- Photo report, comments/reactions, album creation from the app (dashboard remains the admin surface). (Own-photo delete and save/스크랩 are now **in scope** per Q2/Q4.)
- Video upload.
