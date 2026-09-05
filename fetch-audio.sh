#!/usr/bin/env bash
#
# Holt die Aussprache-Aufnahmen von Tofugu und legt sie unter
# shared/media/audio ab. Die Dateien liegen bewusst nicht im Repository:
# das Urheberrecht liegt bei Tofugu, eine Erlaubnis ist angefragt und
# nicht erteilt. Siehe shared/media/audio/HERKUNFT.md.
#
#   ./fetch-audio.sh              fehlende Aufnahmen holen
#   ./fetch-audio.sh --force      auch vorhandene neu holen
#   ./fetch-audio.sh --check      nur sagen, was fehlt
#
# Gebraucht werden curl und ffmpeg. Konvertiert wird nach WAV (mono,
# 22050 Hz, 16 Bit), weil die Desktop-JVM ohne Zusatzbibliothek nur WAV,
# AIFF und AU abspielen kann.
#
set -euo pipefail

cd "$(dirname "$(readlink -f "$0")")"

TARGET_DIR="shared/media/audio"
BASE_URL="https://files.tofugu.com/articles/japanese/2014-06-30-learn-hiragana"
SUFFIX="_v2.mp3"

FORCE=0
CHECK_ONLY=0

for arg in "$@"; do
    case "$arg" in
        --force) FORCE=1 ;;
        --check) CHECK_ONLY=1 ;;
        -h|--help)
            sed -n '3,15p' "$0" | sed 's/^# \?//'
            exit 0
            ;;
        *)
            echo "Unbekanntes Argument: $arg" >&2
            exit 1
            ;;
    esac
done

# Der Dateiname ist der Slug aus der Kana-Tabelle, die Quelle ist nach dem
# Zeichen benannt. Hiragana und Katakana klingen gleich, eine Datei je Lesung
# deckt also beide Schriften ab: 104 Dateien für 208 Zeichen.
#
# Diese Liste muss zu Pronunciation.audioName passen; AudioSourcesTest
# vergleicht beides, damit sie nicht auseinanderläuft.
RECORDINGS=(
    "a あ"
    "ba ば"
    "be べ"
    "bi び"
    "bo ぼ"
    "bu ぶ"
    "bya びゃ"
    "byo びょ"
    "byu びゅ"
    "cha ちゃ"
    "chi ち"
    "cho ちょ"
    "chu ちゅ"
    "da だ"
    "de で"
    "di ぢ"
    "do ど"
    "du づ"
    "e え"
    "fu ふ"
    "ga が"
    "ge げ"
    "gi ぎ"
    "go ご"
    "gu ぐ"
    "gya ぎゃ"
    "gyo ぎょ"
    "gyu ぎゅ"
    "ha は"
    "he へ"
    "hi ひ"
    "ho ほ"
    "hya ひゃ"
    "hyo ひょ"
    "hyu ひゅ"
    "i い"
    "ja じゃ"
    "ji じ"
    "jo じょ"
    "ju じゅ"
    "ka か"
    "ke け"
    "ki き"
    "ko こ"
    "ku く"
    "kya きゃ"
    "kyo きょ"
    "kyu きゅ"
    "ma ま"
    "me め"
    "mi み"
    "mo も"
    "mu む"
    "mya みゃ"
    "myo みょ"
    "myu みゅ"
    "na な"
    "ne ね"
    "ni に"
    "no の"
    "nu ぬ"
    "nya にゃ"
    "nyo にょ"
    "nyu にゅ"
    "n ん"
    "o お"
    "pa ぱ"
    "pe ぺ"
    "pi ぴ"
    "po ぽ"
    "pu ぷ"
    "pya ぴゃ"
    "pyo ぴょ"
    "pyu ぴゅ"
    "ra ら"
    "re れ"
    "ri り"
    "ro ろ"
    "ru る"
    "rya りゃ"
    "ryo りょ"
    "ryu りゅ"
    "sa さ"
    "se せ"
    "sha しゃ"
    "shi し"
    "sho しょ"
    "shu しゅ"
    "so そ"
    "su す"
    "ta た"
    "te て"
    "to と"
    "tsu つ"
    "u う"
    "wa わ"
    "wo を"
    "ya や"
    "yo よ"
    "yu ゆ"
    "za ざ"
    "ze ぜ"
    "zo ぞ"
    "zu ず"
)

for tool in curl ffmpeg; do
    if ! command -v "$tool" >/dev/null 2>&1; then
        echo "$tool wird gebraucht, ist aber nicht installiert." >&2
        exit 1
    fi
done

mkdir -p "$TARGET_DIR"

missing=()
for entry in "${RECORDINGS[@]}"; do
    slug="${entry%% *}"
    if [[ $FORCE -eq 0 && -s "$TARGET_DIR/$slug.wav" ]]; then
        continue
    fi
    missing+=("$entry")
done

if [[ ${#missing[@]} -eq 0 ]]; then
    echo "Alle ${#RECORDINGS[@]} Aufnahmen liegen vor."
    exit 0
fi

if [[ $CHECK_ONLY -eq 1 ]]; then
    echo "Es fehlen ${#missing[@]} von ${#RECORDINGS[@]} Aufnahmen:"
    printf '  %s\n' "${missing[@]%% *}"
    exit 1
fi

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

echo "Hole ${#missing[@]} Aufnahmen von tofugu.com ..."

failed=()
for entry in "${missing[@]}"; do
    slug="${entry%% *}"
    glyph="${entry##* }"

    if ! curl -sfL --max-time 30 "$BASE_URL/$glyph$SUFFIX" -o "$tmp/$slug.mp3"; then
        failed+=("$slug ($glyph)")
        continue
    fi

    # In eine Nachbardatei schreiben und erst danach verschieben: ein
    # abgebrochener Lauf soll keine halbe WAV-Datei hinterlassen, die der
    # nächste Lauf für fertig hält.
    if ! ffmpeg -hide_banner -loglevel error -y \
        -i "$tmp/$slug.mp3" -ac 1 -ar 22050 -sample_fmt s16 "$tmp/$slug.wav"; then
        failed+=("$slug ($glyph, Konvertierung)")
        continue
    fi
    mv "$tmp/$slug.wav" "$TARGET_DIR/$slug.wav"
    printf '.'
done
printf '\n'

if [[ ${#failed[@]} -gt 0 ]]; then
    echo "Fehlgeschlagen:" >&2
    printf '  %s\n' "${failed[@]}" >&2
    exit 1
fi

echo "Fertig. $TARGET_DIR enthält jetzt $(ls -1 "$TARGET_DIR"/*.wav | wc -l) Aufnahmen."
