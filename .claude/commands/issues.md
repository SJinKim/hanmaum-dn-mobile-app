# /issues — Issue- und Board-Pflege ohne Token-Verschwendung

Board: **DN-Mobile-App**, Projekt-Nummer `6`, Owner `SJinKim`.

## Die Regel

`gh ... --format json` **ohne Feldauswahl ist verboten.** Gemessen an diesem Repo:
das rohe Board-JSON ist 44.965 Zeichen, die gefilterte Fassung 1.447 — Faktor 31,
rund 11k Tokens pro Abfrage. Immer erst filtern, dann lesen.

## Lesen

```bash
# Offene Issues — die Standardausgabe reicht und ist bereits schlank
gh issue list --limit 30

# Mit Labels, eine Zeile pro Issue
gh issue list --limit 30 --json number,title,labels \
  --jq '.[] | "#\(.number) \(.title) [\(.labels|map(.name)|join(","))]"'

# Board-Status aller Items (das rohe JSON NIE ungefiltert lesen)
gh project item-list 6 --owner SJinKim --format json --limit 100 \
  --jq '.items[] | "#\(.content.number) \(.status) \(.content.title)"'

# Ein einzelnes Issue inkl. Body — nur wenn der Body wirklich gebraucht wird
gh issue view <n> --json number,title,body,labels,state
```

## Board-Status setzen

Die IDs sind stabil und hier fest hinterlegt, damit sie nicht jedes Mal neu
abgefragt werden müssen (`gh project field-list` kostet ~2k Tokens):

| | |
|---|---|
| Projekt-ID | `PVT_kwHOA-OC0c4Bh0dZ` |
| Status-Feld | `PVTSSF_lAHOA-OC0c4Bh0dZzhguyOg` |

| Status | Option-ID |
|---|---|
| Todo | `adddf652` |
| In Progress | `f1f51406` |
| In Review | `8bf124c3` |
| Done | `62c51743` |

Item-ID für ein Issue holen, dann Status setzen:

```bash
ITEM=$(gh project item-list 6 --owner SJinKim --format json --limit 100 \
  --jq '.items[] | select(.content.number == <n>) | .id')

gh project item-edit --id "$ITEM" \
  --project-id PVT_kwHOA-OC0c4Bh0dZ \
  --field-id PVTSSF_lAHOA-OC0c4Bh0dZzhguyOg \
  --single-select-option-id <option-id>
```

## Wann welcher Status

Aus `feedback_project_board_status`: Der Status wird selbst gepflegt, nicht
liegen gelassen.

- **In Progress** — sobald der Feature-Branch für das Issue steht
- **In Review** — sobald der PR offen ist
- **Done** — der Merge schließt das Issue via `closes #n` automatisch; den
  Board-Status trotzdem prüfen, GitHub zieht ihn nicht immer nach

## Code zum Issue finden

Nicht `grep` über das ganze Repo. Der Graph beantwortet „wo lebt das" in
einem Bruchteil der Tokens:

```bash
graphify query "<worum es im issue geht>" --budget 1500
graphify affected "<Symbol>"      # was bricht, wenn ich das anfasse
```

Siehe CLAUDE.md §9.
