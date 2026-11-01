# Datenschutz: Übergabe vom 29.10.2026

## Letzter Auftrag und verbindliche Entscheidung

Jin hat die Veröffentlichung beauftragt und anschließend ausdrücklich gewählt:
**„Unverändert als öffentlichen Entwurf bereitstellen“**. Danach folgte der
Auftrag, den jetzigen Stand für die Weiterarbeit an anderer Stelle zu pushen.

Die bestätigten Angaben und der sichtbare Entwurfsstatus sollen erhalten
bleiben. Dies ist eine Freigabe für eine **öffentliche Entwurfsfassung**, keine
Erklärung, dass die offenen Pflichtangaben bereits geklärt sind. Keine erneute
allgemeine Veröffentlichungserlaubnis erfragen. Die tatsächliche Veröffentlichung
wurde noch nicht ausgeführt; die öffentliche URL liefert weiterhin 503.

## Gesicherte Quellen

- Branch `feature/privacy-consent-290`, [Mobile PR #300](https://github.com/SJinKim/hanmaum-dn-mobile-app/pull/300), weiterhin offen und Draft.
- Vollständige Texte: `privacy-policy.de.draft.md`, `privacy-policy.ko.draft.md`,
  `privacy-policy.en.draft.md` in diesem Verzeichnis. Letzter Textstand: 28.10.2026.
- Bestätigte Gemeindedaten, Rollen und Aufbewahrungskriterium sowie Herkunft:
  `privacy-policy-church-source-2026-10-27.md`.
- App-Implementierung und bisherige Prüfungen: `privacy-consent-290.md`.
- Die zuvor nur lokal gespeicherte professionelle HTML-Vorschau ist jetzt mit
  Generator und unverändertem Stylesheet unter `privacy-preview/` reproduzierbar.

Der Generator erzeugt dieselben Bytes wie die bisherige lokale Vorschau:

| Sprache | SHA-256 von `index.html` |
| --- | --- |
| DE | `9d8f0ae8c85025ade4d54a14d890a8b69f2e280af5dacd99701ef8b434ae0acc` |
| KO | `f68bd50469eb7314a1f1a6e07ab5cbca8538e1502cc9e535bde7ab5420324bf3` |
| EN | `19ac62120f5909afa285361e1e558de25e46bfb3dc8c6886e4ddf6d62b6c5e90` |

Die neue portable Ablage wurde durch erneutes Erzeugen, Bytevergleich aller
drei Seiten und drei lokale HTTP-/CSP-Prüfungen gegen `serve_preview.py`
verifiziert. Python-Syntax und `git diff --check` sind sauber. App-Code und
Datenschutztexte wurden dafür nicht verändert; Gradle-Prüfungen wurden für
diesen Dokumentations-/Vorschau-Checkpoint nicht erneut ausgeführt.

## Verifizierter Remote-/Live-Stand

Am 29.10.2026 unmittelbar vor dieser Übergabe geprüft:

- [Edge PR #5](https://github.com/SJinKim/graceops-edge/pull/5) ist gemergt;
  `graceops-edge` `origin/main` steht auf `3c14de6`. Die Versionsveröffentlichung
  entspricht dem Implementierungscommit `c0cc2aa`.
- [Ops PR #79](https://github.com/SJinKim/hanmaum-dn-ops/pull/79) und
  [App PR #311](https://github.com/SJinKim/hanmaum-dn-mobile-app/pull/311) sind gemergt.
  #311 betrifft die App-Marketingversion, nicht die Datenschutz-Dokumentkennung.
- [Server PR #373](https://github.com/SJinKim/hanmaum-dn-server/pull/373) ist offen
  und ersetzt den geschlossenen #359. Der alte Verweis in #300 ist überholt.
- `https://privacy.graceops.de/`: HTTPS geprüft, HTTP 503.
- ST `https://api.staging.graceops.de/api/v1/consents/policies`: HTTP 401.
  Eine öffentlich angebotene Server-Policy-Version wurde somit nicht bestätigt.

## Read-only-Preflight am Server

Die bestehenden Production-/ST-API-, ST-OIDC- und Grafana-Prüfungen haben jeweils
PASS geliefert. Das laufende Caddy-Image ist unverändert 2.11.4 mit gepinntem
Digest. Die Mounts für Caddy-Konfiguration, Site-Dateien und `/srv/privacy`
sind schreibgeschützt. Auf dem Server fehlt `scripts/privacy_release.py` noch:
der gemergte Versions-Publisher ist noch nicht installiert.

Es gab keine Änderung an Serverdateien, Containern, Policy-URL-Variablen oder
Einwilligungen. Die vier bisherigen Dienste sind unverändert erreichbar.
Der neue Edge-Quellstand wurde lokal erneut mit `scripts/validate.sh` geprüft:
vollständige Compose-/Caddy-Konfiguration gültig, alle 27 Tests bestanden.

## Nächste Schritte für die Veröffentlichung

1. Zuerst den ausdrücklich gewählten **öffentlichen Entwurfsmodus** technisch
   vorbereiten. Der bestehende Approved-Release-Publisher lehnt Entwurfstexte
   absichtlich ab. Seine Freigabeprüfungen erhalten; keine `approved: true`-
   Metadaten oder abgeschlossene Policy-Version für diesen Text vortäuschen.
2. Die drei unveränderten Sprachfassungen mit sichtbarem Entwurfsstatus und
   eindeutig als nicht freigegeben gekennzeichneten Metadaten bereitstellen.
   Dafür einen dokumentierten Entwurfs-Publikationspfad/-modus verwenden.
3. Den konkreten Aktivierungsplan gegen die tatsächlich laufende Edge-Installation
   prüfen. Gemergtes Routing ist noch kein Live-Routing. Unter dem gemeinsamen
   Lock arbeiten; Rollback auf den tatsächlich vorher aktiven Stand vorbereiten.
   Keinen Legacy-Cutover erneut ausführen und keine App-Container neu starten.
4. Nach Aktivierung alle drei Sprachseiten, Entwurfskennzeichnung, HTTP-/TLS-
   Verhalten sowie die bestehenden vier Dienste prüfen und Ergebnis dokumentieren.
5. Die Entwurfsveröffentlichung getrennt von APP_PRIVACY-/ST-Freigabe halten.
   Keine `ST_PRIVACY_POLICY_URL` als freigegeben aktivieren und keinen erfolgreichen
   Ops-Release-Check behaupten, solange freigegebener Text, bestätigte Server-Version
   und die vollständige Integration fehlen.

Rechtsgrundlagen, konkrete Anbieter-/Drittlandangaben und einzelne technische
Löschfristen bleiben im Text offen. Jin möchte den bestehenden Text unverändert
öffentlich sehen; diese Angaben bei der Entwurfsveröffentlichung weder erfinden
noch heimlich entfernen. Eine rechtliche Abschlussprüfung wurde nicht behauptet.

## Weiterarbeiten

PR #300 bzw. `feature/privacy-consent-290` auschecken und zunächst
`python3 docs/codex/privacy-preview/build_preview.py` ausführen.
Die benötigten Quellen liegen damit vollständig im Git-Stand; lokale
`reports/`-Dateien, `/tmp`-Skripte und maschinenspezifische Pfade sind nicht nötig.
Zugangsdaten und private Schlüssel bleiben außerhalb des Repositories.
