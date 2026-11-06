# API response migration — mobile #316 / server #285

Goal: release consumers that accept the legacy payload and the ApiResponse envelope before changing the server. No UI, dependency, client configuration or release automation changes.

1. Add MockEngine regressions for albums, floors, rooms and church location: legacy and wrapped success, empty lists, unsuccessful status/envelope, malformed or null data, cancellation. The tests must fail against the old repositories.
2. Add a small internal core/network decoder used only by these four GETs. Reject failed HTTP/envelope responses and missing data; preserve the current JSON tolerance and domain mapping.
3. Replace the raw decoding in AlbumsRepositoryImpl, FloorPlanRepositoryImpl and ChurchLocationRepositoryImpl; keep interfaces and injected client. Rethrow coroutine cancellation.
4. Run targeted tests, full Android/common unit tests, lint, Android APK build and iOS native tests. Keep TestFlight runtime validation pending for Jin.
5. Open the mobile PR first and link it from server #285. The server migration may be reviewed now but remains a draft until this mobile version has actually been released and verified on target devices. Merging mobile alone does not fulfill the rollout prerequisite.
