# Herkunft der Audiodateien

Diese Aufnahmen stammen aus dem Hiragana-Lehrartikel von Tofugu:
https://www.tofugu.com/japanese/learn-hiragana/

Das Urheberrecht liegt bei Tofugu (https://www.tofugu.com/).

## Stand der Erlaubnis

Angefragt, **noch nicht erteilt**. Bis eine Zusage vorliegt, sind die Dateien
ein Platzhalter für die Entwicklung und die App bleibt unveröffentlicht.

- Kommt eine Zusage: Credits und Link bleiben in der README stehen, dazu die
  konkreten Bedingungen aus der Antwort hier ergänzen.
- Kommt keine Zusage oder keine Antwort: durch eigene oder frei lizenzierte
  Aufnahmen ersetzen. Der Dateiname ist der Slug aus der Kana-Tabelle
  (`ka.wav`, `shi.wav`, `kya.wav`, `di.wav`), also lässt sich der Satz
  komplett austauschen, ohne Code zu ändern.

## Technisches

Von MP3 nach WAV konvertiert (mono, 22050 Hz, 16 Bit), weil die Desktop-JVM
ohne zusätzliche Bibliothek nur WAV, AIFF und AU abspielen kann.

Da Hiragana und Katakana identisch klingen, deckt eine Datei je Lesung beide
Schriften ab: 104 Dateien für 208 Zeichen.

Ein Test (`AudioAssetsTest`) prüft, dass für jedes Zeichen eine lesbare Datei
vorliegt und dass keine überzähligen Dateien herumliegen.
