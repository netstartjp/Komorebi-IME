# Vendored Karukan engine

- Upstream: https://github.com/togatoga/karukan
- Revision: `7756d68c725ea2c6e611618af79e06b6363275db`
- Retrieved: 2026-07-28
- License: MIT OR Apache-2.0

Komorebi IME vendors only `karukan-engine` and its data. The Android integration adds:

- `Backend::from_files` so the app, rather than the desktop Hugging Face cache, owns downloads;
- a `model-download` feature so Android excludes the desktop downloader;
- an Android JNI `cdylib`;
- a reduced Tokenizers feature set (no training acceleration or progress UI).

The upstream source licenses and third-party notices are retained beside this file and are copied
into the APK's in-app license screen.
