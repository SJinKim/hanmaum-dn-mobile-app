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
liegen gelassen. Jeder dieser drei Schritte ist Pflicht, nicht Kür — ein
Board, das hinterherhinkt, ist schlimmer als keins.

- **In Progress** — sobald der Feature-Branch für das Issue steht
- **In Review** — sobald der PR offen ist, **und der PR ist mit dem Issue
  verlinkt** (siehe unten)
- **Done** — nach dem Merge. Prüfen statt annehmen: GitHub zieht den
  Board-Status nicht zuverlässig nach, und ein Issue ohne gültiges
  Closing-Keyword schließt sich gar nicht

## PR mit Issue verlinken — das Keyword muss englisch sein

Der PR-Body **muss** `Closes #<n>` enthalten (oder `Fixes` / `Resolves`).
Das ist kein Stil, sondern der Mechanismus: GitHub erzeugt daraus den
Development-Link, und **nur** daraus speist sich die Board-Spalte
*Linked pull requests*. Ohne den Link schließt sich das Issue beim Merge
nicht, und die Karte zeigt keinen PR.

**Deutsche Keywords funktionieren nicht.** Sieben PRs schrieben „Behebt #n";
GitHub parst das nicht, also blieben sieben Issues offen und sieben Karten
ohne PR — die gesamte Board-Drift vom 2026-09-01 hatte diese eine Ursache.
Issues und PRs werden auf Englisch geschrieben.

Nachträglich reparierbar: den Body eines auch schon gemergten PR editieren
und das Keyword eintragen — GitHub baut den Link rückwirkend auf. Das Issue
schließt sich dann nicht mehr von selbst, das bleibt Handarbeit.

Nach dem Öffnen des PR verifizieren, dass der Link wirklich steht:

```bash
gh api graphql -f query='
{ repository(owner:"SJinKim", name:"hanmaum-dn-mobile-app") {
    issue(number:<n>) {
      state
      closedByPullRequestsReferences(first:5, includeClosedPrs:true) { nodes { number } }
    } } }'
```

Leere `nodes` heißt: das Keyword fehlt oder ist nicht englisch.

Board-Spalte für alle Karten auf einmal prüfen — Status und verlinkter PR
nebeneinander:

```bash
gh api graphql -f query='
{ user(login:"SJinKim") { projectV2(number:6) {
    items(first:100) { nodes {
      content { ... on Issue { number state } }
      fieldValues(first:20) { nodes { __typename
        ... on ProjectV2ItemFieldSingleSelectValue { name }
        ... on ProjectV2ItemFieldPullRequestValue { pullRequests(first:5){nodes{number}} }
      } } } } } } }' --jq '.data.user.projectV2.items.nodes[] | select(.content.number != null) |
  "\(.content.number)\t\(.content.state)\t\([.fieldValues.nodes[]|select(.__typename=="ProjectV2ItemFieldSingleSelectValue")|.name]|join(""))\t\([.fieldValues.nodes[]|select(.__typename=="ProjectV2ItemFieldPullRequestValue")|.pullRequests.nodes[].number]|join(","))"'
```

Eine Karte in **Done** mit leerem PR-Feld ist ein Fund, kein Rauschen.

## Done ohne PR — das Feld *Remarks* füllen

Manche Karten sind zu Recht ohne PR fertig: beiläufig in einem fremden PR
mitrepariert, per Commit direkt erledigt, oder als hinfällig geschlossen.
Ein leeres PR-Feld sieht dann aus wie derselbe Verlinkungsfehler von oben.

Deshalb: **jede Done-Karte ohne verlinkten PR bekommt einen Eintrag im
Board-Feld `Remarks`** — die Feststellung, dass keiner existiert, *und der
Grund*. Ohne Grund ist der Eintrag wertlos; er soll die nächste Person davon
abhalten, den Fall noch einmal zu untersuchen.

```bash
ITEM=$(gh project item-list 6 --owner SJinKim --format json --limit 100 \
  --jq '.items[] | select(.content.number == <n>) | .id')

gh project item-edit --id "$ITEM" \
  --project-id PVT_kwHOA-OC0c4Bh0dZ \
  --field-id PVTF_lAHOA-OC0c4Bh0dZzhhDgFc \
  --text "No PR: <Grund>."
```

Beispiel — #104: *„No PR: the relaxed TODO gate that resolves this landed
incidentally inside #106 (v2 redesign import), which was not opened for this
issue — linking it would misattribute that PR."* Einen fremden PR
nachträglich mit `Closes` zu versehen, nur damit die Spalte gefüllt ist, wäre
die falsche Reparatur: der Link behauptet dann eine Absicht, die es nie gab.

## Code zum Issue finden

Nicht `grep` über das ganze Repo. Der Graph beantwortet „wo lebt das" in
einem Bruchteil der Tokens:

```bash
graphify query "<worum es im issue geht>" --budget 1500
graphify affected "<Symbol>"      # was bricht, wenn ich das anfasse
```

Siehe CLAUDE.md §9.
