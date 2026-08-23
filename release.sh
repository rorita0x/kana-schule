#!/usr/bin/env bash
#
# Baut die Release-Artefakte für Linux und Android und sagt am Ende, wo sie
# liegen.
#
#   ./release.sh              alles
#   ./release.sh linux        nur Desktop und AppImage
#   ./release.sh android      nur APK
#   ./release.sh --no-appimage
#
set -euo pipefail

cd "$(dirname "$(readlink -f "$0")")"

APP_NAME="kana-schule"
APP_LABEL="Kana Führerschein"
ARCH_NAME="x86_64"

DIST_DIR="shared/build/compose/binaries/main-release/app/${APP_NAME}"
JAR_DIR="shared/build/compose/jars"
APK="androidApp/build/outputs/apk/release/androidApp-release.apk"
APPIMAGE_OUT="build/appimage"
APPIMAGETOOL="${XDG_CACHE_HOME:-$HOME/.cache}/kana-schule/appimagetool-${ARCH_NAME}.AppImage"
APPIMAGETOOL_URL="https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-${ARCH_NAME}.AppImage"

DO_LINUX=1
DO_ANDROID=1
DO_APPIMAGE=1

for arg in "$@"; do
    case "$arg" in
        linux) DO_ANDROID=0 ;;
        android) DO_LINUX=0; DO_APPIMAGE=0 ;;
        --no-appimage) DO_APPIMAGE=0 ;;
        -h|--help)
            sed -n '3,10p' "$0" | sed 's/^# \?//'
            exit 0
            ;;
        *)
            echo "Unbekanntes Argument: $arg" >&2
            exit 2
            ;;
    esac
done

step()  { printf '\n\033[1;34m==>\033[0m \033[1m%s\033[0m\n' "$1"; }
note()  { printf '    %s\n' "$1"; }
warn()  { printf '\033[1;33m    Achtung:\033[0m %s\n' "$1"; }
fail()  { printf '\033[1;31mFehler:\033[0m %s\n' "$1" >&2; exit 1; }

human() { du -sh "$1" 2>/dev/null | cut -f1; }

# --------------------------------------------------------------- Vorbedingungen

[ -x ./gradlew ] || fail "./gradlew fehlt - im Projektverzeichnis ausführen."

if [ "$DO_ANDROID" = 1 ]; then
    if [ ! -f local.properties ] && [ -z "${ANDROID_HOME:-}" ]; then
        fail "Android SDK nicht gefunden. sdk.dir in local.properties setzen oder ANDROID_HOME exportieren."
    fi
    if [ ! -f keystore.properties ]; then
        warn "keystore.properties fehlt - das APK wird unsigniert und lässt sich nicht installieren."
        warn "Anlegen mit storeFile, storePassword, keyAlias, keyPassword."
    fi
fi

# ---------------------------------------------------------------------- Desktop

if [ "$DO_LINUX" = 1 ]; then
    step "Desktop-Anwendung bauen"
    ./gradlew --quiet :shared:createReleaseDistributable :shared:packageReleaseUberJarForCurrentOS
    [ -x "${DIST_DIR}/bin/${APP_NAME}" ] || fail "Startprogramm nicht gefunden: ${DIST_DIR}/bin/${APP_NAME}"
    note "fertig"
fi

# --------------------------------------------------------------------- AppImage

if [ "$DO_APPIMAGE" = 1 ]; then
    step "AppImage bauen"

    if [ ! -x "$APPIMAGETOOL" ]; then
        note "appimagetool wird einmalig heruntergeladen nach ${APPIMAGETOOL}"
        mkdir -p "$(dirname "$APPIMAGETOOL")"
        curl -fsSL -o "$APPIMAGETOOL" "$APPIMAGETOOL_URL" ||
            fail "Download von appimagetool fehlgeschlagen: $APPIMAGETOOL_URL"
        chmod +x "$APPIMAGETOOL"
    fi

    APPDIR="${APPIMAGE_OUT}/${APP_NAME}.AppDir"
    rm -rf "$APPDIR"
    mkdir -p "${APPDIR}/usr"

    # Die jpackage-Struktur bleibt erhalten: das Startprogramm sucht seine
    # Bibliotheken relativ zu sich selbst unter ../lib.
    cp -r "${DIST_DIR}/." "${APPDIR}/usr/"

    cp packaging/${APP_NAME}.png "${APPDIR}/${APP_NAME}.png"
    cp packaging/${APP_NAME}.desktop "${APPDIR}/${APP_NAME}.desktop"

    # Auch als installierte Ablage, damit Desktop-Umgebungen das Symbol finden.
    mkdir -p "${APPDIR}/usr/share/icons/hicolor/512x512/apps" "${APPDIR}/usr/share/applications"
    cp packaging/${APP_NAME}.png "${APPDIR}/usr/share/icons/hicolor/512x512/apps/${APP_NAME}.png"
    cp packaging/${APP_NAME}.desktop "${APPDIR}/usr/share/applications/${APP_NAME}.desktop"

    cat > "${APPDIR}/AppRun" <<'APPRUN'
#!/bin/sh
HERE="$(dirname "$(readlink -f "$0")")"
exec "${HERE}/usr/bin/kana-schule" "$@"
APPRUN
    chmod +x "${APPDIR}/AppRun"

    APPIMAGE_FILE="${APPIMAGE_OUT}/Kana-Fuehrerschein-${ARCH_NAME}.AppImage"
    rm -f "$APPIMAGE_FILE"
    # extract-and-run, damit es auch ohne nutzbares FUSE durchläuft.
    ARCH="$ARCH_NAME" "$APPIMAGETOOL" --appimage-extract-and-run \
        "$APPDIR" "$APPIMAGE_FILE" >/dev/null 2>"${APPIMAGE_OUT}/appimagetool.log" ||
        { cat "${APPIMAGE_OUT}/appimagetool.log" >&2; fail "appimagetool fehlgeschlagen"; }
    chmod +x "$APPIMAGE_FILE"
    note "fertig"
fi

# ---------------------------------------------------------------------- Android

if [ "$DO_ANDROID" = 1 ]; then
    step "Android-APK bauen"
    ./gradlew --quiet :androidApp:assembleRelease
    [ -f "$APK" ] || fail "APK nicht gefunden: $APK"

    SIGNER="$(ls -d "${ANDROID_HOME:-$HOME/Android/Sdk}"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1 || true)"
    if [ -n "$SIGNER" ] && [ -x "$SIGNER" ]; then
        if "$SIGNER" verify "$APK" >/dev/null 2>&1; then
            note "Signatur geprüft: in Ordnung"
        else
            warn "Das APK ist NICHT signiert und lässt sich so nicht installieren."
        fi
    else
        warn "apksigner nicht gefunden, Signatur nicht geprüft."
    fi
fi

# ----------------------------------------------------------------------- Bilanz

printf '\n\033[1;32m==> Fertig\033[0m  %s\n\n' "$APP_LABEL"

if [ "$DO_LINUX" = 1 ]; then
    printf '  \033[1mLinux\033[0m\n'
    if [ "$DO_APPIMAGE" = 1 ]; then
        printf '    AppImage   %-6s %s\n' "$(human "$APPIMAGE_FILE")" "$PWD/$APPIMAGE_FILE"
    fi
    printf '    Programm   %-6s %s\n' "$(human "$DIST_DIR")" "$PWD/${DIST_DIR}/bin/${APP_NAME}"
    JAR="$(ls -t "${JAR_DIR}"/*release*.jar 2>/dev/null | head -1 || true)"
    [ -n "$JAR" ] && printf '    Uber-JAR   %-6s %s\n' "$(human "$JAR")" "$PWD/$JAR"
    echo
fi

if [ "$DO_ANDROID" = 1 ]; then
    printf '  \033[1mAndroid\033[0m\n'
    printf '    APK        %-6s %s\n' "$(human "$APK")" "$PWD/$APK"
    echo
    printf '    Installieren:  adb install -r "%s"\n' "$PWD/$APK"
    printf '    Bei INSTALL_FAILED_UPDATE_INCOMPATIBLE erst die Debug-Version entfernen:\n'
    printf '                   adb uninstall moe.rorita.kanaschule\n'
    echo
fi
