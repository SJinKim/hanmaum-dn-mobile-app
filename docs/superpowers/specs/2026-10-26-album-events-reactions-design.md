# Album — Event-Alben mit Reaktionen und Download

**Datum:** 2026-10-26
**Branch:** `SJinKim/album`
**Design:** Figma `DN-App` — Frames noch offen (Dark + Light, vor jedem Code abzunehmen)
**Status:** Entwurf — Builder-Check erledigt, wartet auf Freigabe
**Ersetzt:** `docs/superpowers/specs/2026-07-02-album-s3-migration-and-grid-redesign.md`
(Album 2.0 / Foto-Hub pro 순, Epic #303)

---

## Warum

Bei Gemeinde-Events entstehen Fotos. Gemacht und verwaltet werden sie vom Team
**DMU**, einem der Ministry-Teams. Bisher wurden diese Fotos **nie veröffentlicht** —
die App ist der erste Kanal dafür. Mitglieder sollen die Fotos eines Events als Album
ansehen, darauf mit einem Emoji reagieren und sie herunterladen können.

Die bisherige Album-Funktion hängt an öffentlichen pCloud-Freigaben und wird nicht
genutzt. Sie wird durch diese Funktion vollständig ersetzt. Speicher ist der
gemietete **Hetzner Object Storage** (S3-kompatibel).

## Ziele

- DMU lädt Fotos per Drag & Drop in der **hanmaum-dn-web-app** hoch, füllt ein Album
  in Ruhe und veröffentlicht es dann bewusst.
- Alle aktiven Mitglieder sehen alle veröffentlichten Alben in der App.
- Pro Foto: eine Emoji-Reaktion pro Person, Anzahl sichtbar.
- Download eines einzelnen Fotos und eines ganzen Albums in die Galerie.
- Push an alle aktiven Mitglieder, wenn ein Album veröffentlicht wird.

## Nicht-Ziele

- Kein Upload aus der App. Hochladen nur in der Web-App.
- Keine Kommentare, kein Teilen nach außen, keine Personen-Markierung.
- Keine Verknüpfung mit Kalender-Events. Ein Album hat eigenen Titel und eigenes Datum.
- Keine Sichtbarkeitsstufen pro Album oder Gruppe (kein `ALBUM_VIEWER`).
- Kein voller Emoji-Picker.
- Kein ZIP-Download.
- Kein RAW-Format.
- Keine Einwilligungs- oder Melde-Funktion in der App. Die Einwilligung der
  Abgebildeten regelt die Gemeinde selbst (Entscheidung Jin, 2026-10-26). Ein Foto
  entfernt DMU bei Bedarf in der Web-App.

## Anti-Ziele (wäre ein Fehlschlag, auch wenn es technisch läuft)

- Speicherkosten über dem Grundtarif von **7,72 €/Monat** — etwa weil die App
  Originale zum Anschauen lädt oder Vorschaubilder fehlen.
- Ein halb befülltes Album wird für Mitglieder sichtbar, bevor DMU es freigibt.
- Der Keycloak-Token geht an den S3-Host (siehe CLAUDE.md §3, Token-Leak).
- Die App listet den Bucket und zeigt dadurch Dateien, die nicht zu Alben gehören.

## Rahmenbedingungen

- **Ein gemeinsamer Bucket.** Dort liegen auch andere Dateien (z. B. Profilfotos,
  server#140). Alben bekommen ein festes Präfix; die **Datenbank ist die Quelle der
  Wahrheit**, nicht der Bucket-Inhalt.
- Bucket ist **privat**. Zugriff nur über vorsignierte URLs vom Server.
- S3-Zugangsdaten liegen nur in `/opt/hanmaum-dn-server/.env.staging` (bzw. prod)
  auf dem Server — nie im Repo, nie in GitHub-Secrets der Clients. Für Staging sind
  sie bereits eingetragen.
- **Figma zuerst:** jeder Screen (App und Web) als Frame in Dark + Light, abgenommen,
  dann Code.
- **Contract-Fluss:** server → `hanmaum-dn-ops/api/openapi.yaml` → Web/App. Die
  Endpoints entstehen im Server; Web und App richten sich danach.

---

## Entscheidungen

Markierung: ohne Zusatz = von Jin entschieden. **(meine Wahl)** = von mir
vorgeschlagen, nicht widersprochen — bitte beim Lesen prüfen.

### Alben und Veröffentlichung

| Punkt | Entscheidung |
|---|---|
| Album-Daten | Eigener **Titel** und eigenes **Datum**, von den Uploadern gesetzt |
| Lebenszyklus | `DRAFT` → `PUBLISHED`. Entwürfe sieht nur, wer Alben bearbeiten darf |
| Sichtbarkeit | Alle Mitglieder mit Status `ACTIVE` sehen alle veröffentlichten Alben |
| Zurückziehen | Editor kann ein Album zurück auf `DRAFT` setzen; Reaktionen bleiben **(meine Wahl)** |
| Nachladen | Editor kann Fotos zu einem veröffentlichten Album hinzufügen **(meine Wahl)** |
| Foto löschen | Entfernt Original + Vorschauen in S3 und die Reaktionen **(meine Wahl)** |
| Album löschen | Löscht alle Fotos samt S3-Dateien, nach Bestätigungsdialog **(meine Wahl)** |
| Album-Name | Wird beim Anlegen in der Web-App festgelegt (Entscheidung Jin, 2026-10-26); später änderbar, der Speicherordner bleibt |
| Titelbild | Wird beim Anlegen in der Web-App festgelegt (Entscheidung Jin, 2026-10-26). Es ist immer eines der Fotos des Albums **(meine Wahl)** und kann später gewechselt werden. Wählt der Editor keins, gilt das erste Foto |
| Reihenfolge der Alben | Nach Album-Datum absteigend **(meine Wahl)** |
| Reihenfolge der Fotos | Upload-Reihenfolge (`sort_order`) **(meine Wahl)** |

### Berechtigung

- Neue Keycloak-Realm-Rolle **`ALBUM_EDITOR`**, nach dem Muster von `GUIDE_EDITOR`:
  Gruppe `/ministries/album/editors` → Rolle `ALBUM_EDITOR`. Ein Admin weist sie zu.
  So ist „wer hochlädt" konfigurierbar statt fest auf DMU verdrahtet.
- Kein `ALBUM_VIEWER`.
- `ADMIN` und `PASTOR` dürfen über die bestehende Rollen-Hierarchie alles.
- Server: `@PreAuthorize("hasRole('ADMIN') or hasRole('ALBUM_EDITOR')")` auf allen
  schreibenden Endpoints.
- Zu ändern im Server: Realm-Exporte
  `infrastructure/docker/keycloak/export/hanmaum-realm.json` und `realm-export.json`,
  Tabelle in `KEYCLOAK_RUNBOOK.md`. Auf Staging/Prod muss ein Admin die Gruppe in
  Keycloak anlegen — das ist ein Keycloak-Config-Schritt, den Jin freigibt.

### Reaktionen

| Punkt | Entscheidung |
|---|---|
| Emojis | Feste Leiste ❤️ 👍 😂 😮 😢 🙏 — kein voller Picker |
| Öffnen | **Lange drücken** auf das Foto im Vollbild |
| Doppeltipp | Setzt ❤️ **(meine Wahl, aus der Recherche)** |
| Pro Person | **Genau eine** Reaktion pro Foto. Anderes Emoji wählen = wechseln, eigenes antippen = entfernen |
| Anzeige im Vollbild | Chips unter dem Foto: Emoji + **Anzahl**. Eigener Chip hervorgehoben |
| Überlauf | **Alle** genutzten Chips zeigen (max. 6, eine Zeile) — kein „+N" nötig (Option A) |
| Anzeige im Raster | Kachel-Badge: Top-3-Emojis + Gesamtzahl **(meine Wahl)** |

Warum kein „+N": bei 6 festen Emojis gibt es höchstens 6 Chips, die in eine Zeile
passen. Der ursprüngliche Wunsch „ab 5 oder 10 mit + erweitern" erledigt sich damit.

Gespeichert wird ein **Code**, kein Emoji-Zeichen: `HEART`, `THUMBS_UP`, `LAUGH`,
`WOW`, `SAD`, `PRAY`. Die App übersetzt den Code in das Zeichen.

### Download

| Punkt | Entscheidung |
|---|---|
| Einzeln | Im Vollbild „Speichern" → Original in die Galerie |
| Ganzes Album | „Alle speichern" → nacheinander in die Galerie, mit Fortschritt. Kein ZIP **(meine Wahl)** |
| Bestätigung | Vor „Alle speichern": „120 Fotos · ca. 480 MB speichern?" **(meine Wahl)** |
| Quelle | Download = Original. Anschauen = verkleinerte Versionen **(meine Wahl)** |

### Formate

Hochladbar: **JPEG, HEIC, PNG**. Kein RAW (Rohdaten der Kamera, 25–60 MB pro Bild).
Vorschauen werden immer als JPEG (oder WebP) erzeugt, auch aus HEIC.

### Push

Beim Veröffentlichen geht ein Push an alle aktiven Mitglieder. Kein Push, wenn nur
Fotos nachgeladen werden **(meine Wahl)**. Tippen öffnet das Album. Der Push läuft
über die bestehende Notification-Infrastruktur (`NotificationFanoutListener`).

- **Text nur auf Koreanisch**, wie jeder andere Push heute **(meine Wahl)**. Der
  Server kennt die Sprache der Mitglieder nicht; Lokalisierung von Pushes ist ein
  eigenes, appweites Thema und nicht Teil dieses Features.
- **Nur beim ersten Veröffentlichen** **(meine Wahl)**. Zurückziehen und erneut
  veröffentlichen schickt keinen zweiten Push. Dafür merkt sich das Album
  `first_published_at`.
- Öffnet jemand eine alte Benachrichtigung zu einem Album, das wieder Entwurf ist
  oder gelöscht wurde, zeigt die App „Dieses Album ist nicht mehr verfügbar“
  **(meine Wahl)**. Die Benachrichtigungs-Zeilen bleiben stehen.

---

## Speicher und Kosten

### Präfixe im Bucket

```
ldn-event-album/{env}/{albumFolder}/original/{photoPublicId}.{ext}
ldn-event-album/{env}/{albumFolder}/display/{photoPublicId}.jpg   ~1600 px lange Kante
ldn-event-album/{env}/{albumFolder}/thumb/{photoPublicId}.jpg     ~400 px lange Kante
members/…                                  Profilfotos (server#140)
files/…                                    sonstige Dateien
```

- **Wurzel `ldn-event-album/`, darunter `staging/` und `prod/`** (Entscheidung Jin,
  2026-10-26). Staging und Prod teilen sich den Bucket, ohne dass sich Pfade
  überschneiden oder ein Löschen auf Staging Prod-Dateien trifft.
- **Alben sind nach Event-Namen gruppiert** (Entscheidung Jin). Der Ordnername
  `{albumFolder}` ist `{album_date}_{Titel}`, z. B. `2026-10-12_추수감사절`:
  - Das Datum vorne sortiert die Ordner in jedem S3-Browser chronologisch
    **(meine Wahl)**.
  - Der Titel wird bereinigt: `/`, `\` und Steuerzeichen raus, Leerzeichen → `-`.
    Koreanische Zeichen bleiben erhalten (S3-Schlüssel sind UTF-8) **(meine Wahl)**.
  - Gibt es den Ordner schon (gleiches Datum, gleicher Titel), bekommt der neue ein
    Suffix `-2`, `-3` … **(meine Wahl)**.
  - Der Ordnername wird **beim Anlegen festgelegt und in `albums.storage_folder`
    gespeichert**. Benennt DMU das Album später um, bleibt der Ordner, wie er ist;
    sonst müssten alle Dateien kopiert werden **(meine Wahl)**.
- Die Fotodateien selbst heißen nach der UUID (`public_id`), nicht nach dem
  Kamera-Dateinamen. So gibt es keine Kollisionen. Der ursprüngliche Dateiname
  steht in `album_photos.original_filename` und wird beim Speichern in der App
  wieder verwendet **(meine Wahl)**.

Die App kennt keine Pfade. Sie bekommt pro Foto vom Server vorsignierte URLs für
`thumb`, `display` und (nur beim Speichern) `original`.

### Warum das in den Grundtarif passt

- Raster lädt nur `thumb` (~40–80 KB), Vollbild nur `display` (~300–500 KB).
  Das Original (3–8 MB) wird nur beim aktiven Speichern geladen.
- Der Grundtarif enthält Speicher und Traffic in einer Größenordnung, die eine
  Gemeinde-App mit diesem Muster nicht erreicht. Der Builder rechnet den aktuellen
  Hetzner-Tarif kurz nach und schreibt das Ergebnis in den Server-PR.

### Vorsignierte URLs und Cache

- GET-URLs laufen ab (Server-Config `get-url-ttl`). Weil sich die URL ändert, muss der
  Bild-Cache der App (Coil) über einen **stabilen Schlüssel** gehen — die
  `photoId` + Variante — nicht über die URL. Sonst lädt jedes Öffnen alles neu.
- Uploads aus der Web-App gehen per vorsignierter PUT-URL direkt an S3; der Server
  sieht die Bytes erst beim Erzeugen der Vorschauen.
- Kein Bearer-Token an den S3-Host — weder in der App (`sendWithoutRequest` ist
  host-gebunden, CLAUDE.md §3) noch in der Web-App.

---

## Datenmodell (Skizze)

```
albums
  id, public_id, title, album_date, status (DRAFT|PUBLISHED),
  cover_photo_id (nullable), published_at (nullable),
  first_published_at (nullable, steuert den einmaligen Push),
  storage_folder (beim Anlegen festgelegt, unveränderlich),
  created_by, created_at, updated_at

album_photos
  id, public_id, album_id → albums,
  original_key, display_key, thumb_key, original_filename,
  content_type, size_bytes, width, height,
  sort_order, status (UPLOADING|READY|FAILED), created_at

photo_reactions
  photo_id → album_photos, member_id → members,
  emoji (HEART|THUMBS_UP|LAUGH|WOW|SAD|PRAY), created_at
  PRIMARY KEY (photo_id, member_id)
```

Der Primärschlüssel `(photo_id, member_id)` erzwingt „eine Reaktion pro Person".
Wechseln ist ein Upsert, Entfernen ein Delete.

Auf `main` existiert bereits eine Tabelle `albums` aus der pCloud-Zeit
(`V20260515000000__create_albums_table.sql`: `name`, `pcloud_code`, `display_order`).
Sie wird von einer neuen Flyway-Migration ersetzt. Die pCloud-Zeilen sind
Wegwerf-Daten — Fotos wurden nie veröffentlicht **(meine Wahl: Tabelle neu
aufbauen statt migrieren)**.

## Ablauf

### Upload (Web-App)

1. Editor legt ein Album an: **Name** und Datum → `DRAFT`.
2. Im selben Anlege-Dialog Drag & Drop mehrerer Dateien. Pro Datei fragt die Web-App den Server nach einer
   vorsignierten PUT-URL; der Server legt eine `album_photos`-Zeile `UPLOADING` an.
3. Browser lädt direkt nach S3 hoch und meldet dem Server „fertig".
4. Server erzeugt `display` + `thumb`, liest Breite/Höhe, setzt `READY`
   (bei Fehler `FAILED` mit Hinweis in der Web-App).
5. Noch im Anlege-Dialog wählt der Editor per Klick auf ein hochgeladenes Foto das
   **Titelbild** (Stern-Markierung). Ohne Auswahl ist das erste Foto vorbelegt.
6. Editor prüft, sortiert ggf., klickt **Veröffentlichen** →
   `PUBLISHED`, `published_at` gesetzt, Push geht raus.

### Anschauen (App)

1. **Alben-Liste:** veröffentlichte Alben, neueste zuerst — Titelbild, Titel, Datum,
   Anzahl Fotos.
2. **Album:** Raster aus `thumb`, Badge mit Top-3-Reaktionen + Summe. Aktion
   „Alle speichern".
3. **Vollbild:** `display`, wischen zum nächsten Foto, Reaktions-Chips darunter,
   lange drücken = Leiste, Doppeltipp = ❤️, „Speichern".

---

## Umfang pro Repo

### hanmaum-dn-server

- S3-Anbindung auf `main` bringen. Wiederverwendbar vom alten Branch
  `feat/HDN-0-album2-s3-photo-hub`: `common/storage/S3ClientConfig.kt`,
  `S3StorageProperties.kt`, `S3StorageService.kt`, `S3StorageServiceIT.kt` und der
  `app.storage.s3`-Block in `application.yml`. **Nicht** übernehmen: dessen
  Album-Modell und Migration `V20260703120000__album2_photo_hub.sql`.
- Die Storage-Schicht ist gemeinsam mit den Profilfotos (server#140) gedacht.
- Album-Modell, Migration, Endpoints (Lesen für Mitglieder, Schreiben für Editoren),
  Vorschau-Erzeugung, Reaktionen, Push beim Veröffentlichen.
- Bestehenden pCloud-Code entfernen: `features/album/` (Controller, DTO, Service,
  Repository, Domain).
- Rolle `ALBUM_EDITOR` in Realm-Exporten und `KEYCLOAK_RUNBOOK.md`.
- `./gradlew syncOpenApiToOps` und Commit im Ops-Repo.

### hanmaum-dn-web-app

- Alben-Verwaltung für `ALBUM_EDITOR`/`ADMIN`: Liste (Entwurf/veröffentlicht),
  Album anlegen/bearbeiten, Drag-&-Drop-Upload mit Fortschritt pro Datei, Foto
  löschen, Titelbild wählen, Veröffentlichen/Zurückziehen, Album löschen.

### hanmaum-dn-mobile-app

- Ersetzt `features/album/` komplett (heute pCloud: `PCloudDownloadResponse`,
  `PCloudFolderResponse`, `AlbumCacheRepositoryImpl`, …) und die Route
  `AlbumDetailRoute(pcloudCode, albumName)`.
- Alben-Liste, Album-Raster, Vollbild mit Reaktionen, Einzel- und Album-Download.
- Galerie-Speichern ist plattformspezifisch (Android MediaStore, iOS Photos) →
  `expect`/`actual` bzw. Interface im `PlatformModule`. Inklusive der nötigen
  Berechtigungstexte (iOS `NSPhotoLibraryAddUsageDescription`).
- Push-Deeplink auf das Album.

## Dem Builder überlassen

- **HEIC-Dekodierung auf dem Server.** Auf der JVM schwierig. Optionen: libvips oder
  ImageMagick im Docker-Image, oder Umwandlung im Browser vor dem Upload. Vorgabe nur:
  Vorschauen sind JPEG/WebP, das Original bleibt unverändert.
- Synchron oder asynchron erzeugte Vorschauen, Größen der Vorschauen im Rahmen oben.
- Maximale Dateigröße pro Upload (Vorschlag: 30 MB).
- Exakte Endpoint-Pfade und DTOs — sie entstehen im Server und gehen über `openapi.yaml`.
- Aufräumen verwaister `UPLOADING`-Zeilen und S3-Dateien (der alte Branch hatte dafür
  einen `cleanup-cron`).
- Wie die App die Gesamtgröße für die Bestätigung „ca. 480 MB" bekommt
  (`size_bytes` summiert im Album-DTO liegt nahe).

---

## Aufräumen (nach Abnahme des Designs, vor Löschung einzeln bestätigen)

| Was | Warum |
|---|---|
| mobile Epic #303 „Album 2.0" | Konzept ersetzt |
| server #116 pCloud-Jahresfilter | pCloud fällt weg |
| server-Branch `feat/HDN-0-album2-s3-photo-hub` | S3-Klassen vorher retten, dann löschen |
| Spec `2026-07-02-album-s3-migration-and-grid-redesign.md` (PR #304) | durch dieses Dokument ersetzt |
| Remote-Branch `origin/docs/HDN-303-album-spec` | gehört zum alten Spec |

Bleiben: server #140 (Object Storage + Profilfoto-Upload) und mobile #139
(Profilfoto-Upload) — sie teilen sich die Storage-Schicht mit diesem Feature.

## Reihenfolge

1. Dieses Dokument abnehmen.
2. Figma: Web-Verwaltung + App (Liste, Raster, Vollbild, Reaktionen, Download) —
   Dark + Light, abnehmen.
3. Plan (superpowers:writing-plans), Tickets im Board.
4. Server → Ops-Sync → Web-App und App parallel.
5. Aufräumen.
