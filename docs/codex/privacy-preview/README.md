# Reproduzierbare Datenschutz-Vorschau

Diese Quellen sichern die bisher nur lokal vorhandene DE/KO/EN-Vorschau.
Sie benötigen ausschließlich Python 3 und lesen die drei Markdown-Arbeitsfassungen
im übergeordneten `docs/codex/`-Verzeichnis. Die Darstellung und Texte bleiben
gegenüber dem Stand vom 28.10.2026 unverändert.

Vom Repository-Verzeichnis aus:

```bash
python3 docs/codex/privacy-preview/build_preview.py
python3 docs/codex/privacy-preview/serve_preview.py --port 18088
```

Danach im Browser `http://127.0.0.1:18088/`, `/ko/` oder `/en/` öffnen.
Falls der Port belegt ist, für den lokalen Server einen anderen Port angeben.
Die HTML-Dateien werden unter `build/privacy-preview/` erzeugt und nicht
eingecheckt. Mit `--output` lässt sich beim Generator ein anderes Ausgabeverzeichnis
wählen; dieses beim Server mit `--directory` übergeben.

Die Vorschau enthält bewusst die sichtbare Entwurfskennzeichnung, zwanzig
aufklappbare Prüfpunkte je Sprache und den Redaktionsteil. Keine fachliche
Freigabe wird durch das Erzeugen der Dateien erteilt. Der Server bindet nur
an Loopback, setzt die bisherige CSP und verändert keinen öffentlichen Server.

Veröffentlichungsentscheidung und nächste Schritte:
[Übergabe vom 29.10.2026](../privacy-handoff-2026-10-29.md).
