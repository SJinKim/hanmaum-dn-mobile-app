# Event RSVP — Teilnahme-Status (Mobile)

**Datum:** 2026-08-29
**Branch:** `SJinKim/rsvp-state` (von `SJinKim/screen-makover`)
**Design:** Figma `DN-App`, Section `17 · RSVP · 행사 참석`
**Status:** Entwurf — wartet auf Review

---

## Überblick

Mitglieder sollen für Gemeinde-Events vorab zusagen, absagen oder sich offen halten
(참석 / 불참 / 미정). Die Antwort kommt zuerst als Bottom Sheet; wer sie wegdrückt,
erreicht sie später über eine eigene RSVP-Seite unterhalb von 출석 체크.

Der Server hat Event-RSVP bereits — aber als reines Check-in ohne Status. Die dafür
nötigen Server-Änderungen sind eigene Tickets und laufen parallel:

| Ticket | Inhalt | Blockiert |
|---|---|---|
| `hanmaum-dn-server#121` | `status`, `reminder_count`, `last_reminded_at` auf `event_rsvp_logs` | #122, #123 |
| `hanmaum-dn-server#122` | `PUT /events/rsvps/{id}/response`, `myStatus` auf `ActiveEventRsvpDto` | Diese App-Arbeit |
| `hanmaum-dn-server#123` | Konfigurierbares Nachhaken bei 미정 | Reminder-Deeplink |
| `hanmaum-dn-ops#10` | `openapi.yaml` nachziehen | — |

**Diese App-Arbeit ist gegen #122 gebaut und kann vorher nicht end-to-end laufen.**
Sie ist trotzdem jetzt schon sinnvoll: die Screens sind unabhängig testbar, und der
Repository-Layer ist die einzige Stelle, die den Contract berührt.

---

## Abgrenzung

**Drin:** Bottom Sheet, RSVP-Listenscreen, Einstieg aus 출석 체크, Deeplink aus der
Erinnerung, Status setzen und ändern.

**Nicht drin:**

- **Anlegen von RSVPs.** Das bleibt Admin/GROUP_LEADER in der Web-App.
- **Teilnehmerliste.** `/attendees` ist serverseitig Admin-only und bleibt es.
- **Eigene Notification-Infrastruktur.** Push existiert bereits; wir hängen uns nur
  an `NotificationRouter` an.
- **Änderung an 출석 체크 selbst**, abgesehen von einem Einstiegs-Banner. Die
  wiederkehrende Gottesdienst-Anwesenheit (`attendance-controller`) und die
  einmaligen Events (`event-rsvp-controller`) bleiben getrennte Konzepte.

---

## Fachliche Festlegungen

1. **`windowStart` / `windowEnd` sind die Anmeldefrist**, nicht die Event-Zeit. So war
   das Feld ursprünglich gedacht: die Gemeinde braucht drei bis vier Tage vorher
   Planungssicherheit. Die App formuliert es entsprechend als „응답 마감", nie als
   Event-Zeitpunkt.
2. **„Antwort ausstehend" = `myStatus == null` ODER `myStatus == MAYBE`.** 미정 bleibt
   bewusst in der Liste, sonst hätte das Nachhaken keinen Ort, an dem der Nutzer
   reagieren kann.
3. **Das Sheet erscheint nur für `myStatus == null`.** Wer schon geantwortet hat — auch
   mit 미정 — wird nicht erneut unterbrochen; die Erinnerung läuft über Push.

---

## Domain

Neuer Slice `features/eventrsvp/`, parallel zu `features/attendance/`.

```kotlin
// domain/model/RsvpStatus.kt
enum class RsvpStatus { GOING, NOT_GOING, MAYBE }

// domain/model/EventRsvp.kt
data class EventRsvp(
    val publicId: String,
    val title: String,
    val windowStart: Instant,
    val windowEnd: Instant,
    val announcementId: String?,
    val myStatus: RsvpStatus?,   // null = noch nicht geantwortet
    val respondedAt: Instant?,
) {
    val isPending: Boolean get() = myStatus == null || myStatus == RsvpStatus.MAYBE
}

// domain/repository/EventRsvpRepository.kt
interface EventRsvpRepository {
    /** RSVPs, deren Anmeldefrist gerade offen ist. */
    suspend fun getActive(): Result<List<EventRsvp>>
    /** Setzt oder ändert die eigene Antwort. Idempotent. */
    suspend fun respond(publicId: String, status: RsvpStatus): Result<EventRsvp>
}
```

`Instant` statt der rohen Strings, die `AttendanceDefinition` benutzt: dort sind es
`HH:mm:ss`-Zeiten ohne Datum, hier echte `date-time`-Werte. Das Parsen gehört in den
Data-Layer, nicht in die Screens.

---

## Data

```
data/model/EventRsvpDtos.kt      ActiveEventRsvpResponse, EventRsvpResponseDto, SetRsvpResponseRequest
data/repository/EventRsvpRepositoryImpl.kt
```

Analog zu `AttendanceRepositoryImpl`: `runCatching`, `ApiResponse<T>`-Envelope,
relative Pfade (`createHttpClient` setzt `/api/v1/` davor).

- `GET events/rsvps/active` → `List<ActiveEventRsvpResponse>`
- `PUT events/rsvps/{publicId}/response` mit `{"status": "..."}` → `EventRsvpResponseDto`

Unbekannte Status-Strings werden zu `null` gemappt statt zu werfen — ein künftiger
vierter Status auf dem Server darf die App nicht abstürzen lassen.

---

## Presentation

### `RsvpViewModel`

Ein ViewModel für Sheet und Liste, weil beide dieselbe Liste anzeigen und dieselbe
Mutation auslösen.

```kotlin
data class RsvpUiState(
    val isLoading: Boolean = true,
    val events: List<EventRsvp> = emptyList(),
    val respondingTo: String? = null,   // publicId während des Requests
    val error: String? = null,
) {
    val pending: List<EventRsvp> get() = events.filter { it.isPending }.sortedBy { it.windowEnd }
    val answered: List<EventRsvp> get() = events.filter { !it.isPending }.sortedBy { it.windowEnd }
    val sheetCandidate: EventRsvp? get() = events.filter { it.myStatus == null }.minByOrNull { it.windowEnd }
}
```

`respond()` schreibt optimistisch in den State und rollt bei Fehler zurück — die
Auswahl muss sich sofort anfühlen, und der Request ist idempotent, also ist ein
Wiederholen unschädlich.

### `RsvpSheet`

Bottom Sheet über Home, sobald `sheetCandidate != null`. Baut auf der Sheet-Konvention
aus Section 13 auf: Scrim, Grabber Row, `radius 32/32/0/0`, Drop Shadow, Option-Rows
mit Radio rechts. Drei Optionen plus „나중에 답하기".

Wegdrücken setzt nur lokalen State — **keine Server-Mutation**, denn „später" ist keine
Antwort. Innerhalb derselben Session kommt das Sheet nicht wieder; beim nächsten
Kaltstart schon, solange die Frist läuft und `myStatus` weiter `null` ist.

### `RsvpScreen`

Voller Screen unter `RsvpRoute`. Aufbau exakt wie im Figma-Frame `RSVP 목록`:
Summary-Kacheln (응답 필요 / 참석 / 불참), Abschnitt 응답 필요 mit Karten und drei
Auswahl-Buttons, Abschnitt 응답 완료 mit änderbaren Zeilen.

Statusfarben, einmal festgelegt und überall gleich:

| Status | Fläche | Kontur / Text |
|---|---|---|
| 참석 | `accent/lime-dim` | `accent/lime`, Text `accent/lime-ink` |
| 불참 | `accent/red-dim` | `accent/red` |
| 미정 | `accent/amber-dim` | `accent/amber` |

Bei 미정 steht unter der Karte die nächste Erinnerung als Datum. Der Text kommt aus
`windowEnd` minus dem Offset — die App rechnet ihn **nicht** selbst aus, sondern zeigt
ihn nur, wenn der Server ihn liefert. Andernfalls entfällt die Zeile. Sonst würde eine
Änderung an `reminderOffsets` auf dem Server die App zur Lügnerin machen.

### Leerzustände

- Keine offenen RSVPs, aber beantwortete: Abschnitt 응답 필요 entfällt.
- Gar keine RSVPs: `DnErrorState`-Muster mit `ui/calendar`, „예정된 행사가 없습니다".
- Ladefehler: bestehender `DnErrorState` mit Retry, wie in Section 14.

---

## Navigation, DI, Einstiege

```kotlin
@Serializable object RsvpRoute            // core/navigation/Routes.kt
```

Drei Einstiege:

1. **출석 체크** — Banner „응답하지 않은 행사 N건", nur sichtbar wenn `pending` nicht leer.
2. **Bottom Sheet auf Home** — beim Start, für unbeantwortete RSVPs.
3. **Push-Erinnerung** — `NotificationDestination.Rsvp` ergänzen; `App.kt` navigiert
   analog zum bestehenden `Attendance`-Zweig auf `RsvpRoute`.

DI in `di/AppModule.kt`: `single<EventRsvpRepository>` und `viewModel { RsvpViewModel(get()) }`,
wie die übrigen Slices.

---

## Tests

`commonTest`, `FakeEventRsvpRepository` nach dem Muster von `FakeAttendanceRepository`.

- `pending` enthält `null` **und** `MAYBE`, sortiert nach `windowEnd`
- `sheetCandidate` ignoriert `MAYBE` und liefert die früheste Frist
- `respond()` aktualisiert optimistisch und rollt bei Fehler auf den alten Status zurück
- Zweimal derselbe Status → kein Fehlerzustand (Idempotenz)
- Unbekannter Status-String vom Server → `myStatus == null`, kein Crash
- Leere Liste → beide Abschnitte leer, kein Sheet

Keine Kommas oder Satzzeichen in Backtick-Testnamen — Kotlin/Native lehnt das ab.

---

## Offene Punkte

1. **Liefert der Server das Datum der nächsten Erinnerung?** Steht in #123 nicht drin.
   Ohne dieses Feld entfällt die Hinweiszeile unter 미정. Vor dem Merge von #122 klären;
   es wäre ein Einzeiler im DTO.
2. **Zeitzone.** `windowEnd` kommt als `OffsetDateTime`. Die App formatiert in
   `TimeZone.currentSystemDefault()`, wie `AttendanceViewModel`. Für Mitglieder in
   Deutschland und Korea gleichzeitig ist das die richtige Wahl, verschiebt aber die
   angezeigte Frist — bewusst so.
