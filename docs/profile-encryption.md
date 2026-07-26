# At-rest encryption of user data

Everything this IME stores about what the user types is encrypted with a key held in the
**Android Keystore**, where on devices with a secure element it is not extractable at all.

| File | Written by | Protected by |
| --- | --- | --- |
| `files/user_dictionary.enc` | our GUI user dictionary | `SecureStore` (AES-GCM, Keystore) |
| `files/mozc/user_dictionary.db` | mozc | `EncryptedStringStorage`, key from Keystore |
| `files/mozc/.history.db` | mozc (conversion learning) | `EncryptedStringStorage`, key from Keystore |

`android:allowBackup="false"` keeps all three out of cloud backup.

This is not a substitute for the OS's full-disk encryption, which already covers the flash chip.
What it adds is protection against a backup, an unlocked device being inspected, and `adb pull` on
a debuggable build.

## Why mozc needed patching

mozc already encrypts `.history.db`. But its `PasswordManager` picks an implementation by platform,
and Android matches the **Linux** branch — `PlainPasswordManager`, which writes the key to
`.encrypt_key.db` **in the same directory as the data it protects**. Anyone who can read one can
read the other, so it protected nothing. `user_dictionary.db` had no encryption at all.

Both are fixed by `patches/0002-android-keystore-profile-encryption.patch`.

## How the key gets in

The key is **injected from Java**, not fetched by C++ calling back into Java. The handover plan
originally proposed the callback direction; injection turned out to be strictly simpler and avoids
`AttachCurrentThread`/`DetachCurrentThread` pairing on mozc's conversion threads entirely.

1. `MozcProfileKey.install()` produces 32 random bytes (mozc's `kPasswordSize`; it rejects any
   other length), sealed by `SecureStore` into `files/mozc_profile_key.enc`.
2. It passes them to the new native `MozcJNI.setEncryptionKey([B)Z`.
3. `PasswordManager::SetInjectedPassword` parks them in a mutex-guarded global.
4. `AndroidPasswordManager` — a new branch that must come **before** the Linux one, since Android
   defines `__linux__` too — serves them to `EncryptedStringStorage`.

Ordering matters: step 1 runs **before `onPostLoad`**, because building the engine reads the
history.

`AndroidPasswordManager::SetPassword` deliberately fails instead of generating a password. It could
not persist one, so every launch would encrypt the history under a key that dies with the process,
silently discarding the previous run's learning. Failing leaves the existing file untouched: the
IME still converts, it just stops persisting what it learns.

## Migration

- **The plain-text key**: if `.encrypt_key.db` exists from an earlier build, its bytes are adopted
  as the key and sealed, so history encrypted under it stays readable. Only then is the plain-text
  copy unlinked.
- **The unencrypted dictionary**: `LoadInternal` tries decryption, then falls back to parsing the
  file as plain protobuf. The next `Save()` writes it back encrypted — which happens on the first
  launch, because `UserDictionary.sync()` fires `IMPORT_USER_DICTIONARY` unconditionally at
  start-up.
- A file that decrypts to nothing **and** does not parse as plain protobuf is reported as
  `DataLossError`, never as an empty dictionary. A missing key must not read as "no words".

## Verified on host

`bazel test //dictionary:user_dictionary_storage_test --config oss_linux` — 14/14. Its `LockTest`
caught a real bug: `Encryptor::EncryptString` rejects empty input, so an empty dictionary could not
be saved. An empty dictionary is now written as a zero-byte file, which reads back as empty.

A throwaway harness additionally confirmed, on the host build: the saved file contains neither the
entry nor the dictionary name in the clear; entries survive the round trip; a legacy plain-text
dictionary loads and is re-encrypted on save; an empty dictionary round-trips; and garbage is an
error rather than a silent empty read.

## Still to verify on device

The `__ANDROID__` branch cannot run on the host, so `converter_main` will not exercise it. On a
real device:

- register a word → restart the app → it is still there
- converse a while → restart → the learning still applies
- `adb shell run-as me.zssu.ime.debug ls files/mozc/` — `.encrypt_key.db` should be **absent**
- `adb shell run-as me.zssu.ime.debug cat files/mozc/user_dictionary.db` — no readable words
- clear app data → the keystore key is gone → must start empty rather than crash

## Known bound

`EncryptedStringStorage` refuses to read anything over 64MB, below the 512MB the protobuf parser
would accept. The bundled dictionaries are ~8MB of TSV, so there is a wide margin, but a user with
a several-hundred-megabyte dictionary would now fail to load it.
