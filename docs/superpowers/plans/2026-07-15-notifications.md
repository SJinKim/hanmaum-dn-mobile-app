# Notifications (Push + In-App Bell) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** FCM push + in-app bell notifications on announcement create, with two-tier seen/read state, per spec `docs/superpowers/specs/2026-07-15-notifications-design.md`.

**Architecture:** Part A adds a notifications feature to the Spring server (`../hanmaum-dn-server`): device-token registry, per-member notification rows, `/me/*` endpoints, and an async AFTER_COMMIT fan-out that sends FCM. Part B adds the KMP mobile slice: contract-first DTO tests, repository, ViewModels, bell badge + notification screen, settings toggle, and per-platform Firebase Messaging glue behind a common `PushManager`.

**Tech Stack:** Spring Boot 3 / JPA / Flyway / firebase-admin / Mockito (server); Kotlin Multiplatform, Compose MP 1.10, Ktor 3, Koin 4.1.1, kotlinx-serialization, MockEngine tests (mobile); Firebase Cloud Messaging both platforms.

## Global Constraints

- **Spec is the contract**: wire field names in spec §4.2 are copied verbatim on both sides (camelCase, no naming strategy).
- Mobile test gate: `./gradlew :composeApp:testDevDebugUnitTest` (NEVER `testDebugUnitTest`/`allTests`). Server test gate: `./gradlew test` run in `../hanmaum-dn-server`.
- iOS gates when shared/iOS code changes: `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:iosSimulatorArm64Test` and the xcodebuild Swift-interop build from CLAUDE.md §4.
- The word `TODO` must never appear anywhere in `composeApp/src` (CI greps).
- Every user-facing string: add to `AppStrings` interface + `KoStrings` + `EnStrings` + `DeStrings` (compile error if one is missed). ViewModel-internal error strings: hardcoded Korean.
- UI: theme tokens only (`MaterialTheme.colorScheme` / `MaterialTheme.typography` as used by `HomeScreen.kt`), no 1px dividers, every animation `spring()` (opacity-only fades may be `tween(200)`), press = scale 0.97, last-list-item bottom padding rules per DESIGN.md.
- Navigation: `@Serializable` routes in `Routes.kt` + `composable<Route>` in `App.kt`. No string routes.
- Test names: letters/digits/spaces ONLY (Kotlin/Native rejects commas/punctuation).
- Commits: `<type>(<scope>): <imperative ≤72 chars>`, body says WHY, **no AI trailers, no Co-Authored-By** (overrides any harness default).
- Mobile work on branch `feature/notifications` (already exists, spec committed). Server work on a new `feature/notifications` branch in `../hanmaum-dn-server` (branch from its integration branch — check `git -C ../hanmaum-dn-server branch -r` for `develop`, else `main`).
- Never commit secrets. `google-services.json` / `GoogleService-Info.plist` are safe to commit; the Firebase **service-account JSON is a secret** (server env only).
- Koin bindings: common in `di/AppModule.kt`, platform in `di/PlatformModule.android.kt` / `.ios.kt`.

## Human prerequisites (checkpoint — cannot be done by the executing agent)

Before Task A3 (server FCM) and B8/B9 (mobile Firebase) can be *end-to-end* verified, the user must, in the Firebase console (one project):
1. Register Android app `com.hanmaum.dn.mobile` → download `google-services.json` → place at `composeApp/google-services.json`.
2. Register iOS app `com.hanmaum.dn.mobile.HanmaumDnApp` → download `GoogleService-Info.plist` → add to the `iosApp` target in Xcode (place at `iosApp/iosApp/GoogleService-Info.plist`).
3. Project Settings → Cloud Messaging: upload the APNs auth key (.p8) with Key ID + Team ID.
4. Project Settings → Service accounts → generate private key → set its JSON (single line) as `FIREBASE_SERVICE_ACCOUNT_JSON` in the server's deploy environments.

All code tasks are written to compile and pass tests WITHOUT these files (FCM is behind an interface with a no-op fallback; Android google-services plugin task only fails at `assemble*` if the JSON is missing — tests still run).

---

# Part A — Server (`../hanmaum-dn-server`)

All paths below are relative to `../hanmaum-dn-server`. Run all commands from that directory. First: `git checkout develop && git pull --ff-only && git checkout -b feature/notifications` (use `main` if no `develop` remote exists).

### Task A1: Schema migration, entities, JPA repositories

**Files:**
- Create: `src/main/resources/db/migration/V20260715120000__create_notification_tables.sql`
- Create: `src/main/kotlin/com/hanmaum/dn/app/features/notifications/domain/DeviceToken.kt`
- Create: `src/main/kotlin/com/hanmaum/dn/app/features/notifications/domain/AppNotification.kt`
- Create: `src/main/kotlin/com/hanmaum/dn/app/features/notifications/repository/DeviceTokenRepository.kt`
- Create: `src/main/kotlin/com/hanmaum/dn/app/features/notifications/repository/AppNotificationRepository.kt`
- Modify: `src/main/kotlin/com/hanmaum/dn/app/features/members/domain/Member.kt` (add `pushEnabled`)

**Interfaces:**
- Consumes: `BaseEntity` (`common/jpa/BaseEntity.kt` — provides `id`, `publicId`, `createdAt`, `updatedAt`, `deletedAt`; tables MUST carry those columns), `Member` entity.
- Produces: entities `DeviceToken(member, token, platform)`, `AppNotification(member, type, title, body, referenceType, referencePublicId)` with `seenAt`/`readAt: Instant?`; enums `DevicePlatform { ANDROID, IOS }`, `NotificationType { ANNOUNCEMENT }`, `NotificationReferenceType { ANNOUNCEMENT }`; repositories with the exact finder names below.

- [ ] **Step 1: Write the migration**

Before writing, confirm the `members` PK type used by other FKs: `grep -rn "REFERENCES members" src/main/resources/db/migration/ | head -3` and match it (expected `BIGINT`).

```sql
-- V20260715120000__create_notification_tables.sql
CREATE TABLE device_tokens (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    member_id BIGINT NOT NULL REFERENCES members (id),
    token VARCHAR(512) NOT NULL UNIQUE,
    platform VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_device_tokens_member ON device_tokens (member_id);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    member_id BIGINT NOT NULL REFERENCES members (id),
    type VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    reference_type VARCHAR(32),
    reference_public_id UUID,
    seen_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_notifications_member_created ON notifications (member_id, created_at DESC);
CREATE INDEX idx_notifications_member_unseen ON notifications (member_id) WHERE seen_at IS NULL;

ALTER TABLE members ADD COLUMN push_enabled BOOLEAN NOT NULL DEFAULT TRUE;
```

- [ ] **Step 2: Write the entities**

```kotlin
// domain/DeviceToken.kt
package com.hanmaum.dn.app.features.notifications.domain

import com.hanmaum.dn.app.common.jpa.BaseEntity
import com.hanmaum.dn.app.features.members.domain.Member
import jakarta.persistence.*

enum class DevicePlatform { ANDROID, IOS }

@Entity
@Table(name = "device_tokens")
class DeviceToken(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member,
    @Column(nullable = false, unique = true, length = 512)
    var token: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var platform: DevicePlatform,
) : BaseEntity()
```

```kotlin
// domain/AppNotification.kt
package com.hanmaum.dn.app.features.notifications.domain

import com.hanmaum.dn.app.common.jpa.BaseEntity
import com.hanmaum.dn.app.features.members.domain.Member
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

enum class NotificationType { ANNOUNCEMENT }
enum class NotificationReferenceType { ANNOUNCEMENT }

@Entity
@Table(name = "notifications")
class AppNotification(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var type: NotificationType,
    @Column(nullable = false)
    var title: String,
    @Column(columnDefinition = "TEXT", nullable = false)
    var body: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", length = 32)
    var referenceType: NotificationReferenceType? = null,
    @Column(name = "reference_public_id")
    var referencePublicId: UUID? = null,
) : BaseEntity() {
    @Column(name = "seen_at")
    var seenAt: Instant? = null

    @Column(name = "read_at")
    var readAt: Instant? = null
}
```

Check `Member.kt` import path first (`features/members/domain/Member.kt`); if the entity lives elsewhere, fix imports, not the layout.

- [ ] **Step 3: Add `pushEnabled` to Member**

In `Member.kt`, next to the other simple `var` columns:

```kotlin
    @Column(name = "push_enabled", nullable = false)
    var pushEnabled: Boolean = true,
```

(If `Member`'s fields are constructor parameters — they are, per the existing file — add it as a constructor property with the default so existing call sites still compile.)

- [ ] **Step 4: Write the repositories**

```kotlin
// repository/DeviceTokenRepository.kt
package com.hanmaum.dn.app.features.notifications.repository

import com.hanmaum.dn.app.features.notifications.domain.DeviceToken
import org.springframework.data.jpa.repository.JpaRepository

interface DeviceTokenRepository : JpaRepository<DeviceToken, Long> {
    fun findByToken(token: String): DeviceToken?
    fun findAllByMemberIdIn(memberIds: Collection<Long>): List<DeviceToken>
    fun deleteAllByTokenIn(tokens: Collection<String>)
    fun deleteByTokenAndMemberId(token: String, memberId: Long)
}
```

```kotlin
// repository/AppNotificationRepository.kt
package com.hanmaum.dn.app.features.notifications.repository

import com.hanmaum.dn.app.features.notifications.domain.AppNotification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface AppNotificationRepository : JpaRepository<AppNotification, Long> {
    fun findAllByMemberIdOrderByCreatedAtDesc(memberId: Long, pageable: Pageable): Page<AppNotification>

    fun countByMemberIdAndSeenAtIsNull(memberId: Long): Long

    fun findByPublicIdAndMemberId(publicId: UUID, memberId: Long): AppNotification?

    @Modifying
    @Query("update AppNotification n set n.seenAt = :now where n.member.id = :memberId and n.seenAt is null")
    fun markAllSeen(@Param("memberId") memberId: Long, @Param("now") now: Instant): Int

    @Modifying
    @Query(
        "update AppNotification n set n.readAt = :now, " +
            "n.seenAt = coalesce(n.seenAt, :now) " +
            "where n.member.id = :memberId and n.readAt is null",
    )
    fun markAllRead(@Param("memberId") memberId: Long, @Param("now") now: Instant): Int
}
```

- [ ] **Step 5: Compile + run existing tests**

Run: `./gradlew build -x test && ./gradlew test`
Expected: BUILD SUCCESSFUL (migration is validated by Flyway on the test context if tests boot Spring; if a test uses a real schema, it now includes the new tables).

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/db/migration src/main/kotlin/com/hanmaum/dn/app/features/notifications src/main/kotlin/com/hanmaum/dn/app/features/members/domain/Member.kt
git commit -m "feat(notifications): schema, entities, repositories for device tokens and notifications"
```

### Task A2: Member helpers for notifications

**Files:**
- Modify: `src/main/kotlin/com/hanmaum/dn/app/features/members/service/MemberService.kt`
- Modify: `src/main/kotlin/com/hanmaum/dn/app/features/members/repository/MemberRepository.kt`

**Interfaces:**
- Produces: `MemberService.resolveMember(keycloakSubject: String, email: String?): Member` (public); `MemberRepository.findAllByMemberStatusAndDeletedAtIsNull(status: MemberStatus): List<Member>`.

- [ ] **Step 1: Add the ACTIVE-members finder**

In `MemberRepository.kt` add:

```kotlin
    fun findAllByMemberStatusAndDeletedAtIsNull(memberStatus: MemberStatus): List<Member>
```

(`MemberStatus` import already present in that file's package space — it lives in `common/domainvalue`.)

- [ ] **Step 2: Add the public resolve wrapper**

`MemberService` has a private/internal `resolveAndLinkMember(keycloakSubject, email, emailVerified)` (used by `getMemberProfile`). Add next to `getMemberProfile`:

```kotlin
    /** Resolve the calling member from JWT claims for the notifications feature. */
    @Transactional
    fun resolveMember(keycloakSubject: String, email: String?): Member =
        resolveAndLinkMember(keycloakSubject, email, false)
```

If `resolveAndLinkMember` has a different name/signature, adapt the delegation — do NOT duplicate its lookup logic.

- [ ] **Step 3: Compile + test + commit**

Run: `./gradlew test` → BUILD SUCCESSFUL, then:

```bash
git add src/main/kotlin/com/hanmaum/dn/app/features/members
git commit -m "feat(members): resolveMember wrapper and ACTIVE members finder for notifications"
```

### Task A3: Push infrastructure (firebase-admin, PushSender, async config)

**Files:**
- Modify: `build.gradle.kts` (dependency)
- Create: `src/main/kotlin/com/hanmaum/dn/app/features/notifications/service/PushSender.kt`
- Create: `src/main/kotlin/com/hanmaum/dn/app/features/notifications/service/FcmPushSender.kt`
- Create: `src/main/kotlin/com/hanmaum/dn/app/config/FirebaseConfig.kt` (put beside existing `@Configuration` classes — `grep -rln "@Configuration" src/main/kotlin | head` and match the package)
- Create: `src/main/kotlin/com/hanmaum/dn/app/config/AsyncConfig.kt` (same package as FirebaseConfig)

**Interfaces:**
- Produces:
  ```kotlin
  interface PushSender {
      /** Returns tokens FCM reported as dead (UNREGISTERED/INVALID_ARGUMENT). */
      fun send(tokens: List<String>, title: String, body: String, data: Map<String, String>, badge: Int?): List<String>
  }
  ```
  Bean of type `PushSender` always exists (no-op when `FIREBASE_SERVICE_ACCOUNT_JSON` is absent). `@EnableAsync` active.

- [ ] **Step 1: Add the dependency**

In `build.gradle.kts` dependencies block:

```kotlin
    implementation("com.google.firebase:firebase-admin:9.4.3")
```

(If 9.4.3 doesn't resolve, use the latest 9.x from Maven Central.)

- [ ] **Step 2: PushSender interface**

```kotlin
// service/PushSender.kt
package com.hanmaum.dn.app.features.notifications.service

interface PushSender {
    /**
     * Sends one push to every token. Returns the subset of tokens that FCM
     * reported as permanently invalid (to be deleted by the caller).
     * Must never throw — log and return emptyList on transport failure.
     */
    fun send(tokens: List<String>, title: String, body: String, data: Map<String, String>, badge: Int?): List<String>
}
```

- [ ] **Step 3: FCM implementation**

```kotlin
// service/FcmPushSender.kt
package com.hanmaum.dn.app.features.notifications.service

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidNotification
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory

class FcmPushSender(private val messaging: FirebaseMessaging) : PushSender {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun send(
        tokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String>,
        badge: Int?,
    ): List<String> {
        if (tokens.isEmpty()) return emptyList()
        return try {
            val message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .putAllData(data)
                .setAndroidConfig(
                    AndroidConfig.builder()
                        .setNotification(AndroidNotification.builder().setChannelId("announcements").build())
                        .build(),
                )
                .setApnsConfig(
                    ApnsConfig.builder()
                        .setAps(
                            Aps.builder()
                                .setSound("default")
                                .apply { badge?.let { setBadge(it) } }
                                .build(),
                        )
                        .build(),
                )
                .build()
            val response = messaging.sendEachForMulticast(message)
            response.responses.mapIndexedNotNull { i, r ->
                val code = r.exception?.messagingErrorCode
                if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) tokens[i] else null
            }
        } catch (e: Exception) {
            log.warn("FCM multicast send failed for {} tokens", tokens.size, e)
            emptyList()
        }
    }
}
```

- [ ] **Step 4: Firebase + async configuration**

```kotlin
// config/FirebaseConfig.kt  (adjust package to match existing @Configuration classes)
package com.hanmaum.dn.app.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.hanmaum.dn.app.features.notifications.service.FcmPushSender
import com.hanmaum.dn.app.features.notifications.service.PushSender
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FirebaseConfig {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun pushSender(@Value("\${FIREBASE_SERVICE_ACCOUNT_JSON:}") serviceAccountJson: String): PushSender {
        if (serviceAccountJson.isBlank()) {
            log.warn("FIREBASE_SERVICE_ACCOUNT_JSON not set - push sending disabled (rows are still written)")
            return object : PushSender {
                override fun send(tokens: List<String>, title: String, body: String, data: Map<String, String>, badge: Int?): List<String> = emptyList()
            }
        }
        val app = if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(
                FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccountJson.byteInputStream()))
                    .build(),
            )
        } else {
            FirebaseApp.getInstance()
        }
        return FcmPushSender(FirebaseMessaging.getInstance(app))
    }
}
```

```kotlin
// config/AsyncConfig.kt
package com.hanmaum.dn.app.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync

@Configuration
@EnableAsync
class AsyncConfig
```

Before creating AsyncConfig, check `grep -rn "@EnableAsync" src/main/kotlin` — if it already exists anywhere, skip the file.

- [ ] **Step 5: Compile, test, commit**

Run: `./gradlew test` → BUILD SUCCESSFUL.

```bash
git add build.gradle.kts src/main/kotlin/com/hanmaum/dn/app/features/notifications/service src/main/kotlin/com/hanmaum/dn/app/config
git commit -m "feat(notifications): firebase-admin PushSender with no-op fallback and async config"
```

### Task A4: NotificationService with tests (TDD)

**Files:**
- Create: `src/test/kotlin/com/hanmaum/dn/app/features/notifications/service/NotificationServiceTest.kt`
- Create: `src/main/kotlin/com/hanmaum/dn/app/features/notifications/service/NotificationService.kt`

**Interfaces:**
- Consumes: repositories from A1, `MemberService.resolveMember` from A2.
- Produces (exact signatures — controller in A5 depends on them):
  ```kotlin
  class NotificationService(
      memberService: MemberService,
      notificationRepository: AppNotificationRepository,
      deviceTokenRepository: DeviceTokenRepository,
  ) {
      fun getNotifications(keycloakSubject: String, email: String?, page: Int, size: Int): Page<AppNotification>
      fun getUnseenCount(keycloakSubject: String, email: String?): Long
      fun markAllSeen(keycloakSubject: String, email: String?)
      fun markRead(keycloakSubject: String, email: String?, publicId: UUID)   // throws NoSuchElementException if not caller's
      fun markAllRead(keycloakSubject: String, email: String?)
      fun registerDeviceToken(keycloakSubject: String, email: String?, token: String, platform: DevicePlatform)
      fun deleteDeviceToken(keycloakSubject: String, email: String?, token: String)
      fun getPushEnabled(keycloakSubject: String, email: String?): Boolean
      fun setPushEnabled(keycloakSubject: String, email: String?, enabled: Boolean)
  }
  ```

- [ ] **Step 1: Write the failing tests** (mirror `AnnouncementServiceTest` style: `@ExtendWith(MockitoExtension::class)`, `@Mock` repos, `@InjectMocks` service; use `org.mockito.kotlin.any/eq/verify`)

```kotlin
package com.hanmaum.dn.app.features.notifications.service

import com.hanmaum.dn.app.common.domainvalue.MemberStatus
import com.hanmaum.dn.app.features.members.domain.Member
import com.hanmaum.dn.app.features.members.service.MemberService
import com.hanmaum.dn.app.features.notifications.domain.AppNotification
import com.hanmaum.dn.app.features.notifications.domain.DevicePlatform
import com.hanmaum.dn.app.features.notifications.domain.DeviceToken
import com.hanmaum.dn.app.features.notifications.domain.NotificationType
import com.hanmaum.dn.app.features.notifications.repository.AppNotificationRepository
import com.hanmaum.dn.app.features.notifications.repository.DeviceTokenRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class NotificationServiceTest {
    @Mock private lateinit var memberService: MemberService
    @Mock private lateinit var notificationRepository: AppNotificationRepository
    @Mock private lateinit var deviceTokenRepository: DeviceTokenRepository
    @InjectMocks private lateinit var service: NotificationService

    private fun member(id: Long = 1L): Member =
        Member(lastName = "김", firstName = "성진").also {
            it.id = id
            it.memberStatus = MemberStatus.ACTIVE
        }
    // NOTE: Member's constructor takes many defaulted params; if lastName/firstName
    // are not enough to construct, use the minimal constructor the codebase allows.

    @Test
    fun `markRead sets readAt and seenAt on own notification`() {
        val m = member()
        `when`(memberService.resolveMember("sub", null)).thenReturn(m)
        val n = AppNotification(m, NotificationType.ANNOUNCEMENT, "t", "b")
        `when`(notificationRepository.findByPublicIdAndMemberId(n.publicId, 1L)).thenReturn(n)

        service.markRead("sub", null, n.publicId)

        org.junit.jupiter.api.Assertions.assertNotNull(n.readAt)
        org.junit.jupiter.api.Assertions.assertNotNull(n.seenAt)
    }

    @Test
    fun `markRead throws for another members notification`() {
        `when`(memberService.resolveMember("sub", null)).thenReturn(member())
        val foreignId = UUID.randomUUID()
        `when`(notificationRepository.findByPublicIdAndMemberId(foreignId, 1L)).thenReturn(null)

        assertThrows(NoSuchElementException::class.java) { service.markRead("sub", null, foreignId) }
    }

    @Test
    fun `markRead does not overwrite existing seenAt`() {
        val m = member()
        `when`(memberService.resolveMember("sub", null)).thenReturn(m)
        val n = AppNotification(m, NotificationType.ANNOUNCEMENT, "t", "b")
        val seen = Instant.parse("2026-07-01T00:00:00Z")
        n.seenAt = seen
        `when`(notificationRepository.findByPublicIdAndMemberId(n.publicId, 1L)).thenReturn(n)

        service.markRead("sub", null, n.publicId)

        assertEquals(seen, n.seenAt)
    }

    @Test
    fun `registerDeviceToken reassigns existing token to caller`() {
        val caller = member(1L)
        val other = member(2L)
        `when`(memberService.resolveMember("sub", null)).thenReturn(caller)
        val existing = DeviceToken(other, "tok", DevicePlatform.ANDROID)
        `when`(deviceTokenRepository.findByToken("tok")).thenReturn(existing)

        service.registerDeviceToken("sub", null, "tok", DevicePlatform.IOS)

        assertEquals(caller, existing.member)
        assertEquals(DevicePlatform.IOS, existing.platform)
        verify(deviceTokenRepository, never()).save(any())
    }

    @Test
    fun `registerDeviceToken saves new token`() {
        `when`(memberService.resolveMember("sub", null)).thenReturn(member())
        `when`(deviceTokenRepository.findByToken("tok")).thenReturn(null)

        service.registerDeviceToken("sub", null, "tok", DevicePlatform.ANDROID)

        val captor = argumentCaptor<DeviceToken>()
        verify(deviceTokenRepository).save(captor.capture())
        assertEquals("tok", captor.firstValue.token)
    }

    @Test
    fun `setPushEnabled flips the member flag`() {
        val m = member()
        `when`(memberService.resolveMember("sub", null)).thenReturn(m)

        service.setPushEnabled("sub", null, false)

        assertEquals(false, m.pushEnabled)
    }

    @Test
    fun `markAllSeen delegates to bulk update`() {
        `when`(memberService.resolveMember("sub", null)).thenReturn(member())

        service.markAllSeen("sub", null)

        verify(notificationRepository).markAllSeen(eq(1L), any())
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew test --tests "com.hanmaum.dn.app.features.notifications.service.NotificationServiceTest"`
Expected: COMPILE FAILURE (`NotificationService` unresolved).

- [ ] **Step 3: Implement**

```kotlin
// service/NotificationService.kt
package com.hanmaum.dn.app.features.notifications.service

import com.hanmaum.dn.app.features.members.domain.Member
import com.hanmaum.dn.app.features.members.service.MemberService
import com.hanmaum.dn.app.features.notifications.domain.AppNotification
import com.hanmaum.dn.app.features.notifications.domain.DevicePlatform
import com.hanmaum.dn.app.features.notifications.domain.DeviceToken
import com.hanmaum.dn.app.features.notifications.repository.AppNotificationRepository
import com.hanmaum.dn.app.features.notifications.repository.DeviceTokenRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional(readOnly = true)
class NotificationService(
    private val memberService: MemberService,
    private val notificationRepository: AppNotificationRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
) {
    private fun caller(keycloakSubject: String, email: String?): Member =
        memberService.resolveMember(keycloakSubject, email)

    fun getNotifications(keycloakSubject: String, email: String?, page: Int, size: Int): Page<AppNotification> {
        val member = caller(keycloakSubject, email)
        return notificationRepository.findAllByMemberIdOrderByCreatedAtDesc(
            member.id!!,
            PageRequest.of(page, size.coerceIn(1, 50)),
        )
    }

    fun getUnseenCount(keycloakSubject: String, email: String?): Long =
        notificationRepository.countByMemberIdAndSeenAtIsNull(caller(keycloakSubject, email).id!!)

    @Transactional
    fun markAllSeen(keycloakSubject: String, email: String?) {
        notificationRepository.markAllSeen(caller(keycloakSubject, email).id!!, Instant.now())
    }

    @Transactional
    fun markRead(keycloakSubject: String, email: String?, publicId: UUID) {
        val member = caller(keycloakSubject, email)
        val notification = notificationRepository.findByPublicIdAndMemberId(publicId, member.id!!)
            ?: throw NoSuchElementException("notification not found")
        val now = Instant.now()
        notification.readAt = notification.readAt ?: now
        notification.seenAt = notification.seenAt ?: now
    }

    @Transactional
    fun markAllRead(keycloakSubject: String, email: String?) {
        notificationRepository.markAllRead(caller(keycloakSubject, email).id!!, Instant.now())
    }

    @Transactional
    fun registerDeviceToken(keycloakSubject: String, email: String?, token: String, platform: DevicePlatform) {
        val member = caller(keycloakSubject, email)
        val existing = deviceTokenRepository.findByToken(token)
        if (existing != null) {
            existing.member = member
            existing.platform = platform
        } else {
            deviceTokenRepository.save(DeviceToken(member, token, platform))
        }
    }

    @Transactional
    fun deleteDeviceToken(keycloakSubject: String, email: String?, token: String) {
        val member = caller(keycloakSubject, email)
        deviceTokenRepository.deleteByTokenAndMemberId(token, member.id!!)
    }

    fun getPushEnabled(keycloakSubject: String, email: String?): Boolean =
        caller(keycloakSubject, email).pushEnabled

    @Transactional
    fun setPushEnabled(keycloakSubject: String, email: String?, enabled: Boolean) {
        caller(keycloakSubject, email).pushEnabled = enabled
    }
}
```

Check how `NoSuchElementException` maps to HTTP in this codebase (`grep -rn "NoSuchElementException\|ExceptionHandler" src/main/kotlin/com/hanmaum/dn/app/common | head`); if there's a global handler mapping a different exception type to 404, use that type instead.

- [ ] **Step 4: Run tests** → all NotificationServiceTest green, whole `./gradlew test` green.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/hanmaum/dn/app/features/notifications src/test/kotlin/com/hanmaum/dn/app/features/notifications
git commit -m "feat(notifications): NotificationService with seen/read state and token registry"
```

### Task A5: DTOs, mappers, controller

**Files:**
- Create: `src/main/kotlin/com/hanmaum/dn/app/features/notifications/api/v1/dto/NotificationDtos.kt`
- Create: `src/main/kotlin/com/hanmaum/dn/app/features/notifications/api/NotificationMappers.kt`
- Create: `src/main/kotlin/com/hanmaum/dn/app/features/notifications/api/v1/NotificationController.kt`

**Interfaces:**
- Consumes: `NotificationService` (A4 signatures).
- Produces the WIRE CONTRACT — these JSON shapes are what mobile Task B2 tests against. Field names verbatim from spec §4.2.

- [ ] **Step 1: DTOs**

```kotlin
// api/v1/dto/NotificationDtos.kt
package com.hanmaum.dn.app.features.notifications.api.v1.dto

import com.hanmaum.dn.app.features.notifications.domain.DevicePlatform
import java.time.Instant
import java.util.UUID

data class NotificationResponse(
    val publicId: UUID,
    val type: String,
    val title: String,
    val body: String,
    val referenceType: String?,
    val referencePublicId: UUID?,
    val createdAt: Instant,
    val seenAt: Instant?,
    val readAt: Instant?,
)

data class NotificationPageResponse(
    val items: List<NotificationResponse>,
    val page: Int,
    val hasNext: Boolean,
)

data class UnseenCountResponse(val count: Long)

data class RegisterDeviceTokenRequest(val token: String, val platform: DevicePlatform)

data class NotificationSettingsDto(val pushEnabled: Boolean)
```

- [ ] **Step 2: Mapper**

```kotlin
// api/NotificationMappers.kt
package com.hanmaum.dn.app.features.notifications.api

import com.hanmaum.dn.app.features.notifications.api.v1.dto.NotificationResponse
import com.hanmaum.dn.app.features.notifications.domain.AppNotification

fun AppNotification.toDto(): NotificationResponse = NotificationResponse(
    publicId = publicId,
    type = type.name,
    title = title,
    body = body,
    referenceType = referenceType?.name,
    referencePublicId = referencePublicId,
    createdAt = createdAt!!,
    seenAt = seenAt,
    readAt = readAt,
)
```

- [ ] **Step 3: Controller** (auth pattern copied from `MemberController` `/members/me`: `@AuthenticationPrincipal jwt: Jwt`, subject + email claim)

```kotlin
// api/v1/NotificationController.kt
package com.hanmaum.dn.app.features.notifications.api.v1

import com.hanmaum.dn.app.common.dto.ApiResponse
import com.hanmaum.dn.app.features.notifications.api.toDto
import com.hanmaum.dn.app.features.notifications.api.v1.dto.NotificationPageResponse
import com.hanmaum.dn.app.features.notifications.api.v1.dto.NotificationSettingsDto
import com.hanmaum.dn.app.features.notifications.api.v1.dto.RegisterDeviceTokenRequest
import com.hanmaum.dn.app.features.notifications.api.v1.dto.UnseenCountResponse
import com.hanmaum.dn.app.features.notifications.service.NotificationService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/me")
class NotificationController(
    private val notificationService: NotificationService,
) {
    private val Jwt.email: String? get() = getClaimAsString("email")

    @GetMapping("/notifications")
    fun getNotifications(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<NotificationPageResponse>> {
        val result = notificationService.getNotifications(jwt.subject, jwt.email, page, size)
        return ResponseEntity.ok(
            ApiResponse.success(
                data = NotificationPageResponse(
                    items = result.content.map { it.toDto() },
                    page = result.number,
                    hasNext = result.hasNext(),
                ),
            ),
        )
    }

    @GetMapping("/notifications/unseen-count")
    fun getUnseenCount(@AuthenticationPrincipal jwt: Jwt): ResponseEntity<ApiResponse<UnseenCountResponse>> =
        ResponseEntity.ok(ApiResponse.success(data = UnseenCountResponse(notificationService.getUnseenCount(jwt.subject, jwt.email))))

    @PostMapping("/notifications/mark-seen")
    fun markAllSeen(@AuthenticationPrincipal jwt: Jwt): ResponseEntity<ApiResponse<Unit>> {
        notificationService.markAllSeen(jwt.subject, jwt.email)
        return ResponseEntity.ok(ApiResponse.success())
    }

    @PostMapping("/notifications/{publicId}/read")
    fun markRead(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable publicId: UUID,
    ): ResponseEntity<ApiResponse<Unit>> {
        notificationService.markRead(jwt.subject, jwt.email, publicId)
        return ResponseEntity.ok(ApiResponse.success())
    }

    @PostMapping("/notifications/read-all")
    fun markAllRead(@AuthenticationPrincipal jwt: Jwt): ResponseEntity<ApiResponse<Unit>> {
        notificationService.markAllRead(jwt.subject, jwt.email)
        return ResponseEntity.ok(ApiResponse.success())
    }

    @PutMapping("/device-tokens")
    fun registerDeviceToken(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: RegisterDeviceTokenRequest,
    ): ResponseEntity<ApiResponse<Unit>> {
        notificationService.registerDeviceToken(jwt.subject, jwt.email, request.token, request.platform)
        return ResponseEntity.ok(ApiResponse.success())
    }

    @DeleteMapping("/device-tokens/{token}")
    fun deleteDeviceToken(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable token: String,
    ): ResponseEntity<ApiResponse<Unit>> {
        notificationService.deleteDeviceToken(jwt.subject, jwt.email, token)
        return ResponseEntity.ok(ApiResponse.success())
    }

    @GetMapping("/notification-settings")
    fun getSettings(@AuthenticationPrincipal jwt: Jwt): ResponseEntity<ApiResponse<NotificationSettingsDto>> =
        ResponseEntity.ok(ApiResponse.success(data = NotificationSettingsDto(notificationService.getPushEnabled(jwt.subject, jwt.email))))

    @PutMapping("/notification-settings")
    fun updateSettings(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: NotificationSettingsDto,
    ): ResponseEntity<ApiResponse<Unit>> {
        notificationService.setPushEnabled(jwt.subject, jwt.email, request.pushEnabled)
        return ResponseEntity.ok(ApiResponse.success())
    }
}
```

Verify security config permits authenticated non-admin access to `/me/**` (`grep -rn "requestMatchers\|authorizeHttpRequests" src/main/kotlin | head`); if routes are allow-listed individually, add `/me/**` as authenticated.

- [ ] **Step 4: Controller tests — follow repo precedent**

Check for an existing controller-test idiom: `find src/test -name "*ControllerTest*" | head`. If one exists (e.g. `@WebMvcTest` with a JWT/security test fixture), add `NotificationControllerTest` in that style covering: `GET /me/notifications` happy path, `POST /me/notifications/{id}/read` for a foreign notification → 404, `PUT /me/device-tokens` request-body binding. If the codebase has NO controller tests, the A4 service tests are the accepted gate — do not invent a new test infrastructure for this PR; note it in the PR body instead.

- [ ] **Step 5: Compile + full test run** → `./gradlew test` green.

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/hanmaum/dn/app/features/notifications/api src/test/kotlin/com/hanmaum/dn/app/features/notifications
git commit -m "feat(notifications): /me notification, device-token and settings endpoints"
```

### Task A6: Announcement event + fan-out listener (TDD)

**Files:**
- Modify: `src/main/kotlin/com/hanmaum/dn/app/features/announcements/service/AnnouncementService.kt`
- Create: `src/main/kotlin/com/hanmaum/dn/app/features/announcements/service/AnnouncementCreatedEvent.kt`
- Create: `src/main/kotlin/com/hanmaum/dn/app/features/notifications/service/NotificationFanoutListener.kt`
- Create: `src/test/kotlin/com/hanmaum/dn/app/features/notifications/service/NotificationFanoutListenerTest.kt`
- Modify: `src/test/kotlin/com/hanmaum/dn/app/features/announcements/service/AnnouncementServiceTest.kt` (constructor gains publisher mock)

**Interfaces:**
- Consumes: `PushSender` (A3), repositories (A1), `MemberRepository.findAllByMemberStatusAndDeletedAtIsNull` (A2).
- Produces: `AnnouncementCreatedEvent(announcementPublicId: UUID, announcementTitle: String)`; push data keys exactly: `type`, `referenceType`, `referencePublicId`, `notificationPublicId` (mobile B7 parses these).

- [ ] **Step 1: Event + publish on create**

```kotlin
// features/announcements/service/AnnouncementCreatedEvent.kt
package com.hanmaum.dn.app.features.announcements.service

import java.util.UUID

data class AnnouncementCreatedEvent(
    val announcementPublicId: UUID,
    val announcementTitle: String,
)
```

In `AnnouncementService`: inject `private val eventPublisher: org.springframework.context.ApplicationEventPublisher` in the constructor and change `createAnnouncement`:

```kotlin
    @Transactional
    fun createAnnouncement(req: CreateAnnouncementRequest): Announcement {
        val saved = announcementRepository.save(req.toEntity())
        if (saved.startAt <= OffsetDateTime.now()) {
            eventPublisher.publishEvent(AnnouncementCreatedEvent(saved.publicId, saved.title))
        }
        return saved
    }
```

Update `AnnouncementServiceTest` with a `@Mock` `ApplicationEventPublisher` and add two tests: event published when `startAt` in the past, NOT published when in the future (`verify(eventPublisher, never()).publishEvent(any<AnnouncementCreatedEvent>())`).

- [ ] **Step 2: Write the failing listener test**

```kotlin
package com.hanmaum.dn.app.features.notifications.service

import com.hanmaum.dn.app.common.domainvalue.MemberStatus
import com.hanmaum.dn.app.features.announcements.service.AnnouncementCreatedEvent
import com.hanmaum.dn.app.features.members.domain.Member
import com.hanmaum.dn.app.features.members.repository.MemberRepository
import com.hanmaum.dn.app.features.notifications.domain.AppNotification
import com.hanmaum.dn.app.features.notifications.domain.DevicePlatform
import com.hanmaum.dn.app.features.notifications.domain.DeviceToken
import com.hanmaum.dn.app.features.notifications.repository.AppNotificationRepository
import com.hanmaum.dn.app.features.notifications.repository.DeviceTokenRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class NotificationFanoutListenerTest {
    @Mock private lateinit var memberRepository: MemberRepository
    @Mock private lateinit var notificationRepository: AppNotificationRepository
    @Mock private lateinit var deviceTokenRepository: DeviceTokenRepository
    @Mock private lateinit var pushSender: PushSender
    @InjectMocks private lateinit var listener: NotificationFanoutListener

    private fun member(id: Long, push: Boolean = true): Member =
        Member(lastName = "김", firstName = "m$id").also {
            it.id = id
            it.memberStatus = MemberStatus.ACTIVE
            it.pushEnabled = push
        }

    @Test
    fun `writes one row per active member and pushes only to push enabled members`() {
        val withPush = member(1L, push = true)
        val noPush = member(2L, push = false)
        `when`(memberRepository.findAllByMemberStatusAndDeletedAtIsNull(MemberStatus.ACTIVE))
            .thenReturn(listOf(withPush, noPush))
        `when`(notificationRepository.saveAll(any<List<AppNotification>>())).thenAnswer { it.arguments[0] }
        `when`(deviceTokenRepository.findAllByMemberIdIn(eq(listOf(1L))))
            .thenReturn(listOf(DeviceToken(withPush, "tok1", DevicePlatform.ANDROID)))
        `when`(notificationRepository.countByMemberIdAndSeenAtIsNull(1L)).thenReturn(3L)
        `when`(pushSender.send(any(), any(), any(), any(), anyOrNull())).thenReturn(emptyList())

        listener.onAnnouncementCreated(AnnouncementCreatedEvent(UUID.randomUUID(), "여름 수련회"))

        val rows = argumentCaptor<List<AppNotification>>()
        verify(notificationRepository).saveAll(rows.capture())
        assertEquals(2, rows.firstValue.size)
        assertEquals("새로운 소식이 있습니다!", rows.firstValue[0].title)
        assertEquals("여름 수련회", rows.firstValue[0].body)
        verify(pushSender).send(eq(listOf("tok1")), any(), any(), any(), eq(3))
    }

    @Test
    fun `deletes tokens reported dead by fcm`() {
        val m = member(1L)
        `when`(memberRepository.findAllByMemberStatusAndDeletedAtIsNull(MemberStatus.ACTIVE)).thenReturn(listOf(m))
        `when`(notificationRepository.saveAll(any<List<AppNotification>>())).thenAnswer { it.arguments[0] }
        `when`(deviceTokenRepository.findAllByMemberIdIn(eq(listOf(1L))))
            .thenReturn(listOf(DeviceToken(m, "dead", DevicePlatform.IOS)))
        `when`(notificationRepository.countByMemberIdAndSeenAtIsNull(1L)).thenReturn(1L)
        `when`(pushSender.send(any(), any(), any(), any(), anyOrNull())).thenReturn(listOf("dead"))

        listener.onAnnouncementCreated(AnnouncementCreatedEvent(UUID.randomUUID(), "t"))

        verify(deviceTokenRepository).deleteAllByTokenIn(eq(listOf("dead")))
    }
}
```

- [ ] **Step 3: Run to verify failure** → compile error (`NotificationFanoutListener` unresolved).

- [ ] **Step 4: Implement the listener**

```kotlin
// service/NotificationFanoutListener.kt
package com.hanmaum.dn.app.features.notifications.service

import com.hanmaum.dn.app.common.domainvalue.MemberStatus
import com.hanmaum.dn.app.features.announcements.service.AnnouncementCreatedEvent
import com.hanmaum.dn.app.features.members.repository.MemberRepository
import com.hanmaum.dn.app.features.notifications.domain.AppNotification
import com.hanmaum.dn.app.features.notifications.domain.NotificationReferenceType
import com.hanmaum.dn.app.features.notifications.domain.NotificationType
import com.hanmaum.dn.app.features.notifications.repository.AppNotificationRepository
import com.hanmaum.dn.app.features.notifications.repository.DeviceTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private const val PUSH_TITLE = "새로운 소식이 있습니다!"

@Component
class NotificationFanoutListener(
    private val memberRepository: MemberRepository,
    private val notificationRepository: AppNotificationRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val pushSender: PushSender,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onAnnouncementCreated(event: AnnouncementCreatedEvent) {
        try {
            val members = memberRepository.findAllByMemberStatusAndDeletedAtIsNull(MemberStatus.ACTIVE)
            if (members.isEmpty()) return

            val rows = members.map { member ->
                AppNotification(
                    member = member,
                    type = NotificationType.ANNOUNCEMENT,
                    title = PUSH_TITLE,
                    body = event.announcementTitle,
                    referenceType = NotificationReferenceType.ANNOUNCEMENT,
                    referencePublicId = event.announcementPublicId,
                )
            }
            val saved = notificationRepository.saveAll(rows)
            val notificationIdByMember = saved.associate { it.member.id!! to it.publicId }

            val pushMembers = members.filter { it.pushEnabled }
            val tokensByMember = deviceTokenRepository
                .findAllByMemberIdIn(pushMembers.map { it.id!! })
                .groupBy { it.member.id!! }

            val deadTokens = mutableListOf<String>()
            for (member in pushMembers) {
                val tokens = tokensByMember[member.id]?.map { it.token } ?: continue
                val badge = notificationRepository.countByMemberIdAndSeenAtIsNull(member.id!!).toInt()
                val data = mapOf(
                    "type" to NotificationType.ANNOUNCEMENT.name,
                    "referenceType" to NotificationReferenceType.ANNOUNCEMENT.name,
                    "referencePublicId" to event.announcementPublicId.toString(),
                    "notificationPublicId" to notificationIdByMember[member.id]!!.toString(),
                )
                deadTokens += pushSender.send(tokens, PUSH_TITLE, event.announcementTitle, data, badge)
            }
            if (deadTokens.isNotEmpty()) deviceTokenRepository.deleteAllByTokenIn(deadTokens)
        } catch (e: Exception) {
            log.error("notification fan-out failed for announcement {}", event.announcementPublicId, e)
        }
    }
}
```

- [ ] **Step 5: Run all tests** → `./gradlew test` green (including updated `AnnouncementServiceTest`).

- [ ] **Step 6: Commit + push + PR**

```bash
git add -A src/main src/test
git commit -m "feat(notifications): announcement fan-out with per-member rows and FCM push"
git push -u origin feature/notifications
```

Open a PR in the server repo per its CONTRIBUTING conventions. Part A is complete and independently shippable (push silently disabled until `FIREBASE_SERVICE_ACCOUNT_JSON` is set).

---

# Part B — Mobile (this repo, branch `feature/notifications`)

Base package `com.hanmaum.dn.mobile`; `$M` below = `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile`. Mobile tasks are independent of Part A's deployment — MockEngine tests encode the contract.

### Task B1: Strings (KO/EN/DE)

**Files:**
- Modify: `$M/core/i18n/AppStrings.kt` + the three implementations (find them: `grep -rln "KoStrings\|EnStrings\|DeStrings" $M/core/i18n/`)

**Interfaces:**
- Produces string properties used by B4–B6 screens. Names are exact.

- [ ] **Step 1: Add to the `AppStrings` interface** (new section comment `// Notifications`):

```kotlin
    // Notifications
    val notificationsTitle: String
    val notificationsEmpty: String
    val notificationsReadAll: String
    val notificationsToday: String
    val notificationsYesterday: String
    val notificationsEarlier: String
    val notificationsError: String
    val notificationTimeJustNow: String
    fun notificationTimeMinutesAgo(minutes: Int): String
    fun notificationTimeHoursAgo(hours: Int): String
    fun notificationTimeDaysAgo(days: Int): String
    val pushPrimingTitle: String
    val pushPrimingBody: String
    val pushPrimingEnable: String
    val settingsPushToggle: String
    val settingsPushPermissionHint: String
```

(Interface `fun` members are fine even if the file currently only has `val`s — each language object implements them.)

- [ ] **Step 2: Implement in all three languages**

| property | Ko | En | De |
|---|---|---|---|
| notificationsTitle | 알림 | Notifications | Mitteilungen |
| notificationsEmpty | 알림이 오면 여기에 표시됩니다 | You'll see notifications here when they arrive | Neue Mitteilungen erscheinen hier |
| notificationsReadAll | 모두 읽음 | Mark all read | Alle gelesen |
| notificationsToday | 오늘 | Today | Heute |
| notificationsYesterday | 어제 | Yesterday | Gestern |
| notificationsEarlier | 이전 | Earlier | Früher |
| notificationsError | 알림을 불러오지 못했습니다 | Couldn't load notifications | Mitteilungen konnten nicht geladen werden |
| notificationTimeJustNow | 방금 전 | just now | gerade eben |
| notificationTimeMinutesAgo(m) | "${m}분 전" | "${m}m ago" | "vor ${m} Min." |
| notificationTimeHoursAgo(h) | "${h}시간 전" | "${h}h ago" | "vor ${h} Std." |
| notificationTimeDaysAgo(d) | "${d}일 전" | "${d}d ago" | "vor ${d} Tagen" |
| pushPrimingTitle | 새 소식을 놓치지 마세요 | Don't miss new announcements | Verpassen Sie keine Neuigkeiten |
| pushPrimingBody | 새로운 공지가 올라오면 알려드릴게요 | We'll notify you when something new is posted | Wir benachrichtigen Sie bei neuen Ankündigungen |
| pushPrimingEnable | 알림 켜기 | Turn on notifications | Mitteilungen aktivieren |
| settingsPushToggle | 푸시 알림 | Push notifications | Push-Mitteilungen |
| settingsPushPermissionHint | 기기 설정에서 알림을 허용해주세요 | Allow notifications in system settings | Erlauben Sie Mitteilungen in den Systemeinstellungen |

The priming card's "later" button reuses the existing `laterButton` string.

- [ ] **Step 3: Compile + commit**

Run: `./gradlew :composeApp:testDevDebugUnitTest` → BUILD SUCCESSFUL (a missing language impl is a compile error).

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/i18n
git commit -m "feat(notification): notification strings in ko en de"
```

### Task B2: Domain, DTOs, repository — contract-first (TDD)

**Files:**
- Create: `$M/features/notification/domain/model/Notification.kt`
- Create: `$M/features/notification/domain/repository/NotificationRepository.kt`
- Create: `$M/features/notification/data/model/NotificationDtos.kt`
- Create: `$M/features/notification/data/repository/NotificationRepositoryImpl.kt`
- Test: `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/notification/data/repository/NotificationRepositoryImplTest.kt`
- Modify: `$M/di/AppModule.kt`

**Interfaces:**
- Consumes: injected shared `HttpClient`, `core/domain/model/ApiResponse`.
- Produces (used by B3/B4/B6/B10):
  ```kotlin
  enum class NotificationType { ANNOUNCEMENT, EVENT_REMINDER, UNKNOWN }
  data class NotificationReference(val type: NotificationType, val publicId: String)
  data class Notification(publicId, type, title, body, reference: NotificationReference?, createdAt: Instant, isSeen: Boolean, isRead: Boolean)
  data class NotificationPage(val items: List<Notification>, val hasNext: Boolean)
  interface NotificationRepository { /* 9 methods, all Result<...>, exact names below */ }
  ```

- [ ] **Step 1: Domain models**

```kotlin
// domain/model/Notification.kt
package com.hanmaum.dn.mobile.features.notification.domain.model

import kotlinx.datetime.Instant

enum class NotificationType { ANNOUNCEMENT, EVENT_REMINDER, UNKNOWN }

data class NotificationReference(
    val type: NotificationType,
    val publicId: String,
)

data class Notification(
    val publicId: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val reference: NotificationReference?,
    val createdAt: Instant,
    val isSeen: Boolean,
    val isRead: Boolean,
)

data class NotificationPage(
    val items: List<Notification>,
    val hasNext: Boolean,
)
```

```kotlin
// domain/repository/NotificationRepository.kt
package com.hanmaum.dn.mobile.features.notification.domain.repository

import com.hanmaum.dn.mobile.features.notification.domain.model.NotificationPage

interface NotificationRepository {
    suspend fun getNotifications(page: Int): Result<NotificationPage>
    suspend fun getUnseenCount(): Result<Int>
    suspend fun markAllSeen(): Result<Unit>
    suspend fun markRead(publicId: String): Result<Unit>
    suspend fun markAllRead(): Result<Unit>
    suspend fun getPushEnabled(): Result<Boolean>
    suspend fun setPushEnabled(enabled: Boolean): Result<Unit>
    suspend fun registerDeviceToken(token: String, platform: String): Result<Unit>
    suspend fun deleteDeviceToken(token: String): Result<Unit>
}
```

- [ ] **Step 2: Write the failing MockEngine tests**

Copy the `mockClient` helper verbatim from `MinistryRepositoryImplTest.kt` (same file layout: private `testJson`, `mockClient(responseJson, onRequest)`). Then:

```kotlin
package com.hanmaum.dn.mobile.features.notification.data.repository

// imports: mirror MinistryRepositoryImplTest, plus io.ktor.http.content.TextContent

class NotificationRepositoryImplTest {

    private val notificationJson = """
        {"success":true,"message":null,"data":{"items":[
          {"publicId":"n1","type":"ANNOUNCEMENT","title":"새로운 소식이 있습니다!","body":"여름 수련회",
           "referenceType":"ANNOUNCEMENT","referencePublicId":"a1",
           "createdAt":"2026-07-15T10:00:00Z","seenAt":null,"readAt":null},
          {"publicId":"n2","type":"PRAYER_ALERT","title":"t","body":"b",
           "referenceType":null,"referencePublicId":null,
           "createdAt":"2026-07-14T10:00:00Z","seenAt":"2026-07-14T11:00:00Z","readAt":"2026-07-14T11:00:00Z"}
        ],"page":0,"hasNext":true}}
    """.trimIndent()

    @Test
    fun `getNotifications hits the right path and maps fields`() = runTest {
        var path = ""
        val repo = NotificationRepositoryImpl(mockClient(notificationJson) { path = it.url.encodedPath + "?" + it.url.encodedQuery })
        val page = repo.getNotifications(page = 0).getOrThrow()
        assertEquals("/me/notifications?page=0&size=20", path)
        assertEquals(2, page.items.size)
        assertTrue(page.hasNext)
        val first = page.items[0]
        assertEquals(NotificationType.ANNOUNCEMENT, first.type)
        assertEquals("a1", first.reference?.publicId)
        assertEquals(false, first.isSeen)
        assertEquals(false, first.isRead)
    }

    @Test
    fun `unknown type maps to UNKNOWN with no reference crash`() = runTest {
        val repo = NotificationRepositoryImpl(mockClient(notificationJson))
        val second = repo.getNotifications(0).getOrThrow().items[1]
        assertEquals(NotificationType.UNKNOWN, second.type)
        assertEquals(null, second.reference)
        assertTrue(second.isRead)
    }

    @Test
    fun `unseen count parses`() = runTest {
        val repo = NotificationRepositoryImpl(mockClient("""{"success":true,"message":null,"data":{"count":3}}"""))
        assertEquals(3, repo.getUnseenCount().getOrThrow())
    }

    @Test
    fun `register device token sends exact body keys`() = runTest {
        var body = ""
        var method = ""
        var path = ""
        val repo = NotificationRepositoryImpl(
            mockClient("""{"success":true,"message":null,"data":null}""") {
                method = it.method.value; path = it.url.encodedPath
                body = (it.body as TextContent).text
            },
        )
        repo.registerDeviceToken("tok123", "ANDROID").getOrThrow()
        assertEquals("PUT", method)
        assertEquals("/me/device-tokens", path)
        assertEquals("""{"token":"tok123","platform":"ANDROID"}""", body)
    }

    @Test
    fun `set push enabled sends exact body`() = runTest {
        var body = ""
        val repo = NotificationRepositoryImpl(
            mockClient("""{"success":true,"message":null,"data":null}""") { body = (it.body as TextContent).text },
        )
        repo.setPushEnabled(false).getOrThrow()
        assertEquals("""{"pushEnabled":false}""", body)
    }

    @Test
    fun `mark read posts to the notification path`() = runTest {
        var path = ""
        var method = ""
        val repo = NotificationRepositoryImpl(
            mockClient("""{"success":true,"message":null,"data":null}""") { path = it.url.encodedPath; method = it.method.value },
        )
        repo.markRead("n1").getOrThrow()
        assertEquals("POST", method)
        assertEquals("/me/notifications/n1/read", path)
    }

    @Test
    fun `server error status maps to failure`() = runTest {
        val client = HttpClient(MockEngine { respond("{}", HttpStatusCode.InternalServerError, headersOf(HttpHeaders.ContentType, "application/json")) }) {
            install(ContentNegotiation) { json(testJson) }
        }
        assertTrue(NotificationRepositoryImpl(client).getUnseenCount().isFailure)
    }
}
```

- [ ] **Step 3: Run to verify failure**

Run: `./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.features.notification.data.repository.NotificationRepositoryImplTest"`
Expected: COMPILE FAILURE (types unresolved).

- [ ] **Step 4: DTOs + repository implementation**

```kotlin
// data/model/NotificationDtos.kt
package com.hanmaum.dn.mobile.features.notification.data.model

import com.hanmaum.dn.mobile.features.notification.domain.model.Notification
import com.hanmaum.dn.mobile.features.notification.domain.model.NotificationReference
import com.hanmaum.dn.mobile.features.notification.domain.model.NotificationType
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class NotificationResponse(
    val publicId: String,
    val type: String,
    val title: String,
    val body: String,
    val referenceType: String? = null,
    val referencePublicId: String? = null,
    val createdAt: String,
    val seenAt: String? = null,
    val readAt: String? = null,
) {
    fun toDomain(): Notification {
        val refType = referenceType?.let { rt -> NotificationType.entries.find { it.name == rt } }
        return Notification(
            publicId = publicId,
            type = NotificationType.entries.find { it.name == type } ?: NotificationType.UNKNOWN,
            title = title,
            body = body,
            reference = if (refType != null && referencePublicId != null) {
                NotificationReference(refType, referencePublicId)
            } else {
                null
            },
            createdAt = Instant.parse(createdAt),
            isSeen = seenAt != null,
            isRead = readAt != null,
        )
    }
}

@Serializable
data class NotificationPageResponse(
    val items: List<NotificationResponse>,
    val page: Int,
    val hasNext: Boolean,
)

@Serializable
data class UnseenCountResponse(val count: Int)

@Serializable
data class RegisterDeviceTokenRequest(val token: String, val platform: String)

@Serializable
data class NotificationSettingsDto(val pushEnabled: Boolean)
```

```kotlin
// data/repository/NotificationRepositoryImpl.kt
package com.hanmaum.dn.mobile.features.notification.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.notification.data.model.NotificationPageResponse
import com.hanmaum.dn.mobile.features.notification.data.model.NotificationSettingsDto
import com.hanmaum.dn.mobile.features.notification.data.model.RegisterDeviceTokenRequest
import com.hanmaum.dn.mobile.features.notification.data.model.UnseenCountResponse
import com.hanmaum.dn.mobile.features.notification.domain.model.NotificationPage
import com.hanmaum.dn.mobile.features.notification.domain.repository.NotificationRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class NotificationRepositoryImpl(
    private val client: HttpClient,
) : NotificationRepository {

    override suspend fun getNotifications(page: Int): Result<NotificationPage> = runCatching {
        val response = client.get("me/notifications?page=$page&size=20")
        check(response.status.isSuccess()) { "알림을 불러오지 못했습니다 (${response.status})" }
        val body = response.body<ApiResponse<NotificationPageResponse>>()
        val data = body.data ?: error("알림 응답이 비어 있습니다")
        NotificationPage(items = data.items.map { it.toDomain() }, hasNext = data.hasNext)
    }

    override suspend fun getUnseenCount(): Result<Int> = runCatching {
        val response = client.get("me/notifications/unseen-count")
        check(response.status.isSuccess()) { "unseen count failed (${response.status})" }
        response.body<ApiResponse<UnseenCountResponse>>().data?.count ?: 0
    }

    override suspend fun markAllSeen(): Result<Unit> = simplePost("me/notifications/mark-seen")

    override suspend fun markRead(publicId: String): Result<Unit> = simplePost("me/notifications/$publicId/read")

    override suspend fun markAllRead(): Result<Unit> = simplePost("me/notifications/read-all")

    override suspend fun getPushEnabled(): Result<Boolean> = runCatching {
        val response = client.get("me/notification-settings")
        check(response.status.isSuccess()) { "settings load failed (${response.status})" }
        response.body<ApiResponse<NotificationSettingsDto>>().data?.pushEnabled ?: true
    }

    override suspend fun setPushEnabled(enabled: Boolean): Result<Unit> = runCatching {
        val response = client.put("me/notification-settings") {
            contentType(ContentType.Application.Json)
            setBody(NotificationSettingsDto(pushEnabled = enabled))
        }
        check(response.status.isSuccess()) { "settings save failed (${response.status})" }
    }

    override suspend fun registerDeviceToken(token: String, platform: String): Result<Unit> = runCatching {
        val response = client.put("me/device-tokens") {
            contentType(ContentType.Application.Json)
            setBody(RegisterDeviceTokenRequest(token = token, platform = platform))
        }
        check(response.status.isSuccess()) { "token register failed (${response.status})" }
    }

    override suspend fun deleteDeviceToken(token: String): Result<Unit> = runCatching {
        val response = client.delete("me/device-tokens/$token")
        check(response.status.isSuccess()) { "token delete failed (${response.status})" }
    }

    private suspend fun simplePost(path: String): Result<Unit> = runCatching {
        val response = client.post(path)
        check(response.status.isSuccess()) { "$path failed (${response.status})" }
    }
}
```

- [ ] **Step 5: Koin binding** — in `AppModule.kt` next to the other repositories:

```kotlin
    single<NotificationRepository> { NotificationRepositoryImpl(get()) }
```

- [ ] **Step 6: Run tests** → the whole `./gradlew :composeApp:testDevDebugUnitTest` green.

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/notification composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/notification composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/di/AppModule.kt
git commit -m "feat(notification): domain, wire DTOs and repository with contract tests"
```

### Task B3: NotificationListViewModel (TDD)

**Files:**
- Test: `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/notification/presentation/NotificationListViewModelTest.kt`
- Create: `$M/features/notification/presentation/NotificationListViewModel.kt`
- Modify: `$M/di/AppModule.kt`

**Interfaces:**
- Consumes: `NotificationRepository` (B2).
- Produces (screen in B5 renders exactly this):
  ```kotlin
  data class NotificationListUiState(
      val items: List<Notification> = emptyList(),
      val isLoading: Boolean = false,
      val isLoadingMore: Boolean = false,
      val hasNext: Boolean = false,
      val error: String? = null,
  ) { val allRead: Boolean get() = items.all { it.isRead } }

  class NotificationListViewModel(repository) : ViewModel() {
      val uiState: StateFlow<NotificationListUiState>
      val openAnnouncement: SharedFlow<String>   // announcement publicId to navigate to
      fun load()          // page 0 + markAllSeen on success
      fun loadMore()
      fun onItemClick(notification: Notification)
      fun onReadAll()
  }
  ```

- [ ] **Step 1: Write the failing tests** (fake repository, `StandardTestDispatcher`; look at an existing ViewModel test for the `Dispatchers.setMain` setup idiom — `grep -rln "StandardTestDispatcher" composeApp/src/commonTest | head -1`)

```kotlin
package com.hanmaum.dn.mobile.features.notification.presentation

import com.hanmaum.dn.mobile.features.notification.domain.model.Notification
import com.hanmaum.dn.mobile.features.notification.domain.model.NotificationPage
import com.hanmaum.dn.mobile.features.notification.domain.model.NotificationReference
import com.hanmaum.dn.mobile.features.notification.domain.model.NotificationType
import com.hanmaum.dn.mobile.features.notification.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun sample(id: String, read: Boolean = false) = Notification(
    publicId = id,
    type = NotificationType.ANNOUNCEMENT,
    title = "새로운 소식이 있습니다!",
    body = "본문",
    reference = NotificationReference(NotificationType.ANNOUNCEMENT, "a-$id"),
    createdAt = Instant.parse("2026-07-15T10:00:00Z"),
    isSeen = false,
    isRead = read,
)

private class FakeNotificationRepository : NotificationRepository {
    var pages = mutableMapOf(0 to NotificationPage(listOf(sample("n1"), sample("n2", read = true)), hasNext = true),
                             1 to NotificationPage(listOf(sample("n3")), hasNext = false))
    var markAllSeenCalls = 0
    var markReadIds = mutableListOf<String>()
    var markAllReadCalls = 0
    var failList = false

    override suspend fun getNotifications(page: Int) =
        if (failList) Result.failure(RuntimeException("boom")) else Result.success(pages.getValue(page))
    override suspend fun getUnseenCount() = Result.success(0)
    override suspend fun markAllSeen(): Result<Unit> { markAllSeenCalls++; return Result.success(Unit) }
    override suspend fun markRead(publicId: String): Result<Unit> { markReadIds += publicId; return Result.success(Unit) }
    override suspend fun markAllRead(): Result<Unit> { markAllReadCalls++; return Result.success(Unit) }
    override suspend fun getPushEnabled() = Result.success(true)
    override suspend fun setPushEnabled(enabled: Boolean) = Result.success(Unit)
    override suspend fun registerDeviceToken(token: String, platform: String) = Result.success(Unit)
    override suspend fun deleteDeviceToken(token: String) = Result.success(Unit)
}

class NotificationListViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `load populates items and marks all seen`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository()
        val vm = NotificationListViewModel(repo)
        vm.load(); advanceUntilIdle()
        assertEquals(2, vm.uiState.value.items.size)
        assertTrue(vm.uiState.value.hasNext)
        assertEquals(1, repo.markAllSeenCalls)
    }

    @Test
    fun `item click marks read optimistically and emits navigation`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository()
        val vm = NotificationListViewModel(repo)
        vm.load(); advanceUntilIdle()
        var navigated: String? = null
        val job = kotlinx.coroutines.launch { navigated = vm.openAnnouncement.first() }
        vm.onItemClick(vm.uiState.value.items[0]); advanceUntilIdle()
        assertTrue(vm.uiState.value.items[0].isRead)
        assertEquals(listOf("n1"), repo.markReadIds)
        assertEquals("a-n1", navigated)
        job.cancel()
    }

    @Test
    fun `read all flips every item`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository()
        val vm = NotificationListViewModel(repo)
        vm.load(); advanceUntilIdle()
        vm.onReadAll(); advanceUntilIdle()
        assertTrue(vm.uiState.value.allRead)
        assertEquals(1, repo.markAllReadCalls)
    }

    @Test
    fun `load more appends next page`() = runTest(dispatcher) {
        val vm = NotificationListViewModel(FakeNotificationRepository())
        vm.load(); advanceUntilIdle()
        vm.loadMore(); advanceUntilIdle()
        assertEquals(3, vm.uiState.value.items.size)
        assertEquals(false, vm.uiState.value.hasNext)
    }

    @Test
    fun `load failure surfaces error`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository().apply { failList = true }
        val vm = NotificationListViewModel(repo)
        vm.load(); advanceUntilIdle()
        assertTrue(vm.uiState.value.error != null)
    }
}
```

- [ ] **Step 2: Run to verify failure** → compile error.

- [ ] **Step 3: Implement**

```kotlin
// presentation/NotificationListViewModel.kt
package com.hanmaum.dn.mobile.features.notification.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.notification.domain.model.Notification
import com.hanmaum.dn.mobile.features.notification.domain.model.NotificationType
import com.hanmaum.dn.mobile.features.notification.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationListUiState(
    val items: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
) {
    val allRead: Boolean get() = items.all { it.isRead }
}

class NotificationListViewModel(
    private val repository: NotificationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationListUiState())
    val uiState = _uiState.asStateFlow()

    private val _openAnnouncement = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val openAnnouncement = _openAnnouncement.asSharedFlow()

    private var page = 0

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.items.isEmpty(), error = null) }
            repository.getNotifications(page = 0)
                .onSuccess { result ->
                    page = 0
                    _uiState.update { it.copy(isLoading = false, items = result.items, hasNext = result.hasNext) }
                    // Opening the screen means everything is now "seen" (badge -> 0).
                    repository.markAllSeen()
                }
                .onFailure { _ ->
                    _uiState.update { it.copy(isLoading = false, error = "알림을 불러오지 못했습니다") }
                }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (!state.hasNext || state.isLoadingMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            repository.getNotifications(page = page + 1)
                .onSuccess { result ->
                    page += 1
                    _uiState.update { it.copy(isLoadingMore = false, items = it.items + result.items, hasNext = result.hasNext) }
                }
                .onFailure { _uiState.update { it.copy(isLoadingMore = false) } }
        }
    }

    fun onItemClick(notification: Notification) {
        _uiState.update { state ->
            state.copy(items = state.items.map { if (it.publicId == notification.publicId) it.copy(isRead = true) else it })
        }
        viewModelScope.launch { repository.markRead(notification.publicId) }
        val ref = notification.reference
        if (ref != null && ref.type == NotificationType.ANNOUNCEMENT) {
            _openAnnouncement.tryEmit(ref.publicId)
        }
    }

    fun onReadAll() {
        if (_uiState.value.allRead) return
        _uiState.update { state -> state.copy(items = state.items.map { it.copy(isRead = true) }) }
        viewModelScope.launch { repository.markAllRead() }
    }
}
```

- [ ] **Step 4: Koin** — `viewModel { NotificationListViewModel(get()) }` in `AppModule.kt`.

- [ ] **Step 5: Run tests** → green. **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/notification composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/notification composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/di/AppModule.kt
git commit -m "feat(notification): list viewmodel with seen read semantics"
```

### Task B4: Home bell badge (TDD on the ViewModel)

**Files:**
- Modify: `$M/features/announcement/presentation/HomeViewModel.kt`
- Modify: `$M/features/announcement/presentation/HomeScreen.kt` (HomeTopBar)
- Modify: `$M/di/AppModule.kt` (HomeViewModel gets 2nd dependency)
- Modify: `$M/App.kt` (pass bell click → navigate)
- Modify: `$M/core/navigation/Routes.kt` (`NotificationListRoute`)
- Test: extend/create `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/announcement/presentation/HomeViewModelTest.kt`

**Interfaces:**
- Consumes: `NotificationRepository.getUnseenCount()` (B2).
- Produces: `HomeUiState.unseenCount: Int` (default 0); `HomeViewModel(repository, notificationRepository)`; `@Serializable object NotificationListRoute`; `HomeScreen(..., onNotificationsClick: () -> Unit)`.

- [ ] **Step 1: Failing test** — reuse the `FakeNotificationRepository` idea from B3 (duplicate the fake inside this test file; each test file owns its fakes) plus a fake `AnnouncementRepository` returning `emptyList()`:

```kotlin
    @Test
    fun `unseen count lands in ui state`() = runTest(dispatcher) {
        val vm = HomeViewModel(FakeAnnouncementRepository(), FakeNotificationRepository(unseen = 5))
        vm.loadAnnouncements(); advanceUntilIdle()
        assertEquals(5, vm.uiState.value.unseenCount)
    }

    @Test
    fun `unseen count failure keeps zero`() = runTest(dispatcher) {
        val vm = HomeViewModel(FakeAnnouncementRepository(), FakeNotificationRepository(failCount = true))
        vm.loadAnnouncements(); advanceUntilIdle()
        assertEquals(0, vm.uiState.value.unseenCount)
    }
```

(`FakeNotificationRepository(unseen: Int = 0, failCount: Boolean = false)` — `getUnseenCount()` returns failure when `failCount`; all other members return `Result.success` defaults.)

- [ ] **Step 2: Verify failure, then implement**

`HomeUiState` gains `val unseenCount: Int = 0`. `HomeViewModel` constructor gains `private val notificationRepository: NotificationRepository`. Inside `loadAnnouncements()` add (before/independent of the announcement fetch — a badge failure must never affect announcements):

```kotlin
        viewModelScope.launch {
            notificationRepository.getUnseenCount()
                .onSuccess { count -> _uiState.update { it.copy(unseenCount = count) } }
            // onFailure: keep the previous count; the badge is best-effort.
        }
```

`AppModule.kt`: `viewModel { HomeViewModel(repository = get(), notificationRepository = get()) }`.

- [ ] **Step 3: Route + bell UI**

`Routes.kt`: add `@Serializable object NotificationListRoute` next to the other routes.

`HomeTopBar` in `HomeScreen.kt` — replace the stub `IconButton` (currently `onClick = { /* 알림 기능 추가 예정 */ }`) with a badged, wired version. `HomeTopBar` gains parameters `unseenCount: Int` and `onNotificationsClick: () -> Unit` (threaded from `HomeScreen`'s parameters; `HomeScreen` gains `onNotificationsClick: () -> Unit`):

```kotlin
        BadgedBox(
            badge = {
                // DESIGN.md: badge appears/disappears with a spring scale, never a bare pop.
                androidx.compose.animation.AnimatedVisibility(
                    visible = unseenCount > 0,
                    enter = androidx.compose.animation.scaleIn(spring(dampingRatio = 0.6f, stiffness = 400f)),
                    exit = androidx.compose.animation.scaleOut(spring(dampingRatio = 0.6f, stiffness = 400f)),
                ) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                        Text(if (unseenCount > 9) "9+" else unseenCount.toString())
                    }
                }
            },
        ) {
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = strings.notifications,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
```

In `App.kt`, at the `HomeScreen` registration, pass `onNotificationsClick = { navController.navigate(NotificationListRoute) }`. (B5 registers the destination; until then navigating would crash — B4 and B5 land in one PR, run order B4 → B5 before manual testing.)

- [ ] **Step 4: Tests + commit**

Run: `./gradlew :composeApp:testDevDebugUnitTest` → green.

```bash
git add -A composeApp/src/commonMain composeApp/src/commonTest
git commit -m "feat(notification): unseen count badge on home bell"
```

### Task B5: Notification screen + navigation registration

**Files:**
- Create: `$M/features/notification/presentation/NotificationListScreen.kt`
- Create: `$M/features/notification/presentation/components/NotificationRow.kt`
- Modify: `$M/App.kt`

**Interfaces:**
- Consumes: `NotificationListViewModel` (B3 exact API), `NotificationListRoute` (B4), `AnnouncementDetailRoute(id)`, strings from B1.
- Produces: `NotificationListScreen(onBack: () -> Unit, onOpenAnnouncement: (String) -> Unit)` registered in `App.kt`.

- [ ] **Step 1: Row component**

Follow HomeScreen's import style. DESIGN.md rules embedded below: card on `surfaceContainerLowest`, `shape_medium`≈`RoundedCornerShape(14.dp)` — BUT first check `core/presentation/theme/` for shape/spacing tokens (`ls composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/presentation/theme/`); if a `Shapes`/`Dimens` object exists, use it instead of literals.

```kotlin
// presentation/components/NotificationRow.kt
package com.hanmaum.dn.mobile.features.notification.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.features.notification.domain.model.Notification
import com.hanmaum.dn.mobile.features.notification.domain.model.NotificationType

@Composable
fun NotificationRow(
    notification: Notification,
    timeText: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "rowPress",
    )
    val icon: ImageVector = when (notification.type) {
        NotificationType.ANNOUNCEMENT -> Icons.Default.Campaign
        else -> Icons.Default.Notifications
    }
    val iconTint = when (notification.type) {
        NotificationType.ANNOUNCEMENT -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(MaterialTheme.shapes.medium)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconTint.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold,
                    color = if (notification.isRead) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            if (!notification.isRead) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            }
        }
    }
}
```

- [ ] **Step 2: Screen**

```kotlin
// presentation/NotificationListScreen.kt
package com.hanmaum.dn.mobile.features.notification.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.features.notification.domain.model.Notification
import com.hanmaum.dn.mobile.features.notification.presentation.components.NotificationRow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NotificationListScreen(
    onBack: () -> Unit,
    onOpenAnnouncement: (String) -> Unit,
    viewModel: NotificationListViewModel = koinViewModel(),
) {
    val strings = LocalStrings.current
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(Unit) { viewModel.openAnnouncement.collect(onOpenAnnouncement) }

    // Infinite scroll: request the next page when the last item becomes visible.
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            state.hasNext && last >= state.items.lastIndex
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) viewModel.loadMore() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = strings.back,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = strings.notificationsTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = viewModel::onReadAll, enabled = !state.allRead && state.items.isNotEmpty()) {
                Text(strings.notificationsReadAll, color = MaterialTheme.colorScheme.primary)
            }
        }

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = viewModel::load) { Text(strings.retry) }
                }
            }
            state.items.isEmpty() -> EmptyNotifications()
            else -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                groupByDay(state.items).forEach { (header, group) ->
                    item(key = "header-$header") {
                        Text(
                            text = when (header) {
                                DayBucket.TODAY -> strings.notificationsToday
                                DayBucket.YESTERDAY -> strings.notificationsYesterday
                                DayBucket.EARLIER -> strings.notificationsEarlier
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(group, key = { it.publicId }) { notification ->
                        NotificationRow(
                            notification = notification,
                            timeText = relativeTime(notification.createdAt),
                            onClick = { viewModel.onItemClick(notification) },
                        )
                    }
                }
                if (state.isLoadingMore) {
                    item(key = "loading-more") {
                        Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNotifications() {
    val strings = LocalStrings.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = strings.notificationsEmpty,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

internal enum class DayBucket { TODAY, YESTERDAY, EARLIER }

internal fun groupByDay(
    items: List<Notification>,
    now: Instant = Clock.System.now(),
): List<Pair<DayBucket, List<Notification>>> {
    val zone = TimeZone.currentSystemDefault()
    val today = now.toLocalDateTime(zone).date
    return items
        .groupBy { n ->
            val date = n.createdAt.toLocalDateTime(zone).date
            when {
                date == today -> DayBucket.TODAY
                date == today.minus(kotlinx.datetime.DatePeriod(days = 1)) -> DayBucket.YESTERDAY
                else -> DayBucket.EARLIER
            }
        }
        .toList()
        .sortedBy { it.first.ordinal }
}

@Composable
private fun relativeTime(createdAt: Instant): String {
    val strings = LocalStrings.current
    val minutes = (Clock.System.now() - createdAt).inWholeMinutes
    return when {
        minutes < 1 -> strings.notificationTimeJustNow
        minutes < 60 -> strings.notificationTimeMinutesAgo(minutes.toInt())
        minutes < 60 * 24 -> strings.notificationTimeHoursAgo((minutes / 60).toInt())
        else -> strings.notificationTimeDaysAgo((minutes / (60 * 24)).toInt())
    }
}
```

Adjust `kotlinx.datetime` API calls to the version in `libs.versions.toml` if `DatePeriod`/`minus` differ (check an existing usage: `grep -rn "kotlinx.datetime" $M --include="*.kt" | head`). If `MaterialTheme.shapes.medium` isn't 14dp in this theme, keep the theme's value — tokens win over the spec's dp number.

**List entry stagger (DESIGN.md — spring, 40ms/item, cap 5):** wrap `NotificationRow` in a small entry animation. Before adding it, check whether an existing list screen already has a stagger helper (`grep -rn "delayMillis\|staggered\|animateItem" $M --include="*.kt" | head`) and reuse it. If none exists, use this inside the `items` lambda around `NotificationRow` (index from `itemsIndexed`):

```kotlin
var appeared by remember { mutableStateOf(false) }
LaunchedEffect(Unit) {
    kotlinx.coroutines.delay(40L * index.coerceAtMost(5))
    appeared = true
}
val alpha by animateFloatAsState(if (appeared) 1f else 0f, tween(200), label = "stagger-a")
val offsetY by animateDpAsState(if (appeared) 0.dp else 8.dp, spring(dampingRatio = 0.85f, stiffness = 260f), label = "stagger-y")
// apply Modifier.graphicsLayer { this.alpha = alpha }.offset(y = offsetY) to NotificationRow's Modifier
```

- [ ] **Step 3: Register in `App.kt`**

Next to the other `composable<...>` blocks:

```kotlin
        composable<NotificationListRoute> {
            NotificationListScreen(
                onBack = { navController.popBackStack() },
                onOpenAnnouncement = { id -> navController.navigate(AnnouncementDetailRoute(id)) },
            )
        }
```

(Import `NotificationListRoute`, match the transition style neighboring `composable<>` registrations use — copy whatever enter/exit spec `AnnouncementDetailRoute` has so push/swipe-back behave identically.)

- [ ] **Step 4: Verify + commit**

Run: `./gradlew :composeApp:testDevDebugUnitTest` (green) and `./gradlew :composeApp:assembleDevDebug` (compiles).

```bash
git add -A composeApp/src/commonMain
git commit -m "feat(notification): notification screen with date groups and read state"
```

### Task B6: Settings push toggle + priming card

**Files:**
- Create: `$M/features/notification/presentation/NotificationSettingsViewModel.kt`
- Modify: `$M/features/profile/presentation/SettingsScreen.kt`
- Modify: `$M/features/announcement/presentation/HomeScreen.kt` (priming card)
- Create: `$M/core/push/PushPreferences.kt`
- Modify: `$M/di/AppModule.kt`
- Test: `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/notification/presentation/NotificationSettingsViewModelTest.kt`

**Interfaces:**
- Consumes: `NotificationRepository` (B2), `PushManager` (defined in B7 — if executing strictly in order, do B7 FIRST or stub `isPermissionGranted()` behind the existing `NotificationService.isNotificationPermissionGranted()`; DECIDED: use the existing `core/notification/NotificationService.isNotificationPermissionGranted()` here so B6 has NO dependency on B7, and the OS-permission request in the priming card uses B7's `PushManager.requestPermission()` — wire that single call site when B7 lands).
- Produces: `NotificationSettingsViewModel` with `NotificationSettingsUiState(pushEnabled: Boolean = true, isLoading: Boolean = false)`, `fun load()`, `fun onToggle(enabled: Boolean)`; `PushPreferences.promptDismissed: Boolean` (multiplatform-settings).

- [ ] **Step 1: PushPreferences** (mirror `LocationPreferencesImpl` — find it: `grep -rn "class LocationPreferencesImpl" $M -r`)

```kotlin
// core/push/PushPreferences.kt
package com.hanmaum.dn.mobile.core.push

import com.russhwolf.settings.Settings

interface PushPreferences {
    fun isPromptDismissed(): Boolean
    fun setPromptDismissed(dismissed: Boolean)
}

class PushPreferencesImpl(private val settings: Settings) : PushPreferences {
    override fun isPromptDismissed(): Boolean = settings.getBoolean(KEY, false)
    override fun setPromptDismissed(dismissed: Boolean) = settings.putBoolean(KEY, dismissed)
    private companion object { const val KEY = "push_prompt_dismissed" }
}
```

Koin: `single<PushPreferences> { PushPreferencesImpl(Settings()) }`.

- [ ] **Step 2: Settings ViewModel (TDD — write this failing test first)**

```kotlin
class NotificationSettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `load reads server flag`() = runTest(dispatcher) {
        // This file's own fake: copy B3's FakeNotificationRepository and add
        //   var pushEnabled = true; val setPushEnabledCalls = mutableListOf<Boolean>(); var failSetPush = false
        // getPushEnabled() returns Result.success(pushEnabled); setPushEnabled records the
        // value and returns failure when failSetPush.
        val repo = FakeNotificationRepository()
        repo.pushEnabled = false
        val vm = NotificationSettingsViewModel(repo)
        vm.load(); advanceUntilIdle()
        assertEquals(false, vm.uiState.value.pushEnabled)
    }

    @Test
    fun `toggle optimistically updates and calls server`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository()
        val vm = NotificationSettingsViewModel(repo)
        vm.load(); advanceUntilIdle()
        vm.onToggle(false); advanceUntilIdle()
        assertEquals(false, vm.uiState.value.pushEnabled)
        assertEquals(listOf(false), repo.setPushEnabledCalls)
    }

    @Test
    fun `toggle reverts on server failure`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository().apply { failSetPush = true }
        val vm = NotificationSettingsViewModel(repo)
        vm.load(); advanceUntilIdle()
        vm.onToggle(false); advanceUntilIdle()
        assertEquals(true, vm.uiState.value.pushEnabled)
    }
}
```

Implementation:

```kotlin
// presentation/NotificationSettingsViewModel.kt
package com.hanmaum.dn.mobile.features.notification.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.notification.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationSettingsUiState(
    val pushEnabled: Boolean = true,
    val isLoading: Boolean = false,
)

class NotificationSettingsViewModel(
    private val repository: NotificationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getPushEnabled()
                .onSuccess { enabled -> _uiState.update { it.copy(isLoading = false, pushEnabled = enabled) } }
                .onFailure { _uiState.update { it.copy(isLoading = false) } }
        }
    }

    fun onToggle(enabled: Boolean) {
        val previous = _uiState.value.pushEnabled
        _uiState.update { it.copy(pushEnabled = enabled) }
        viewModelScope.launch {
            repository.setPushEnabled(enabled)
                .onFailure { _uiState.update { it.copy(pushEnabled = previous) } }
        }
    }
}
```

Koin: `viewModel { NotificationSettingsViewModel(get()) }`.

- [ ] **Step 3: Settings row** — open `SettingsScreen.kt`, find its row/section idiom (it was built in PR #84 with a settings-row composable) and add, in the notifications/general section, following the file's own pattern:

```kotlin
    val settingsVm: NotificationSettingsViewModel = koinViewModel()
    val pushState by settingsVm.uiState.collectAsState()
    LaunchedEffect(Unit) { settingsVm.load() }
    val notificationService = koinInject<NotificationService>()   // org.koin.compose.koinInject
    // ... inside the settings list, styled like the sibling rows:
    //   Title: strings.settingsPushToggle
    //   Trailing: Switch(checked = pushState.pushEnabled, onCheckedChange = settingsVm::onToggle)
    //   Below, only when !notificationService.isNotificationPermissionGranted():
    //   Text(strings.settingsPushPermissionHint, style = MaterialTheme.typography.bodySmall,
    //        color = MaterialTheme.colorScheme.outline)
```

Use the file's existing row composable rather than inventing one — this is a follow-the-sibling task, the exact code depends on that file's helpers.

- [ ] **Step 4: Priming card on Home** — mirror `GeofenceRationaleCard` (same file, `HomeScreen.kt` ~line 200): a card shown when `!pushPreferences.isPromptDismissed()` and notification permission not granted, with `strings.pushPrimingTitle` / `pushPrimingBody`, `OutlinedButton(strings.laterButton)` → `pushPreferences.setPromptDismissed(true)`, `Button(strings.pushPrimingEnable)` → request the OS permission (B7's `PushManager.requestPermission()`; until B7 lands, wire the button to `pushPreferences.setPromptDismissed(true)` only and leave a `// wired to PushManager in the push-plumbing task` comment — NEVER the word T-O-D-O). Place it under the geofence card block with the same visibility pattern.

- [ ] **Step 5: Tests + commit**

Run: `./gradlew :composeApp:testDevDebugUnitTest` → green.

```bash
git add -A composeApp/src/commonMain composeApp/src/commonTest
git commit -m "feat(notification): settings push toggle and home priming card"
```

### Task B7: Push plumbing — common interface, event bus, payload parser (TDD)

**Files:**
- Create: `$M/core/push/PushManager.kt`
- Create: `$M/core/push/PushEventBus.kt`
- Create: `$M/core/push/PushTapPayload.kt`
- Test: `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/core/push/PushTapPayloadTest.kt`

**Interfaces:**
- Produces (B8/B9 implement, B10 consumes):
  ```kotlin
  interface PushManager {
      val platform: String                      // "ANDROID" | "IOS" — wire value for registerDeviceToken
      suspend fun currentToken(): String?
      fun isPermissionGranted(): Boolean
      suspend fun requestPermission(): Boolean
  }
  object PushEventBus {
      val tokenRefreshes: MutableSharedFlow<String>          // extraBufferCapacity = 1
      val notificationTaps: MutableSharedFlow<PushTapPayload> // replay = 1 (cold-start tap survives until Home collects)
      fun consumeTap()                                        // clears replay cache after handling
  }
  data class PushTapPayload(type, referenceType, referencePublicId, notificationPublicId — all String?)
  fun parsePushTap(data: Map<String, String>): PushTapPayload?
  ```

- [ ] **Step 1: Failing parser test**

```kotlin
package com.hanmaum.dn.mobile.core.push

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PushTapPayloadTest {
    @Test
    fun `full payload parses all keys`() {
        val payload = parsePushTap(
            mapOf(
                "type" to "ANNOUNCEMENT",
                "referenceType" to "ANNOUNCEMENT",
                "referencePublicId" to "a1",
                "notificationPublicId" to "n1",
            ),
        )
        assertEquals("ANNOUNCEMENT", payload?.referenceType)
        assertEquals("a1", payload?.referencePublicId)
        assertEquals("n1", payload?.notificationPublicId)
    }

    @Test
    fun `payload without our keys returns null`() {
        assertNull(parsePushTap(mapOf("google.message_id" to "x")))
    }

    @Test
    fun `partial payload keeps missing keys null`() {
        val payload = parsePushTap(mapOf("type" to "ANNOUNCEMENT"))
        assertEquals("ANNOUNCEMENT", payload?.type)
        assertNull(payload?.referencePublicId)
    }
}
```

- [ ] **Step 2: Verify failure, implement**

```kotlin
// core/push/PushTapPayload.kt
package com.hanmaum.dn.mobile.core.push

data class PushTapPayload(
    val type: String?,
    val referenceType: String?,
    val referencePublicId: String?,
    val notificationPublicId: String?,
)

/** Returns null when the map carries none of our data keys (e.g. a bare FCM system map). */
fun parsePushTap(data: Map<String, String>): PushTapPayload? {
    val payload = PushTapPayload(
        type = data["type"],
        referenceType = data["referenceType"],
        referencePublicId = data["referencePublicId"],
        notificationPublicId = data["notificationPublicId"],
    )
    return if (payload.type == null && payload.referenceType == null &&
        payload.referencePublicId == null && payload.notificationPublicId == null
    ) null else payload
}
```

```kotlin
// core/push/PushEventBus.kt
package com.hanmaum.dn.mobile.core.push

import kotlinx.coroutines.flow.MutableSharedFlow

/** Bridge between platform push callbacks (service/AppDelegate) and common code. */
object PushEventBus {
    val tokenRefreshes = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val notificationTaps = MutableSharedFlow<PushTapPayload>(replay = 1, extraBufferCapacity = 1)

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun consumeTap() {
        notificationTaps.resetReplayCache()
    }
}
```

(`resetReplayCache` is `@ExperimentalCoroutinesApi` — annotate accordingly; if the opt-in produces friction, drop `consumeTap` and instead guard double-handling in the collector with the `notificationPublicId`.)

```kotlin
// core/push/PushManager.kt
package com.hanmaum.dn.mobile.core.push

interface PushManager {
    /** Wire value for registerDeviceToken: "ANDROID" or "IOS". */
    val platform: String
    suspend fun currentToken(): String?
    fun isPermissionGranted(): Boolean
    suspend fun requestPermission(): Boolean
}
```

- [ ] **Step 3: Wire the priming card button** (from B6): inject `PushManager` via `koinInject()` in `HomeScreen`, button calls `scope.launch { pushManager.requestPermission(); pushPreferences.setPromptDismissed(true) }`, replacing the placeholder wiring. (Binding exists only after B8/B9 — Android first is fine; iOS Koin startup fails if the binding is missing on iOS, so B8 AND B9 must both land before running on iOS.)

- [ ] **Step 4: Tests + commit**

Run: `./gradlew :composeApp:testDevDebugUnitTest` green, AND (shared code + no platform impls yet — expected to still compile on iOS): `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:iosSimulatorArm64Test` green.

```bash
git add -A composeApp/src/commonMain composeApp/src/commonTest
git commit -m "feat(push): common push manager interface event bus and payload parser"
```

### Task B8: Android push integration

**Files:**
- Modify: `gradle/libs.versions.toml`, root `build.gradle.kts`, `composeApp/build.gradle.kts`
- Create: `composeApp/src/androidMain/kotlin/com/hanmaum/dn/mobile/core/push/AndroidPushManager.kt`
- Create: `composeApp/src/androidMain/kotlin/com/hanmaum/dn/mobile/core/push/DnFirebaseMessagingService.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/hanmaum/dn/mobile/di/PlatformModule.android.kt`
- Modify: `composeApp/src/androidMain/AndroidManifest.xml`
- Modify: `composeApp/src/androidMain/kotlin/com/hanmaum/dn/mobile/MainActivity.kt`

**Interfaces:**
- Consumes: `PushManager`/`PushEventBus`/`parsePushTap` (B7).
- Produces: Koin `single<PushManager> { AndroidPushManager(androidContext()) }`.

- [ ] **Step 1: Gradle wiring**

`gradle/libs.versions.toml`:

```toml
# [versions]
googleServices = "4.4.4"
firebaseMessaging = "25.0.1"
# [libraries]
firebase-messaging = { module = "com.google.firebase:firebase-messaging", version.ref = "firebaseMessaging" }
# [plugins]
googleServices = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

(If either version doesn't resolve from `google()`, take the latest stable — these two do NOT touch the lifecycle/navigation/koin trio, so bumping is safe.)

Root `build.gradle.kts` plugins block: `alias(libs.plugins.googleServices) apply false`.

`composeApp/build.gradle.kts`:
- `androidMain.dependencies { implementation(libs.firebase.messaging) }`
- After the `plugins {}` block (top level), apply conditionally so builds without the Firebase config file still work:

```kotlin
// google-services requires composeApp/google-services.json (see plan: Human prerequisites).
// Applied conditionally so clean checkouts and CI without the file still build.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
```

- [ ] **Step 2: Messaging service**

```kotlin
// core/push/DnFirebaseMessagingService.kt (androidMain)
package com.hanmaum.dn.mobile.core.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hanmaum.dn.mobile.MainActivity

internal const val ANNOUNCEMENT_CHANNEL_ID = "announcements"
internal const val PUSH_DATA_PREFIX = "push_"

class DnFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        PushEventBus.tokenRefreshes.tryEmit(token)
    }

    // Called for foreground messages only (background notification+data messages
    // are rendered by FCM and delivered as launcher-intent extras on tap).
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: return
        val body = message.notification?.body ?: ""
        ensureChannel(this)

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            message.data.forEach { (k, v) -> putExtra(PUSH_DATA_PREFIX + k, v) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            message.data["notificationPublicId"]?.hashCode() ?: 0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, ANNOUNCEMENT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        try {
            NotificationManagerCompat.from(this).notify((message.messageId ?: title).hashCode(), notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS revoked between grant check and notify.
        }
    }
}

internal fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(ANNOUNCEMENT_CHANNEL_ID, "공지 알림", NotificationManager.IMPORTANCE_HIGH),
        )
    }
}
```

(Small icon: reuse whatever `AndroidNotificationService` uses; if a proper app icon drawable exists, prefer it. Channel name hardcoded Korean = existing `AndroidNotificationService` precedent.)

- [ ] **Step 3: AndroidPushManager**

```kotlin
// core/push/AndroidPushManager.kt (androidMain)
package com.hanmaum.dn.mobile.core.push

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.hanmaum.dn.mobile.core.notification.NotificationService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidPushManager(
    private val context: Context,
    private val notificationService: NotificationService,
) : PushManager {
    override val platform: String = "ANDROID"

    override suspend fun currentToken(): String? = suspendCancellableCoroutine { cont ->
        try {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        } catch (_: Exception) {
            // FirebaseApp not initialized (no google-services.json) - push disabled.
            cont.resume(null)
        }
    }

    override fun isPermissionGranted(): Boolean = notificationService.isNotificationPermissionGranted()

    override suspend fun requestPermission(): Boolean {
        // POST_NOTIFICATIONS needs an Activity-based request. HomeScreen already has a
        // permission-request mechanism for the geofence flow - REUSE IT: read HomeScreen.kt
        // lines ~60-115 and trigger the same launcher for POST_NOTIFICATIONS.
        // From this manager we can only report the current state:
        return isNotificationPermissionGranted()
    }

    private fun isNotificationPermissionGranted() = notificationService.isNotificationPermissionGranted()
}
```

IMPORTANT for the implementer: inspect how `HomeScreen` requests the geofence/notification permission today (`requestingPermission` state around line 100). If it uses a composable permission launcher (e.g. an expect/actual or accompanist-like helper), route the priming card's 알림 켜기 through THAT mechanism on Android, and through `PushManager.requestPermission()` on iOS (where B9's implementation does the real `UNUserNotificationCenter` request). Keep `PushManager.requestPermission()` as the common API; the Android actual may legitimately be a no-op that returns current state if the composable launcher path is used.

- [ ] **Step 4: Manifest + MainActivity + Koin**

`AndroidManifest.xml` inside `<application>`:

```xml
        <service
            android:name="com.hanmaum.dn.mobile.core.push.DnFirebaseMessagingService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>
        <meta-data
            android:name="com.google.firebase.messaging.default_notification_channel_id"
            android:value="announcements" />
```

`MainActivity.kt` — forward tap extras (cold start AND warm):

```kotlin
    // in onCreate, after existing setup:
    handlePushExtras(intent)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handlePushExtras(intent)
    }

    private fun handlePushExtras(intent: Intent?) {
        val extras = intent?.extras ?: return
        val data = extras.keySet()
            .filter { it.startsWith(PUSH_DATA_PREFIX) }
            .associate { it.removePrefix(PUSH_DATA_PREFIX) to (extras.getString(it) ?: "") }
        // Background-delivered FCM taps put data keys directly on the launcher intent (no prefix):
        val direct = listOf("type", "referenceType", "referencePublicId", "notificationPublicId")
            .mapNotNull { key -> extras.getString(key)?.let { key to it } }
            .toMap()
        parsePushTap(if (data.isNotEmpty()) data else direct)?.let { PushEventBus.notificationTaps.tryEmit(it) }
    }
```

`PlatformModule.android.kt`: `single<PushManager> { AndroidPushManager(androidContext(), get()) }`.

- [ ] **Step 5: Build + test + commit**

Run: `./gradlew :composeApp:testDevDebugUnitTest && ./gradlew :composeApp:assembleDevDebug` → both green (assemble works with or without `google-services.json` thanks to the conditional apply).

```bash
git add -A gradle composeApp/build.gradle.kts build.gradle.kts composeApp/src/androidMain
git commit -m "feat(push): android fcm service token manager and tap intent forwarding"
```

### Task B9: iOS push integration

**Files:**
- Create: `composeApp/src/iosMain/kotlin/com/hanmaum/dn/mobile/core/push/IosPushManager.kt`
- Create: `composeApp/src/iosMain/kotlin/com/hanmaum/dn/mobile/core/push/PushBridge.kt`
- Modify: `composeApp/src/iosMain/kotlin/com/hanmaum/dn/mobile/di/PlatformModule.ios.kt`
- Modify: `iosApp/iosApp/iOSApp.swift`
- Modify: `iosApp/iosApp/*.entitlements` (aps-environment)

**Interfaces:**
- Consumes: B7 types; Swift calls Kotlin via `PushBridgeKt.*` (top-level Kotlin functions export under `<FileName>Kt`; names must NOT start with `init`).
- Produces: `single<PushManager> { IosPushManager() }`.

**HUMAN CHECKPOINT (before this task's E2E, not before coding):** in Xcode — File → Add Package Dependencies → `https://github.com/firebase/firebase-ios-sdk` → add product **FirebaseMessaging** to the `iosApp` target; drop `GoogleService-Info.plist` into the target; Signing & Capabilities → + Capability → **Push Notifications**. The Swift code below compiles ONLY after the SPM package is added — coordinate with the user, or pause this task at Step 3 and request it.

- [ ] **Step 1: Kotlin bridge + manager (compiles without Firebase — Kotlin never imports it)**

```kotlin
// core/push/PushBridge.kt (iosMain) — called from Swift
package com.hanmaum.dn.mobile.core.push

// Single writer (Swift main thread); plain var is sufficient.
private var latestFcmToken: String? = null

fun handlePushToken(token: String) {
    latestFcmToken = token
    PushEventBus.tokenRefreshes.tryEmit(token)
}

fun handlePushTap(data: Map<Any?, *>) {
    val stringData = buildMap {
        data.forEach { (k, v) -> if (k is String && v is String) put(k, v) }
    }
    parsePushTap(stringData)?.let { PushEventBus.notificationTaps.tryEmit(it) }
}

internal fun storedFcmToken(): String? = latestFcmToken
```

(Delete the `SynchronizedObject` import if unused — no dead imports. Swift sees these as `PushBridgeKt.handlePushToken(token:)` / `PushBridgeKt.handlePushTap(data:)`.)

```kotlin
// core/push/IosPushManager.kt (iosMain)
package com.hanmaum.dn.mobile.core.push

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

class IosPushManager : PushManager {
    override val platform: String = "IOS"

    // Token arrives via MessagingDelegate in Swift -> PushBridge.handlePushToken.
    override suspend fun currentToken(): String? = storedFcmToken()

    override fun isPermissionGranted(): Boolean {
        // Synchronous best-effort snapshot; authoritative state flows through requestPermission.
        var granted = false
        val semaphore = platform.darwin.dispatch_semaphore_create(0)
        UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
            granted = settings?.authorizationStatus == UNAuthorizationStatusAuthorized
            platform.darwin.dispatch_semaphore_signal(semaphore)
        }
        platform.darwin.dispatch_semaphore_wait(semaphore, platform.darwin.dispatch_time(platform.darwin.DISPATCH_TIME_NOW, 500_000_000))
        return granted
    }

    override suspend fun requestPermission(): Boolean = suspendCancellableCoroutine { cont ->
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound,
        ) { granted, _ -> cont.resume(granted) }
    }
}
```

(If the semaphore-based sync check feels wrong at implementation time, change `PushManager.isPermissionGranted()` to `suspend fun` across all call sites instead — do NOT ship a busy-wait longer than 500ms.)

`PlatformModule.ios.kt`: `single<PushManager> { IosPushManager() }`.

- [ ] **Step 2: Swift AppDelegate**

Replace `iosApp/iosApp/iOSApp.swift` content:

```swift
import SwiftUI
import ComposeApp
import FirebaseCore
import FirebaseMessaging
import UserNotifications

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        if Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil {
            FirebaseApp.configure()
            Messaging.messaging().delegate = self
        }
        UNUserNotificationCenter.current().delegate = self
        application.registerForRemoteNotifications()
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        if let token = fcmToken {
            PushBridgeKt.handlePushToken(token: token)
        }
    }

    // Foreground presentation: show banner + sound + badge.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .badge])
    }

    // Tap on a notification (foreground, background, or cold start).
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        PushBridgeKt.handlePushTap(data: response.notification.request.content.userInfo)
        completionHandler()
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    init() {
        KoinHelperKt.doInitKoinIos()
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

- [ ] **Step 3: Entitlement** — in each `iosApp/iosApp/*.entitlements` (there may be one per config): add

```xml
    <key>aps-environment</key>
    <string>development</string>
```

(Archive signing flips it to `production` automatically with the App Store profile; Push capability was auto-satisfiable per lessons.md §TestFlight.2.)

- [ ] **Step 4: Verify — ALL THREE gates**

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:iosSimulatorArm64Test
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild build \
  -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO
./gradlew :composeApp:testDevDebugUnitTest
```

The xcodebuild step FAILS until the human checkpoint (SPM + plist) is done — if so, report it as blocked-on-user, do not work around it by removing the Firebase imports.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/iosMain iosApp
git commit -m "feat(push): ios firebase messaging appdelegate bridge and push manager"
```

### Task B10: Token lifecycle, deep links, logout cleanup

**Files:**
- Modify: `$M/features/announcement/presentation/HomeViewModel.kt` (+ its test)
- Modify: `$M/features/announcement/presentation/HomeScreen.kt`
- Modify: `$M/App.kt`
- Modify: `$M/features/profile/presentation/ProfileViewModel.kt`
- Modify: `$M/di/AppModule.kt`

**Interfaces:**
- Consumes: `PushManager`, `PushEventBus`, `parsePushTap` (B7/B8/B9), `NotificationRepository` (B2).
- Produces: registration-on-home, tap → `AnnouncementDetailRoute` + `markRead`, logout token deletion.

- [ ] **Step 1: Registration + refresh in HomeViewModel (TDD)**

Test (extends B4's file; each test file owns its fakes — add to this file's `FakeNotificationRepository` a `val registeredTokens = mutableListOf<Pair<String, String>>()` recorded by `registerDeviceToken`, and a `FakePushManager(private val token: String?) : PushManager` with `platform = "ANDROID"`, `currentToken() = token`, `isPermissionGranted() = true`, `requestPermission() = true`):

```kotlin
    @Test
    fun `registers device token on load when available`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository()
        val vm = HomeViewModel(FakeAnnouncementRepository(), repo, FakePushManager(token = "tok1"))
        vm.loadAnnouncements(); advanceUntilIdle()
        assertEquals(listOf("tok1" to "ANDROID"), repo.registeredTokens)
    }

    @Test
    fun `null token skips registration`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository()
        val vm = HomeViewModel(FakeAnnouncementRepository(), repo, FakePushManager(token = null))
        vm.loadAnnouncements(); advanceUntilIdle()
        assertTrue(repo.registeredTokens.isEmpty())
    }
```

Implementation — `HomeViewModel` gains `private val pushManager: PushManager`; in `init`:

```kotlin
    init {
        viewModelScope.launch {
            PushEventBus.tokenRefreshes.collect { token ->
                notificationRepository.registerDeviceToken(token, pushManager.platform)
            }
        }
    }
    // and inside loadAnnouncements(), fire-and-forget once per process (guard with a private var registered = false):
    private var tokenRegistered = false
    private fun registerTokenIfNeeded() {
        if (tokenRegistered) return
        viewModelScope.launch {
            pushManager.currentToken()?.let { token ->
                notificationRepository.registerDeviceToken(token, pushManager.platform)
                    .onSuccess { tokenRegistered = true }
            }
        }
    }
```

Call `registerTokenIfNeeded()` at the top of `loadAnnouncements()`. Koin: `viewModel { HomeViewModel(repository = get(), notificationRepository = get(), pushManager = get()) }`.

- [ ] **Step 2: Deep-link collection on Home**

In `HomeScreen` (composable scope, next to the existing `LaunchedEffect`s), with new parameter `onOpenAnnouncementDeepLink: (String) -> Unit` wired in `App.kt` to `navController.navigate(AnnouncementDetailRoute(it))`:

```kotlin
    val notificationRepository = koinInject<NotificationRepository>()
    LaunchedEffect(Unit) {
        PushEventBus.notificationTaps.collect { payload ->
            if (payload.referenceType == "ANNOUNCEMENT" && payload.referencePublicId != null) {
                payload.notificationPublicId?.let { notificationRepository.markRead(it) }
                onOpenAnnouncementDeepLink(payload.referencePublicId)
            }
            PushEventBus.consumeTap()
        }
    }
```

(`markRead` inside a collect on a composition scope: wrap in `launch` if `markRead` is suspend — it is; use `coroutineScope`/the LaunchedEffect scope directly, it's already a coroutine.)

- [ ] **Step 3: Logout cleanup** — in `ProfileViewModel.logout()`, BEFORE clearing tokens/credentials (inject `NotificationRepository` + `PushManager`, update Koin binding):

```kotlin
            // Best-effort: stop push to this device for the signed-out account.
            pushManager.currentToken()?.let { notificationRepository.deleteDeviceToken(it) }
```

(Inside the existing logout coroutine; a failure must not block logout — `deleteDeviceToken` already returns `Result`, ignore it.)

- [ ] **Step 4: All gates + commit**

```bash
./gradlew :composeApp:testDevDebugUnitTest
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:iosSimulatorArm64Test
git add -A composeApp/src/commonMain composeApp/src/commonTest composeApp/src/androidMain composeApp/src/iosMain
git commit -m "feat(push): token lifecycle deep links and logout token cleanup"
```

### Task B11: Full verification, MVP.md, PR

**Files:**
- Modify: `../dn-app/MVP.md` (status only — never delete rows)

- [ ] **Step 1: Full §7 gate**

```bash
./gradlew :composeApp:testDevDebugUnitTest
grep -rn "TODO" composeApp/src && echo "FAIL: remove TODOs" || echo "TODO gate clean"
./gradlew lint          # only the 3 pre-existing geofence errors allowed
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:iosSimulatorArm64Test
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild build \
  -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO
```

- [ ] **Step 2: E2E on Android emulator** (needs Part A deployed or run locally + `google-services.json`):
  create an announcement via the admin web app → push arrives (app backgrounded AND foregrounded) → bell badge increments on Home → open screen: badge clears, unread dots correct, groups correct → tap row: routes to announcement detail, dot cleared on return → 모두 읽음 works and disables itself → Settings: toggle push off → new announcement: NO push, row still appears with badge. Screenshot light + dark.

- [ ] **Step 3: iOS simulator run** (CLAUDE.md §4 script): app launches, bell/badge/screen/settings all work against the real backend. Real APNs push CANNOT arrive in the simulator — verify the push path itself on a TestFlight build later; note as residual risk in the PR body.

- [ ] **Step 4: MVP.md + PR**

Update the notifications row in `../dn-app/MVP.md` (status only). Then:

```bash
git push -u origin feature/notifications
gh pr create --base develop --title "feat(notification): push and in-app bell notifications" --body "<why/what/how-tested with ✅/⚠️/🔧/📋/🚫 markers, screenshots both platforms, residual risk: simulator cannot receive APNs — device TestFlight verification pending>"
gh pr checks <n> --watch --fail-fast && gh pr merge <n> --squash --delete-branch
```

## Task order / dependencies

A1 → A2 → A3 → A4 → A5 → A6 (server, strictly sequential).
B1 → B2 → B3 → B4 → B5 → B6 → B7 → B8 → B9 → B10 → B11 (B7 before B8/B9; B4+B5 land together before manual testing; B9 has a human checkpoint).
Parts A and B are independent until B11's E2E, which needs A deployed (or run locally).
