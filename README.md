# Kana Führerschein

Ein Trainer für Hiragana und Katakana, gebaut nach dem Vorbild der deutschen
Fahrschul-Theorie-App: bewusst stupides, repetitives Auswendiglernen mit einer
Wiederholungslogik, die dir falsch beantwortete Zeichen häufiger zeigt und dir
sagt, wie weit du wirklich bist.

Läuft auf Android und auf Linux-Desktop aus derselben Codebasis
(Kotlin Multiplatform, Compose Multiplatform).

<!--
Screenshots: Bilder unter docs/screenshots/ ablegen und die Pfade unten
einsetzen. Sinnvolle Auswahl: Startseite, Drill auf dem Handy,
Drill im breiten Fenster (Zwei-Spalten), Lernkarte, Einstellungen.
-->

| Startseite | Üben (Handy) | Lernen |
|---|---|---|
| _Screenshot folgt_ | _Screenshot folgt_ | _Screenshot folgt_ |

| Üben (Desktop, 16:9) | Einstellungen |
|---|---|
| _Screenshot folgt_ | _Screenshot folgt_ |

---

## Für wen das gedacht ist

Für Anfänger, die **Kana lesen** lernen wollen, und zwar schnell und
vollständig — nicht für Leute, die Japanisch „mal ausprobieren".

Konkret passt die App, wenn du:

- die 208 Kana in Wochen statt Monaten sicher lesen willst,
- deutschsprachig bist: die Aussprachehilfen vergleichen mit deutschen Lauten
  und warnen genau da, wo deutsche Sprecher zuverlässig danebenliegen,
- eine ehrliche Zahl willst, wie weit du bist, statt einer Streak-Anzeige,
- offline lernen willst — es gibt keinen Account, keinen Server, keine
  Netzwerkverbindung. Der Lernstand ist eine Datei auf deinem Gerät.

Sie passt **nicht**, wenn du Vokabeln, Kanji oder Grammatik lernen willst.
Die App macht genau eine Sache.

---

## Wie es funktioniert

Zwei Wege vom Hauptmenü aus.

### Üben

Ein Zeichen erscheint groß in der Mitte. Du tippst die Romaji-Lesung und
drückst Enter. Auf dem Handy über eine eigene Tastatur mit den 22 Buchstaben,
die Romaji überhaupt braucht (kein `l`, `q`, `v`, `x`), am Rechner über die
richtige Tastatur.

- **Enter** — Antwort abgeben, danach weiterblättern
- **Leertaste** — „keine Ahnung", zählt als Fehler, zeigt die Lösung
- **Esc** — Eingabe löschen

Richtig antworten schaltet nach kurzer Bestätigung von selbst weiter. Bei einem
Fehler bleibt die Lösung stehen, bis du Enter drückst — du sollst sie lesen.

Eine Runde hat standardmäßig 30 Fragen. Sie endet nicht mit offenen Fehlern:
bis zu acht Fragen Nachspielzeit hängen dran, um verpasste Zeichen noch
festzusetzen.

### Lernen

Zum Anschauen, ohne Abfrage. Eine Karte je Lesung zeigt:

- **beide Schriften nebeneinander** — し und シ sind dieselbe Lesung, und die
  Verbindung einmal gesehen zu haben kostet nichts,
- die Romaji-Lesung,
- die Aussprache, getrennt in **Konsonant**, **Vokal** und **Achtung**,
- einen Knopf für die Aufnahme; sie spielt beim Anzeigen automatisch, was du
  im Hauptmenü abschalten kannst.

Mit dem Haken „Alle Zeichen zeigen" blätterst du durch alles, ohne Haken nur
durch das, was du noch nicht gelernt hast. Am Rechner: Pfeiltasten blättern,
Leertaste hört nochmal, Esc zurück.

---

## Die Lernmethodik

### Niemals Multiple Choice

Das ist die zentrale Entscheidung. Bei Kana sind die falschen Antworten
zwangsläufig ähnlich, also lässt sich die Lösung per Ausschlussverfahren
finden, ohne das Zeichen zu kennen. Man kommt sich dabei kompetent vor und
lernt nichts. Deshalb tippst du die Antwort immer selbst: freie Produktion
statt Wiedererkennen.

### Kunrei und Wāpuro zählen, mit Hinweis

し ist `shi` in Hepburn und `si` in Kunrei. Wer `si` tippt, **kennt** し — das
als Fehler zu werten würde die App zu einem Umschrift-Test machen und das
Lernsignal ruinieren. Also: volle Punkte, aber ein Hinweis dazu:

> ✓ Richtig. Hepburn schreibt „shi" — „si" ist die Kunrei-Form.

Schweigen wäre die andere Falle: wer sechs Wochen `sya` tippt, ohne dass es
jemand sagt, lernt nie, dass `sha` existiert. Der Hinweis ist gedrosselt,
damit er nicht zur Tapete wird. Wenn du willst, kannst du in den Einstellungen
auf „nur Hepburn" umstellen — dann wird `si` ein Beinahe-Treffer, der neu
gefragt wird, aber nie ein Fehler.

### Neun Boxen mit festen Abständen

Kein SM-2, kein FSRS. Das Deck ist bei 230 Items geschlossen, das Ziel ist
Vollbeherrschung in wenigen Wochen — und ein Intervall, das man dir hinschreiben
kann, ist Voraussetzung für eine glaubwürdige Fortschrittszahl.

| Box | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 |
|---|---|---|---|---|---|---|---|---|---|
| Wiederholung in | Session | 10 min | 1 h | 8 h | 1 Tag | 3 Tage | 1 Woche | 3 Wochen | 2 Monate |

Jedes Intervall streut ±10 %, damit keine Wiederholungslawinen entstehen.
Eine Antwort über 6 Sekunden hebt ab Box 4 nicht mehr auf: das ist
Rekonstruktion, kein Wiedererkennen.

**Ein Fehler kostet drei Boxen, nicht alles.** Box 8 fällt auf 5, Box 6 auf 3,
darunter auf 1. Ein Totalabsturz nach einem einzigen Ausrutscher drillt
Bekanntes neu und ist der Hauptgrund, aus dem Leute Wiederholungs-Apps
aufgeben.

### Das Fahrschul-Gefühl steckt in der Reihenfolge

Ein verpasstes Zeichen kommt **nach genau drei anderen Fragen** wieder, und
noch einmal nach zehn. Feste Abstände, nicht zufällige — du lernst unterbewusst
„das kommt zurück", und das ist der ganze psychologische Motor. Aufgelöst ist
ein Fehler erst, wenn das Zeichen in derselben Runde **zweimal** saß.

### Prüfungsreif

Die Zahl auf der Startseite. Ein Zeichen zählt voll erst, wenn es in Box 6
oder höher steht, dreimal in Folge saß, in den letzten 20 Antworten 90 %
Trefferquote hat **und** im Schnitt unter 4 Sekunden beantwortet wird. Kana in
acht Sekunden pro Zeichen zu lesen ist kein Lesen.

Zwei Eigenschaften sind Absicht:

- **Der Nenner ist alles**, gesperrte Zeichen eingeschlossen. Du kannst nicht
  zu 80 % hiragana-prüfungsreif sein, wenn drei Zeilen freigeschaltet sind.
  Die ehrliche Zahl ist die Schlagzeile, „x % der freigeschalteten" die graue
  Nebenangabe.
- **Ein Sockel von 70 %** für beherrschte Zeichen. Zwei Wochen Urlaub kosten
  dich von 100 % auf etwa 74 %, nicht auf 30 %. Der Totaleinbruch nach einer
  Pause ist das demotivierendste Verhalten, das so eine App zeigen kann.

### Verwechslungen werden benannt, nicht nur gezählt

Tippst du bei シ „tsu", ist das nicht „irgendwie falsch" — es ist die
klassische シ/ツ-Verwechslung. Die App erkennt das, weil deine Eingabe ein
*anderes* Zeichen benennt, und sagt dir, was den Unterschied macht:

> シ hat die Striche links und öffnet nach rechts, ツ hat sie oben.

Dabei wird auch der Partner mit heruntergestuft: wer auf シ mit „tsu"
antwortet, hat beide unsicher — es ist derselbe undifferenzierte Klumpen.
Danach kommen die zwei abwechselnd eingestreut, denn verschachtelter Kontrast
ist die einzige Präsentationsform, die Unterscheidung wirklich aufbaut.

Die Prüfreihenfolge ist dabei tragend: erst exakter Treffer, dann
Verwechslung, dann Tippfehler. Andernfalls würde „hi" für し als Tippfehler
durchgehen (Abstand 1 zu „shi") statt als die Verwechslung mit ひ, die es ist.

### Der erste Kontakt ist keine Prüfung

Ein Zeichen, das du noch nie beantwortet hast, wird vorher vorgestellt. Die
erste Antwort danach ist Abschreiben aus dem Kurzzeitgedächtnis, nicht
Erinnern — sie zählt deshalb nicht in die Trefferquote und erzeugt keinen
Fehler, selbst wenn du danebentippst. Ab dem zweiten Kontakt zählt alles
normal. Ein Zeichen, das du schon einmal gesehen hast, bekommt **keine** Karte
mehr: kurz vorher nochmal hinsehen würde das Erinnern ersetzen.

### Tippfehler sind neutral

`kittte` statt `kitte` kostet nichts: keine Box-Änderung, kein Streak-Verlust,
die Frage kommt sofort wieder. Zweimal hintereinander ist es allerdings kein
Tippfehler mehr, sondern ein Fehler — sonst ließe sich die Statistik durch
hartnäckiges Danebentippen beliebig schönen.

### Die Freischaltleiter

Alles auf einmal wäre erdrückend, deshalb geht es Zeile für Zeile: Hiragana
Gojūon → Dakuten → Handakuten → Yōon, dann dasselbe für Katakana, insgesamt
40 Gruppen. Eine Gruppe gilt als geschafft, wenn jedes Zeichen mindestens in
Box 3 steht und die mittlere Trefferquote 85 % erreicht. Katakana öffnet ab
70 % Hiragana-Prüfungsreife — シ direkt nach し zu lernen ist kontrastiv
nützlich.

Dazu Tagesgrenzen: eine Gruppe und zehn neue Zeichen pro Tag. Ohne die
schaltet ein euphorischer erster Tag 46 Zeichen frei und man erstickt an Tag
drei in Wiederholungen.

**Wenn du schon Kana kennst, musst du nicht warten.** In den Einstellungen
steht die volle Liste aller Gruppen mit einem „bis hier"-Knopf, der alles
davor aufmacht — ohne Rücksicht auf Meisterung und Tagesgrenze.

---

## Der Datensatz

| Gruppe | pro Schrift |
|---|---|
| Gojūon (あ–ん) | 46 |
| Dakuten (が–ぼ) | 20 |
| Handakuten (ぱ–ぽ) | 5 |
| Yōon (きゃ–りょ) | 33 |
| **Summe** | **104** |

Beide Schriften also 208 Zeichen, dazu 22 Wort-Items — denn っ, ッ und ー haben
**keine eigene Lesung**. Sie isoliert abzufragen wäre Unsinn, also werden sie
an Wörtern geübt (きって, コップ, ラーメン …): das Lernziel ist die Regel, nicht
das Zeichen.

Die Tabelle wird aus einer handgeschriebenen Hiragana-Seed-Matrix mechanisch
erzeugt — Katakana über den Unicode-Versatz 0x60, Dakuten über +1, Handakuten
über +2 auf dem Basiszeichen. 208 Zeilen von Hand einzutippen lädt zu 208
Tippfehlern ein; fünf Tabellen plus Regeln zu fünf, und die fängt ein Test.

Bei den Langvokal-Wörtern ist die kanonische Antwort die tippbare
Doppelvokal-Form (`raamen`), nicht die Makron-Form: `rāmen` lässt sich mit 22
Buchstaben nicht schreiben. Sie gilt trotzdem als gleichwertig.

---

## Einstellungen

| | |
|---|---|
| **Freischaltung** | Volle Gruppenliste mit Status, „bis hier" zum Vorspulen, „sperren" zum Zurücknehmen |
| **Lernen** | Neue Zeichen pro Tag, Gruppen pro Tag (bis „ohne Grenze"), Fragen pro Runde |
| **Antworten** | Nur Hepburn als richtig zählen |
| **Ton** | Aussprache automatisch abspielen |
| **Darstellung** | Design hell/dunkel/System, Bildschirmtastatur automatisch/immer/nie |

---

## Bauen und starten

Voraussetzungen: JDK 17 oder neuer, für Android ein Android SDK mit Platform
37 (Pfad in `local.properties` als `sdk.dir`).

```bash
./gradlew :shared:run                 # Desktop starten
./gradlew :androidApp:installDebug    # aufs angeschlossene Gerät
./gradlew :shared:check               # 164 Tests
```

Release-Artefakte:

```bash
./gradlew :androidApp:assembleRelease                # APK
./gradlew :shared:createReleaseDistributable         # App-Image mit eigener JRE
./gradlew :shared:packageReleaseUberJarForCurrentOS  # JAR, braucht ein JVM
```

Die Release-APK wird signiert, wenn eine `keystore.properties` im
Projektwurzelverzeichnis liegt (`storeFile`, `storePassword`, `keyAlias`,
`keyPassword`). Fehlt sie, bleibt der Build unsigniert statt fehlzuschlagen.
Die Datei und Schlüsseldateien sind aus der Versionskontrolle ausgenommen.

Kein `.deb`: jpackage braucht dafür `dpkg-deb`, das es auf Arch nicht gibt.

### Wo der Lernstand liegt

| | |
|---|---|
| Desktop | `${XDG_DATA_HOME:-~/.local/share}/kana-schule/` |
| Android | `/data/data/moe.rorita.kanaschule/files/` |

Darin `state.json` (Lernstand, Einstellungen, Tagesaggregate), `state.json.bak`
als Sicherung von vor dem letzten Schreiben und `reviews.jsonl` als
Antwortprotokoll. Geschrieben wird über eine Nebendatei und ein atomares
Umbenennen — ohne das zerstört ein Prozessabbruch mitten im Schreiben den
gesamten Lernstand.

Zum Zurücksetzen genügt es, das Verzeichnis zu löschen.

---

## Aussprache-Aufnahmen: Danke an Tofugu

Die 104 Aufnahmen stammen aus dem ausgezeichneten Lehrartikel
**[Learn Hiragana von Tofugu](https://www.tofugu.com/japanese/learn-hiragana/)**
— eine der besten frei zugänglichen Einführungen ins Kana-Lernen, und
lesenswert unabhängig von dieser App. Das Urheberrecht an den Aufnahmen liegt
bei [Tofugu](https://www.tofugu.com/).

**Stand der Erlaubnis:** angefragt, noch nicht erteilt. Bis dahin sind die
Dateien ein Platzhalter für die Entwicklung und die App bleibt
unveröffentlicht. Kommt keine Zusage, werden sie durch eigene oder frei
lizenzierte Aufnahmen ersetzt. Details in
`shared/media/audio/HERKUNFT.md`.

104 Aufnahmen decken 208 Zeichen ab, weil か und カ identisch klingen. Sie
liegen als WAV im Repository statt als MP3: die Desktop-JVM spielt ohne
Zusatzbibliothek nur WAV, und gebündeltes Audio hält die App offline und ohne
Netzwerkberechtigung.

---

## Was noch fehlt

- **Verwechslungs-Drill** mit Duell-Karten: beide verwechselbaren Zeichen
  nebeneinander, beide Lesungen hintereinander eintippen. Reines
  Einzelzeichen-Tippen lässt einen per Grobform-Erkennung durchrutschen.
- **Speed-Drill** mit Zeitdruck, um die Antwortzeit zu drücken — Latenz ist
  das ehrliche Fortschrittsmaß, sobald die Trefferquote nahe 100 % liegt.
- **Prüfungssimulation**: 60 Zeichen, keine Rückmeldung während des Laufs,
  Ergebnis am Ende. Die Prüfungsreif-Zahl ist ein Versprechen; ein bestandener
  Testlauf ist der Beweis dazu.
- **Statistikseite** mit Reihen-Heatmap und den häufigsten Verwechslungen.
- **Richtung Romaji → Kana**: Zeichen aus einer Kana-Tastatur zusammensetzen.
  Auch das ist freie Produktion, kein Ausschlussverfahren.
- Gebündelte japanische Schriftart, damit ein Desktop-Paket auf einem System
  ohne CJK-Schriften keine leeren Kästchen zeigt.

---

Persönliches Projekt, nicht veröffentlicht. Windows, macOS, iOS und Web sind
kein Ziel.
