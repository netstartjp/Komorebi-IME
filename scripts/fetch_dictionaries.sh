#!/usr/bin/env bash
#
# Downloads the bundled supplementary dictionaries into the :mozc module's assets.
#
# These are fetched at build time and shipped inside the APK — the app itself never touches the
# network. Re-run to pick up a newer snapshot.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="${ROOT}/mozc/src/main/assets/dictionaries"

mkdir -p "${DEST}"

# KEINOS/google-ime-user-dictionary-ja-en — katakana loanword to English spelling, derived from
# EDICT. Ships as a directory of files split at Google IME's old 10,000-row limit, so they are
# concatenated back into one; mozc has no such limit.
JA_EN_URL="https://github.com/KEINOS/google-ime-user-dictionary-ja-en/archive/master.zip"
JA_EN_FILE="katakana-english.txt"

echo "==> Fetching google-ime-user-dictionary-ja-en"
work="$(mktemp -d)"
trap 'rm -rf "${work}"' EXIT
curl -fsSL -o "${work}/master.zip" "${JA_EN_URL}"
unzip -q "${work}/master.zip" -d "${work}"

# The repository also carries .docx files that were saved with a .txt suffix, and other dictionaries
# we do not want. Take only the katakana-English directory, and only lines that actually parse as
# `reading <tab> word <tab> part-of-speech` — that rejects the Word documents wholesale.
#
# Then keep only the entries that are transliterations. The source is EDICT, a translation
# dictionary, so it also answers 黒 with "black" and そ with the definition of the solfa syllable —
# which surfaces as English prose in the middle of ordinary Japanese input. See the filter script.
merged="${work}/merged.txt"
find "${work}" -path "*Google-ime-jp-カタカナ英語辞典*" -name "*.txt" -exec cat {} + 2>/dev/null |
  awk -F"\t" 'NF >= 3 && $1 ~ /^[ぁ-ゖー]+$/ && $2 != "" {print $1 "\t" $2 "\t" $3}' |
  "${PYTHON:-python3}" "${ROOT}/scripts/filter_katakana_english.py" > "${merged}"

ja_en_entries="$(grep -cv '^#' "${merged}" || true)"
if [[ "${ja_en_entries}" -lt 10000 ]]; then
  echo "error: only ${ja_en_entries} katakana-English entries; refusing to install" >&2
  exit 1
fi
install -m 644 "${merged}" "${DEST}/${JA_EN_FILE}"
echo "    ${ja_en_entries} entries -> ${DEST}/${JA_EN_FILE}"
