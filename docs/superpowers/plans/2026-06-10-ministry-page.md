# Ministry Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Re-introduce the Ministry (사역) feature as an informational directory + detail experience, reachable as a bottom-nav tab, redesigned to Warm Premium v2 and realigned to the real backend contract — with the old registration/apply flow removed.

**Architecture:** Clean architecture under `features/ministry/` (domain → data → presentation). The feature already exists as dead "Luminous Sanctuary" code wired into `App.kt`, DI, and `Routes.kt`; we rewrite it in place. The data layer is realigned to the actual API (`title`/`subtitle`/`about`/`imageUrl`/`active` + `requirements`/`schedules`/`contacts`, all under an `ApiResponse.data` wrapper). The bottom-nav `News` tab is replaced by `Ministry`; News content stays reachable via Home → 전체보기.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform (Material3), Ktor 3 (`ApiResponse<T>` wrapper), Koin, `kotlinx-serialization`, `androidx.lifecycle` ViewModel + StateFlow. Tests: `kotlin.test` + Ktor `MockEngine` + `kotlinx-coroutines-test` (`StandardTestDispatcher`), run via `:composeApp:testDevDebugUnitTest`.

> **Sequencing note (read first):** This is a full rewrite of a tightly-coupled feature. Every file under `features/ministry/` references the domain types, so `commonMain` will NOT compile until the whole slice (Tasks 1–8) is migrated. Tasks 1–8 are implementation tasks that each end with the code in place; **Task 9 is a compile gate**; Tasks 10–14 add and run the unit tests (test code is fully specified — no placeholders). Implement in order.

---

## File Structure

**Rewritten:**
- `features/ministry/domain/model/Ministry.kt` — `Ministry`, `MinistryDetail`, `Schedule`, `Contact` (registration types deleted)
- `features/ministry/domain/repository/MinistryRepository.kt` — 2 read methods only
- `features/ministry/data/model/MinistrySummaryResponse.kt` — summary DTO
- `features/ministry/data/model/MinistryDetailResponse.kt` — detail DTO
- `features/ministry/data/repository/MinistryRepositoryImpl.kt` — 2 methods + mappers
- `features/ministry/presentation/list/{MinistryListUiState,MinistryListViewModel,MinistryListScreen}.kt`
- `features/ministry/presentation/detail/{MinistryDetailUiState,MinistryDetailViewModel,MinistryDetailScreen}.kt`
- `core/navigation/TopLevelDestination.kt` — News → Ministry
- `core/i18n/AppStrings.kt` — remove 6 old ministry strings, add ministry list/section strings

**Created:**
- `features/ministry/data/model/ScheduleResponse.kt`
- `features/ministry/data/model/ContactResponse.kt`
- `features/ministry/data/repository/MinistryRepositoryImplTest.kt` (test)
- `features/ministry/MinistryListViewModelTest.kt` (test)
- `features/ministry/MinistryDetailViewModelTest.kt` (test)

**Deleted:**
- `features/ministry/data/model/CreateRegistrationRequest.kt`
- `features/ministry/data/model/RegistrationResponse.kt`

**Edited:**
- `App.kt` — drop `onBackClick` on the ministry-list composable
- `core/navigation/TopLevelDestinationTest.kt` — News → Ministry assertions
- `core/i18n/AppStringsTest.kt` — add ministry-string assertions

**Unchanged (verified):** `di/AppModule.kt` (binding shapes already match: `MinistryRepositoryImpl(get())`, `MinistryListViewModel(get())`, `MinistryDetailViewModel(publicId, get())`), `core/navigation/Routes.kt` (`MinistryListRoute`, `MinistryDetailRoute` already exist), the `AnnouncementListRoute` composable in `App.kt`.

---

## Task 1: Domain models + repository interface

**Files:**
- Modify (replace): `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/ministry/domain/model/Ministry.kt`
- Modify (replace): `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/ministry/domain/repository/MinistryRepository.kt`

- [ ] **Step 1: Replace the domain models**

Replace the entire contents of `Ministry.kt` with:

```kotlin
package com.hanmaum.dn.mobile.features.ministry.domain.model

data class Ministry(
    val publicId: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val contacts: List<Contact>,
    val isActive: Boolean,
)

data class MinistryDetail(
    val publicId: String,
    val title: String,
    val subtitle: String,
    val about: String,
    val requirements: List<String>,
    val schedules: List<Schedule>,
    val contacts: List<Contact>,
    val imageUrl: String?,
    val isActive: Boolean,
)

data class Schedule(
    val description: String,
    val startTime: String,
    val endTime: String,
)

data class Contact(
    val role: String,
    val name: String,
)
```

- [ ] **Step 2: Replace the repository interface**

Replace the entire contents of `MinistryRepository.kt` with:

```kotlin
package com.hanmaum.dn.mobile.features.ministry.domain.repository

import com.hanmaum.dn.mobile.features.ministry.domain.model.Ministry
import com.hanmaum.dn.mobile.features.ministry.domain.model.MinistryDetail

interface MinistryRepository {
    suspend fun getMinistries(activeOnly: Boolean = true): Result<List<Ministry>>
    suspend fun getMinistryDetail(publicId: String): Result<MinistryDetail>
}
```

> No build/test here — the module will not compile until Task 8. Continue.

---

## Task 2: DTOs (and delete registration DTOs)

**Files:**
- Modify (replace): `.../features/ministry/data/model/MinistrySummaryResponse.kt`
- Modify (replace): `.../features/ministry/data/model/MinistryDetailResponse.kt`
- Create: `.../features/ministry/data/model/ScheduleResponse.kt`
- Create: `.../features/ministry/data/model/ContactResponse.kt`
- Delete: `.../features/ministry/data/model/CreateRegistrationRequest.kt`
- Delete: `.../features/ministry/data/model/RegistrationResponse.kt`

- [ ] **Step 1: Replace `MinistrySummaryResponse.kt`**

```kotlin
package com.hanmaum.dn.mobile.features.ministry.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MinistrySummaryResponse(
    val publicId: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String? = null,
    val contacts: List<ContactResponse> = emptyList(),
    val active: Boolean,
)
```

- [ ] **Step 2: Replace `MinistryDetailResponse.kt`**

```kotlin
package com.hanmaum.dn.mobile.features.ministry.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MinistryDetailResponse(
    val publicId: String,
    val title: String,
    val subtitle: String,
    val about: String,
    val requirements: List<String> = emptyList(),
    val schedules: List<ScheduleResponse> = emptyList(),
    val contacts: List<ContactResponse> = emptyList(),
    val imageUrl: String? = null,
    val active: Boolean,
)
```

- [ ] **Step 3: Create `ScheduleResponse.kt`**

```kotlin
package com.hanmaum.dn.mobile.features.ministry.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleResponse(
    val description: String,
    val startTime: String,
    val endTime: String,
)
```

- [ ] **Step 4: Create `ContactResponse.kt`**

```kotlin
package com.hanmaum.dn.mobile.features.ministry.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ContactResponse(
    val role: String,
    val name: String,
)
```

- [ ] **Step 5: Delete the registration DTOs**

Run:
```bash
git rm composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/ministry/data/model/CreateRegistrationRequest.kt \
       composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/ministry/data/model/RegistrationResponse.kt
```

---

## Task 3: Repository implementation

**Files:**
- Modify (replace): `.../features/ministry/data/repository/MinistryRepositoryImpl.kt`

- [ ] **Step 1: Replace `MinistryRepositoryImpl.kt`**

```kotlin
package com.hanmaum.dn.mobile.features.ministry.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.ministry.data.model.ContactResponse
import com.hanmaum.dn.mobile.features.ministry.data.model.MinistryDetailResponse
import com.hanmaum.dn.mobile.features.ministry.data.model.MinistrySummaryResponse
import com.hanmaum.dn.mobile.features.ministry.data.model.ScheduleResponse
import com.hanmaum.dn.mobile.features.ministry.domain.model.Contact
import com.hanmaum.dn.mobile.features.ministry.domain.model.Ministry
import com.hanmaum.dn.mobile.features.ministry.domain.model.MinistryDetail
import com.hanmaum.dn.mobile.features.ministry.domain.model.Schedule
import com.hanmaum.dn.mobile.features.ministry.domain.repository.MinistryRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class MinistryRepositoryImpl(
    private val client: HttpClient,
) : MinistryRepository {

    override suspend fun getMinistries(activeOnly: Boolean): Result<List<Ministry>> = runCatching {
        val response = client.get("ministries?active=$activeOnly")
        val body = response.body<ApiResponse<List<MinistrySummaryResponse>>>()
        body.data?.map { it.toDomain() } ?: emptyList()
    }

    override suspend fun getMinistryDetail(publicId: String): Result<MinistryDetail> = runCatching {
        val response = client.get("ministries/$publicId")
        val body = response.body<ApiResponse<MinistryDetailResponse>>()
        body.data?.toDomain() ?: error("Ministry detail data is null")
    }

    // ─── Mappers ─────────────────────────────────────────────────────────────

    private fun MinistrySummaryResponse.toDomain() = Ministry(
        publicId = publicId,
        title = title,
        subtitle = subtitle,
        imageUrl = imageUrl,
        contacts = contacts.map { it.toDomain() },
        isActive = active,
    )

    private fun MinistryDetailResponse.toDomain() = MinistryDetail(
        publicId = publicId,
        title = title,
        subtitle = subtitle,
        about = about,
        requirements = requirements,
        schedules = schedules.map { it.toDomain() },
        contacts = contacts.map { it.toDomain() },
        imageUrl = imageUrl,
        isActive = active,
    )

    private fun ScheduleResponse.toDomain() = Schedule(
        description = description,
        startTime = startTime,
        endTime = endTime,
    )

    private fun ContactResponse.toDomain() = Contact(
        role = role,
        name = name,
    )
}
```

---

## Task 4: List presentation logic (UiState + ViewModel)

**Files:**
- Modify (replace): `.../features/ministry/presentation/list/MinistryListUiState.kt`
- Modify (replace): `.../features/ministry/presentation/list/MinistryListViewModel.kt`

- [ ] **Step 1: Replace `MinistryListUiState.kt`** (shape unchanged; rewrite to drop any stale imports)

```kotlin
package com.hanmaum.dn.mobile.features.ministry.presentation.list

import com.hanmaum.dn.mobile.features.ministry.domain.model.Ministry

sealed class MinistryListUiState {
    object Loading : MinistryListUiState()
    data class Success(val ministries: List<Ministry>) : MinistryListUiState()
    data class Error(val message: String) : MinistryListUiState()
}
```

- [ ] **Step 2: Replace `MinistryListViewModel.kt`** (only the fallback message text changes 부서 → 사역)

```kotlin
package com.hanmaum.dn.mobile.features.ministry.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.ministry.domain.repository.MinistryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MinistryListViewModel(
    private val repository: MinistryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MinistryListUiState>(MinistryListUiState.Loading)
    val uiState: StateFlow<MinistryListUiState> = _uiState.asStateFlow()

    init {
        loadMinistries()
    }

    fun loadMinistries() {
        viewModelScope.launch {
            _uiState.value = MinistryListUiState.Loading
            repository.getMinistries(activeOnly = true).fold(
                onSuccess = { _uiState.value = MinistryListUiState.Success(it) },
                onFailure = { _uiState.value = MinistryListUiState.Error(it.message ?: "사역 목록 로딩 실패") },
            )
        }
    }
}
```

---

## Task 5: Detail presentation logic (UiState + ViewModel)

**Files:**
- Modify (replace): `.../features/ministry/presentation/detail/MinistryDetailUiState.kt`
- Modify (replace): `.../features/ministry/presentation/detail/MinistryDetailViewModel.kt`

- [ ] **Step 1: Replace `MinistryDetailUiState.kt`** (drop all registration state)

```kotlin
package com.hanmaum.dn.mobile.features.ministry.presentation.detail

import com.hanmaum.dn.mobile.features.ministry.domain.model.MinistryDetail

sealed class MinistryDetailUiState {
    object Loading : MinistryDetailUiState()
    data class Success(val detail: MinistryDetail) : MinistryDetailUiState()
    data class Error(val message: String) : MinistryDetailUiState()
}
```

- [ ] **Step 2: Replace `MinistryDetailViewModel.kt`** (single detail fetch; no registration)

```kotlin
package com.hanmaum.dn.mobile.features.ministry.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanmaum.dn.mobile.features.ministry.domain.repository.MinistryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MinistryDetailViewModel(
    private val publicId: String,
    private val repository: MinistryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MinistryDetailUiState>(MinistryDetailUiState.Loading)
    val uiState: StateFlow<MinistryDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = MinistryDetailUiState.Loading
            repository.getMinistryDetail(publicId).fold(
                onSuccess = { _uiState.value = MinistryDetailUiState.Success(it) },
                onFailure = { _uiState.value = MinistryDetailUiState.Error(it.message ?: "사역 정보 로딩 실패") },
            )
        }
    }
}
```

---

## Task 6: i18n strings

**Files:**
- Modify: `core/i18n/AppStrings.kt` (interface + `EnStrings` + `KoStrings` + `DeStrings`)

- [ ] **Step 1: Update the `AppStrings` interface — replace the `// Ministry` block**

Find in the interface (around line 50):
```kotlin
    // Ministry
    val registerNow: String
    val registered: String
    val alreadyMember: String
    val bioLabel: String
    val bioPlaceholder: String
```
Replace it with:
```kotlin
    // Ministry
    val ministryListTitle: String
    val ministryListSubtitle: String
    val ministryListEmpty: String
    val ministryAbout: String
    val ministryRequirements: String
    val ministrySchedule: String
    val ministryContact: String
```
Also remove the standalone interface line `val ministryRegisterSheet: String` (around line 87).

- [ ] **Step 2: `EnStrings` — replace its `// Ministry` overrides**

Find:
```kotlin
    override val registerNow = "Register Now"
    override val registered = "Applied ✓"
    override val alreadyMember = "Already a member ✓"
    override val bioLabel = "Bio (optional)"
    override val bioPlaceholder = "Introduce yourself to the leader"
```
Replace with:
```kotlin
    override val ministryListTitle = "Ministries"
    override val ministryListSubtitle = "Discover the ministries serving our church"
    override val ministryListEmpty = "No ministries yet"
    override val ministryAbout = "About"
    override val ministryRequirements = "Requirements"
    override val ministrySchedule = "Schedule"
    override val ministryContact = "Contact"
```
And remove the `EnStrings` line `override val ministryRegisterSheet = "Ministry Registration"`.

- [ ] **Step 3: `KoStrings` — replace its `// Ministry` overrides**

Find:
```kotlin
    override val registerNow = "신청하기"
    override val registered = "신청되었습니다 ✓"
    override val alreadyMember = "멤버입니다 ✓"
    override val bioLabel = "자기소개 (선택)"
    override val bioPlaceholder = "리더에게 전달할 자기소개를 입력하세요"
```
Replace with:
```kotlin
    override val ministryListTitle = "사역"
    override val ministryListSubtitle = "교회의 다양한 사역을 만나보세요"
    override val ministryListEmpty = "등록된 사역이 없습니다"
    override val ministryAbout = "소개"
    override val ministryRequirements = "지원 자격"
    override val ministrySchedule = "일정"
    override val ministryContact = "문의"
```
And remove the `KoStrings` line `override val ministryRegisterSheet = "부서 신청"`.

- [ ] **Step 4: `DeStrings` — replace its `// Ministry` overrides**

Find:
```kotlin
    override val registerNow = "Jetzt anmelden"
    override val registered = "Angemeldet ✓"
    override val alreadyMember = "Bereits Mitglied ✓"
    override val bioLabel = "Biografie (optional)"
    override val bioPlaceholder = "Stellen Sie sich dem Leiter vor"
```
Replace with:
```kotlin
    override val ministryListTitle = "Dienste"
    override val ministryListSubtitle = "Entdecken Sie die Dienste unserer Gemeinde"
    override val ministryListEmpty = "Noch keine Dienste"
    override val ministryAbout = "Über uns"
    override val ministryRequirements = "Voraussetzungen"
    override val ministrySchedule = "Zeitplan"
    override val ministryContact = "Kontakt"
```
And remove the `DeStrings` line `override val ministryRegisterSheet = "Abteilungsanmeldung"`.

---

## Task 7: List screen (Compose, Warm Premium — Option A)

**Files:**
- Modify (replace): `.../features/ministry/presentation/list/MinistryListScreen.kt`

- [ ] **Step 1: Replace `MinistryListScreen.kt`**

```kotlin
package com.hanmaum.dn.mobile.features.ministry.presentation.list

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.ErrorView
import com.hanmaum.dn.mobile.features.ministry.domain.model.Ministry
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MinistryListScreen(
    onMinistryClick: (String) -> Unit,
) {
    val viewModel: MinistryListViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (val s = state) {
            is MinistryListUiState.Loading ->
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is MinistryListUiState.Error ->
                ErrorView(msg = s.message, onRetry = { viewModel.loadMinistries() })
            is MinistryListUiState.Success ->
                MinistryListContent(ministries = s.ministries, onMinistryClick = onMinistryClick)
        }
    }
}

@Composable
private fun MinistryListContent(
    ministries: List<Ministry>,
    onMinistryClick: (String) -> Unit,
) {
    val strings = LocalStrings.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(
                    text = strings.ministryListTitle,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = strings.ministryListSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        if (ministries.isEmpty()) {
            item {
                Text(
                    text = strings.ministryListEmpty,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        } else {
            items(ministries, key = { it.publicId }) { ministry ->
                MinistryCard(ministry = ministry, onClick = { onMinistryClick(ministry.publicId) })
            }
        }
    }
}

@Composable
private fun MinistryCard(
    ministry: Ministry,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh),
        label = "ministryCardScale",
    )

    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer,
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.VolunteerActivism,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ministry.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = ministry.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
```

> Note: list-stagger entry animation (DESIGN §8) is intentionally deferred as optional polish; the card press-scale spring is the in-scope motion. Add stagger later if desired.

---

## Task 8: Detail screen (Compose, sectioned)

**Files:**
- Modify (replace): `.../features/ministry/presentation/detail/MinistryDetailScreen.kt`

- [ ] **Step 1: Replace `MinistryDetailScreen.kt`**

```kotlin
package com.hanmaum.dn.mobile.features.ministry.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.ErrorView
import com.hanmaum.dn.mobile.features.ministry.domain.model.Contact
import com.hanmaum.dn.mobile.features.ministry.domain.model.MinistryDetail
import com.hanmaum.dn.mobile.features.ministry.domain.model.Schedule
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MinistryDetailScreen(
    publicId: String,
    onBackClick: () -> Unit,
) {
    val viewModel: MinistryDetailViewModel = koinViewModel(parameters = { parametersOf(publicId) })
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (val s = state) {
            is MinistryDetailUiState.Loading ->
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is MinistryDetailUiState.Error ->
                ErrorView(msg = s.message, onRetry = { viewModel.load() })
            is MinistryDetailUiState.Success ->
                MinistryDetailContent(detail = s.detail, onBackClick = onBackClick)
        }
    }
}

@Composable
private fun MinistryDetailContent(
    detail: MinistryDetail,
    onBackClick: () -> Unit,
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Hero: ministry image when present, gradient fallback otherwise
        Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            if (detail.imageUrl != null) {
                AsyncImage(
                    model = detail.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer,
                                )
                            )
                        ),
                )
            }
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = strings.back,
                    tint = Color.White,
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(18.dp))
            Text(
                text = detail.title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (detail.subtitle.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = detail.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (detail.about.isNotBlank()) {
                Section(title = strings.ministryAbout) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        detail.about.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                            .forEach { para ->
                                Text(
                                    text = para,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                    }
                }
            }

            if (detail.requirements.isNotEmpty()) {
                Section(title = strings.ministryRequirements) {
                    Column {
                        detail.requirements.forEachIndexed { index, req ->
                            if (index > 0) HairlineSpacer()
                            Text(
                                text = req,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 9.dp),
                            )
                        }
                    }
                }
            }

            if (detail.schedules.isNotEmpty()) {
                Section(title = strings.ministrySchedule) {
                    Column {
                        detail.schedules.forEachIndexed { index, schedule ->
                            if (index > 0) HairlineSpacer()
                            ScheduleRow(schedule)
                        }
                    }
                }
            }

            if (detail.contacts.isNotEmpty()) {
                Section(title = strings.ministryContact) {
                    Column {
                        detail.contacts.forEachIndexed { index, contact ->
                            if (index > 0) HairlineSpacer()
                            ContactRow(contact)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(22.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(10.dp))
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) { content() }
    }
}

@Composable
private fun HairlineSpacer() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun ScheduleRow(schedule: Schedule) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = schedule.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.size(12.dp))
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        ) {
            Text(
                text = "${schedule.startTime}–${schedule.endTime}",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun ContactRow(contact: Contact) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = contact.role,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = contact.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
```

---

## Task 9: Nav swap + App wiring (COMPILE GATE)

**Files:**
- Modify (replace): `core/navigation/TopLevelDestination.kt`
- Modify: `App.kt` (ministry-list composable)

- [ ] **Step 1: Replace `TopLevelDestination.kt`** (News → Ministry)

```kotlin
package com.hanmaum.dn.mobile.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.reflect.KClass

sealed class TopLevelDestination<T : Any>(
    val routeClass: KClass<T>,
    val routeInstance: T,
    val icon: ImageVector,
    val label: String,
) {
    data object Home : TopLevelDestination<HomeRoute>(
        routeClass = HomeRoute::class,
        routeInstance = HomeRoute,
        icon = Icons.Default.Home,
        label = "홈",
    )

    data object Ministry : TopLevelDestination<MinistryListRoute>(
        routeClass = MinistryListRoute::class,
        routeInstance = MinistryListRoute,
        icon = Icons.Default.VolunteerActivism,
        label = "사역",
    )

    data object Calendar : TopLevelDestination<CalendarRoute>(
        routeClass = CalendarRoute::class,
        routeInstance = CalendarRoute,
        icon = Icons.Default.CalendarMonth,
        label = "캘린더",
    )

    data object Album : TopLevelDestination<AlbumsRoute>(
        routeClass = AlbumsRoute::class,
        routeInstance = AlbumsRoute,
        icon = Icons.Default.PhotoLibrary,
        label = "앨범",
    )

    data object Profile : TopLevelDestination<ProfileRoute>(
        routeClass = ProfileRoute::class,
        routeInstance = ProfileRoute,
        icon = Icons.Default.Person,
        label = "프로필",
    )

    companion object {
        val all: List<TopLevelDestination<*>> = listOf(Home, Ministry, Calendar, Album, Profile)
    }
}
```

- [ ] **Step 2: Update the ministry-list composable in `App.kt`**

Find:
```kotlin
                    composable<MinistryListRoute> {
                        MinistryListScreen(
                            onBackClick     = { navController.popBackStack() },
                            onMinistryClick = { publicId ->
                                navController.navigate(MinistryDetailRoute(publicId = publicId))
                            },
                        )
                    }
```
Replace with:
```kotlin
                    composable<MinistryListRoute> {
                        MinistryListScreen(
                            onMinistryClick = { publicId ->
                                navController.navigate(MinistryDetailRoute(publicId = publicId))
                            },
                        )
                    }
```

- [ ] **Step 3: COMPILE GATE — assemble the dev debug variant**

Run: `./gradlew :composeApp:assembleDevDebug`
Expected: `BUILD SUCCESSFUL`. If it fails, fix unresolved references (most likely a missing import in Task 7/8) before continuing.

- [ ] **Step 4: Commit the feature implementation**

```bash
git add -A
git commit -m "feat(ministry): informational ministry directory + detail, bottom-nav tab"
```

---

## Task 10: Repository test (MockEngine)

**Files:**
- Create: `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/ministry/data/repository/MinistryRepositoryImplTest.kt`

- [ ] **Step 1: Write the test**

```kotlin
package com.hanmaum.dn.mobile.features.ministry.data.repository

import com.hanmaum.dn.mobile.core.domain.model.ApiResponse
import com.hanmaum.dn.mobile.features.ministry.data.model.ContactResponse
import com.hanmaum.dn.mobile.features.ministry.data.model.MinistryDetailResponse
import com.hanmaum.dn.mobile.features.ministry.data.model.MinistrySummaryResponse
import com.hanmaum.dn.mobile.features.ministry.data.model.ScheduleResponse
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val testJson = Json { ignoreUnknownKeys = true }

private fun mockClient(
    responseJson: String,
    onRequest: ((HttpRequestData) -> Unit)? = null,
): HttpClient = HttpClient(MockEngine { request ->
    onRequest?.invoke(request)
    respond(
        content = responseJson,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )
}) {
    install(ContentNegotiation) { json(testJson) }
    defaultRequest {
        if (url.host.isBlank()) {
            val path = url.encodedPath.removePrefix("/")
            url.takeFrom("http://localhost")
            url.encodedPath = "/$path"
        }
    }
}

class MinistryRepositoryImplTest {

    private val summary = MinistrySummaryResponse(
        publicId = "m1",
        title = "난민 사역",
        subtitle = "하나님의 사랑을 나누는 사역",
        imageUrl = null,
        contacts = listOf(ContactResponse(role = "팀장", name = "김영원 권사님")),
        active = true,
    )

    private val detail = MinistryDetailResponse(
        publicId = "m1",
        title = "난민 사역",
        subtitle = "하나님의 사랑을 나누는 사역",
        about = "이 사역은...",
        requirements = listOf("큐베세 양육 수료자"),
        schedules = listOf(ScheduleResponse(description = "넷째 주 토요일", startTime = "07:00", endTime = "09:00")),
        contacts = listOf(
            ContactResponse(role = "팀장", name = "김영원 권사님"),
            ContactResponse(role = "간사", name = "최혜령 자매님"),
        ),
        imageUrl = null,
        active = true,
    )

    @Test
    fun getMinistries_mapsSummaryFields() = runTest {
        val json = testJson.encodeToString(ApiResponse(success = true, data = listOf(summary)))
        val result = MinistryRepositoryImpl(mockClient(json)).getMinistries()

        val ministries = result.getOrThrow()
        assertEquals(1, ministries.size)
        val m = ministries[0]
        assertEquals("m1", m.publicId)
        assertEquals("난민 사역", m.title)
        assertEquals("하나님의 사랑을 나누는 사역", m.subtitle)
        assertEquals(true, m.isActive)
        assertEquals(1, m.contacts.size)
        assertEquals("팀장", m.contacts[0].role)
        assertEquals("김영원 권사님", m.contacts[0].name)
    }

    @Test
    fun getMinistries_emptyData_returnsEmptyList() = runTest {
        val json = testJson.encodeToString(
            ApiResponse(success = true, data = emptyList<MinistrySummaryResponse>())
        )
        val result = MinistryRepositoryImpl(mockClient(json)).getMinistries()
        assertEquals(0, result.getOrThrow().size)
    }

    @Test
    fun getMinistries_requestsCorrectPathAndQuery() = runTest {
        var path = ""
        var active = ""
        val json = testJson.encodeToString(
            ApiResponse(success = true, data = emptyList<MinistrySummaryResponse>())
        )
        val client = mockClient(json) { req ->
            path = req.url.encodedPath
            active = req.url.parameters["active"] ?: ""
        }
        MinistryRepositoryImpl(client).getMinistries(activeOnly = true)
        assertEquals("/ministries", path)
        assertEquals("true", active)
    }

    @Test
    fun getMinistryDetail_mapsAllNestedFields() = runTest {
        val json = testJson.encodeToString(ApiResponse(success = true, data = detail))
        val result = MinistryRepositoryImpl(mockClient(json)).getMinistryDetail("m1")

        val d = result.getOrThrow()
        assertEquals("이 사역은...", d.about)
        assertEquals(listOf("큐베세 양육 수료자"), d.requirements)
        assertEquals(1, d.schedules.size)
        assertEquals("07:00", d.schedules[0].startTime)
        assertEquals("09:00", d.schedules[0].endTime)
        assertEquals(2, d.contacts.size)
        assertEquals("간사", d.contacts[1].role)
        assertEquals(true, d.isActive)
    }

    @Test
    fun getMinistryDetail_requestsCorrectPath() = runTest {
        var path = ""
        val json = testJson.encodeToString(ApiResponse(success = true, data = detail))
        val client = mockClient(json) { req -> path = req.url.encodedPath }
        MinistryRepositoryImpl(client).getMinistryDetail("m1")
        assertTrue(path.endsWith("/ministries/m1"), "path was $path")
    }
}
```

- [ ] **Step 2: Run the test**

Run: `./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.features.ministry.data.repository.MinistryRepositoryImplTest"`
Expected: PASS (5 tests).

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/ministry/data/repository/MinistryRepositoryImplTest.kt
git commit -m "test(ministry): cover repository DTO→domain mapping"
```

---

## Task 11: List ViewModel test

**Files:**
- Create: `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/ministry/MinistryListViewModelTest.kt`

- [ ] **Step 1: Write the test**

```kotlin
package com.hanmaum.dn.mobile.features.ministry

import com.hanmaum.dn.mobile.features.ministry.domain.model.Ministry
import com.hanmaum.dn.mobile.features.ministry.domain.model.MinistryDetail
import com.hanmaum.dn.mobile.features.ministry.domain.repository.MinistryRepository
import com.hanmaum.dn.mobile.features.ministry.presentation.list.MinistryListUiState
import com.hanmaum.dn.mobile.features.ministry.presentation.list.MinistryListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class MinistryListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private val sample = Ministry(
        publicId = "m1", title = "난민 사역", subtitle = "사랑을 나누는 사역",
        imageUrl = null, contacts = emptyList(), isActive = true,
    )

    private fun repo(result: Result<List<Ministry>>) = object : MinistryRepository {
        override suspend fun getMinistries(activeOnly: Boolean) = result
        override suspend fun getMinistryDetail(publicId: String): Result<MinistryDetail> =
            Result.failure(NotImplementedError())
    }

    @Test
    fun `success emits Success with ministries`() = runTest {
        val vm = MinistryListViewModel(repo(Result.success(listOf(sample))))
        dispatcher.scheduler.advanceUntilIdle()

        val state = assertIs<MinistryListUiState.Success>(vm.uiState.value)
        assertEquals(1, state.ministries.size)
        assertEquals("난민 사역", state.ministries[0].title)
    }

    @Test
    fun `failure emits Error`() = runTest {
        val vm = MinistryListViewModel(repo(Result.failure(RuntimeException("네트워크 오류"))))
        dispatcher.scheduler.advanceUntilIdle()
        assertIs<MinistryListUiState.Error>(vm.uiState.value)
    }
}
```

- [ ] **Step 2: Run the test**

Run: `./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.features.ministry.MinistryListViewModelTest"`
Expected: PASS (2 tests).

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/ministry/MinistryListViewModelTest.kt
git commit -m "test(ministry): cover list ViewModel success/error states"
```

---

## Task 12: Detail ViewModel test

**Files:**
- Create: `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/ministry/MinistryDetailViewModelTest.kt`

- [ ] **Step 1: Write the test**

```kotlin
package com.hanmaum.dn.mobile.features.ministry

import com.hanmaum.dn.mobile.features.ministry.domain.model.Contact
import com.hanmaum.dn.mobile.features.ministry.domain.model.Ministry
import com.hanmaum.dn.mobile.features.ministry.domain.model.MinistryDetail
import com.hanmaum.dn.mobile.features.ministry.domain.repository.MinistryRepository
import com.hanmaum.dn.mobile.features.ministry.presentation.detail.MinistryDetailUiState
import com.hanmaum.dn.mobile.features.ministry.presentation.detail.MinistryDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class MinistryDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private val sample = MinistryDetail(
        publicId = "m1", title = "난민 사역", subtitle = "사랑을 나누는 사역",
        about = "소개", requirements = emptyList(), schedules = emptyList(),
        contacts = listOf(Contact("팀장", "김영원 권사님")), imageUrl = null, isActive = true,
    )

    private fun repo(result: Result<MinistryDetail>) = object : MinistryRepository {
        override suspend fun getMinistries(activeOnly: Boolean): Result<List<Ministry>> =
            Result.success(emptyList())
        override suspend fun getMinistryDetail(publicId: String) = result
    }

    @Test
    fun `success emits Success with detail`() = runTest {
        val vm = MinistryDetailViewModel("m1", repo(Result.success(sample)))
        dispatcher.scheduler.advanceUntilIdle()

        val state = assertIs<MinistryDetailUiState.Success>(vm.uiState.value)
        assertEquals("난민 사역", state.detail.title)
        assertEquals(1, state.detail.contacts.size)
    }

    @Test
    fun `failure emits Error`() = runTest {
        val vm = MinistryDetailViewModel("m1", repo(Result.failure(RuntimeException("오류"))))
        dispatcher.scheduler.advanceUntilIdle()
        assertIs<MinistryDetailUiState.Error>(vm.uiState.value)
    }
}
```

- [ ] **Step 2: Run the test**

Run: `./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.features.ministry.MinistryDetailViewModelTest"`
Expected: PASS (2 tests).

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/features/ministry/MinistryDetailViewModelTest.kt
git commit -m "test(ministry): cover detail ViewModel success/error states"
```

---

## Task 13: Update navigation test

**Files:**
- Modify: `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/core/navigation/TopLevelDestinationTest.kt`

- [ ] **Step 1: Replace the order test and the News test**

Replace the test method:
```kotlin
    @Test
    fun `all lists destinations in Home News Calendar Album Profile order`() {
        val entries = TopLevelDestination.all
        assertTrue(entries[0] is TopLevelDestination.Home)
        assertTrue(entries[1] is TopLevelDestination.News)
        assertTrue(entries[2] is TopLevelDestination.Calendar)
        assertTrue(entries[3] is TopLevelDestination.Album)
        assertTrue(entries[4] is TopLevelDestination.Profile)
    }
```
with:
```kotlin
    @Test
    fun `all lists destinations in Home Ministry Calendar Album Profile order`() {
        val entries = TopLevelDestination.all
        assertTrue(entries[0] is TopLevelDestination.Home)
        assertTrue(entries[1] is TopLevelDestination.Ministry)
        assertTrue(entries[2] is TopLevelDestination.Calendar)
        assertTrue(entries[3] is TopLevelDestination.Album)
        assertTrue(entries[4] is TopLevelDestination.Profile)
    }
```
And replace:
```kotlin
    @Test
    fun `News routeClass is AnnouncementListRoute`() {
        assertEquals(AnnouncementListRoute::class, TopLevelDestination.News.routeClass)
    }
```
with:
```kotlin
    @Test
    fun `Ministry routeClass is MinistryListRoute`() {
        assertEquals(MinistryListRoute::class, TopLevelDestination.Ministry.routeClass)
    }
```

- [ ] **Step 2: Run the test**

Run: `./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.core.navigation.TopLevelDestinationTest"`
Expected: PASS.

---

## Task 14: i18n test + full verification

**Files:**
- Modify: `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/core/i18n/AppStringsTest.kt`

- [ ] **Step 1: Add ministry-string assertions**

Inside the `EnStrings has non-blank values for all keys` test, before its closing brace, add:
```kotlin
        assertTrue(s.ministryListTitle.isNotBlank())
        assertTrue(s.ministryAbout.isNotBlank())
        assertTrue(s.ministryContact.isNotBlank())
```
Inside the `KoStrings has non-blank values for all keys` test, add:
```kotlin
        assertTrue(s.ministryListTitle.isNotBlank())
        assertEquals("소개", s.ministryAbout)
        assertEquals("일정", s.ministrySchedule)
```
Inside the `DeStrings has non-blank values for all keys` test, add:
```kotlin
        assertTrue(s.ministryListTitle.isNotBlank())
        assertEquals("Über uns", s.ministryAbout)
```

- [ ] **Step 2: Run the full unit-test suite**

Run: `./gradlew :composeApp:testDevDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, all tests pass (existing + new ministry tests).

- [ ] **Step 3: Run lint and the TODO grep gate**

Run: `./gradlew lint`
Expected: no new errors. (3 pre-existing geofence/notification errors — `MissingPermission` in `GeofenceManager.android.kt`/`NotificationService.android.kt` and `CoarseFineLocation` in `AndroidManifest.xml` — are not regressions; note them in the PR body.)

Run: `grep -r "TODO" ./composeApp/src` — expect no matches (CI fails the build on any match).

- [ ] **Step 4: Commit the tests**

```bash
git add composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/core/navigation/TopLevelDestinationTest.kt \
        composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/core/i18n/AppStringsTest.kt
git commit -m "test(ministry): update nav + i18n tests for ministry tab"
```

- [ ] **Step 5: iOS validation (PR)**

Push the branch and open the PR; rely on the `ios-check` CI job (~8 min) for iOS link/run validation — it cannot be validated locally with Command Line Tools only.

---

## Done When

- 사역 appears in the bottom nav (replacing 소식); tapping it shows the directory with Option-A cards.
- Tapping a ministry opens the sectioned detail (소개 / 지원 자격 / 일정 / 문의), with empty sections hidden.
- Home → 전체보기 still reaches the announcement list.
- `./gradlew :composeApp:testDevDebugUnitTest` green; `./gradlew lint` shows only the 3 known pre-existing errors; no `TODO` under `composeApp/src`; `ios-check` green on the PR.
```
