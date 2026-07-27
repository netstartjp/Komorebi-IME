# Release license audit

This checklist is part of the release process. It is not legal advice.

## Every release

1. Run `./gradlew :app:dependencies --configuration releaseRuntimeClasspath`.
2. Compare all resolved runtime component families and versions with
   `app/src/main/assets/legal/THIRD_PARTY_NOTICES.txt`.
3. Re-copy the Mozc `LICENSE` and `src/data/dictionary_oss/README.txt` from the
   exact Mozc revision used to build the native runtime.
4. Run `scripts/fetch_dictionaries.sh` and confirm every distributed dictionary
   starts with source, license URL, modification summary, and downstream license.
5. Build the release candidate and inspect `assets/legal/`, `assets/dictionaries/`,
   `assets/mozc.data`, and every `lib/*/libmozc.so` in the APK.
6. Open Settings → Open source licenses on a device and verify every document is
   readable without network access.
7. Confirm copied Material icon vector files retain the AOSP copyright,
   Apache-2.0 notice, and conversion note, and that Material Icons remains
   listed in both NOTICE files and `THIRD_PARTY_NOTICES.txt`.
8. Do not add data whose upstream terms merely disclaim copyright or whose source
   rights are unclear. Require an affirmative redistribution license.

## Files that must be in the APK

- `assets/legal/LICENSE.txt`
- `assets/legal/NOTICE.txt`
- `assets/legal/PRIVACY.txt`
- `assets/legal/THIRD_PARTY_NOTICES.txt`
- `assets/legal/MOZC_LICENSE.txt`
- `assets/legal/MOZC_DICTIONARY_LICENSES.txt`

The Git repository intentionally excludes downloaded dictionaries and Mozc build
outputs. Legal files are tracked because a source-only checkout must still state
the terms of the binary it is designed to produce.
