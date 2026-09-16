# Android build — offline APK for field use

Written for: the programme owner and whoever helps with IT. No Android experience assumed.

The APK is a thin WebView wrapper around the same `index.html` that runs the website. The
whole guide is bundled inside the app, so it works with **no internet, ever**. The app
requests **no permissions at all** — no internet, no storage, no location.

---

## Read this before distributing

An installed APK **cannot update itself**. This is a dosing tool, so that matters:

> If the national protocol changes, every phone still carrying an old APK keeps giving
> the old doses, and nothing on that phone will say so.

Two things follow, and neither is optional:

1. **Check the version stamp.** Every screen carries it in the footer —
   `Version 1.0.0 · Updated 16 September 2026`. Have supervisors read it off handsets at
   monthly meetings. It is the only way to find a worker running an old copy.
2. **Keep a distribution list.** Know which workers received which version, so you can
   chase them when a new one ships.

If the workers have internet even occasionally — at a monthly meeting, at the BHU, at
home — prefer the website with **Add to Home Screen**. It looks and behaves the same,
works offline after one visit, and updates itself. The APK is for handsets that genuinely
never connect.

---

## Building it

You do **not** need Android Studio, or any Android tooling on your own machine. GitHub
builds the APK.

### Every release

1. Push your change to `main` as usual.
2. Tag it:

   ```bash
   git tag v1.0.1
   git push origin v1.0.1
   ```

3. GitHub Actions builds the APK and attaches it to the release. Download it from the
   **Releases** page and forward it on WhatsApp.

To build without tagging — to test something — open **Actions → Build Android APK → Run
workflow**. The APK appears as a downloadable artifact on the run.

### Versioning

`APP_VERSION` in `index.html` is the single source of truth. The build reads it, so the
version on the launcher and the version in the footer can never drift apart.

On every content change, bump all three together:

| Where | What |
|---|---|
| `index.html` | `APP_VERSION` |
| `index.html` | `buildDate` in **both** `S.en` and `S.ur` |
| `sw.js` | `CACHE` |

---

## Signing key — set this up once

Until you do this, builds fall back to Android's debug key. A debug-signed APK installs
and runs fine for testing, but **do not give one to the field**: it is marked debuggable,
and a later properly-signed APK cannot upgrade over it — workers would have to uninstall
first and lose their saved care-setting.

Create the key (needs a JDK; any machine with Android Studio or Java will do):

```bash
keytool -genkeypair -v \
  -keystore release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias malaria-guide
```

> **Back up `release.jks` and its passwords somewhere safe and shared** — a programme
> password manager, not one person's laptop. If you lose this file you can never ship an
> update to anyone who already installed the app. They would have to uninstall and
> reinstall, losing their settings. This is the single most common way small health-app
> projects get stuck.

Then turn it into a secret:

```bash
base64 -w0 release.jks > release.b64     # macOS: base64 -i release.jks -o release.b64
```

In GitHub → **Settings → Secrets and variables → Actions**, add four repository secrets:

| Secret | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | the contents of `release.b64` |
| `ANDROID_KEYSTORE_PASSWORD` | the keystore password |
| `ANDROID_KEY_ALIAS` | `malaria-guide` |
| `ANDROID_KEY_PASSWORD` | the key password |

Never commit `release.jks` — `android/.gitignore` already blocks `*.jks`.

---

## Installing on a worker's phone

1. Send the APK over WhatsApp and tap it, or copy it via cable/Bluetooth.
2. Android will say the source isn't allowed. Tap **Settings**, turn on
   *Allow from this source* for WhatsApp/Files, then go back.
3. Play Protect may warn *"Unsafe app blocked"*. Tap **More details → Install anyway**.
   This is expected for any app not distributed through the Play Store.
4. Open it. Pick a care setting and a language once; it remembers both.

**Test on one real handset from the field before distributing.** Some organisation phones
are MDM-managed and block sideloading outright — better to discover that on one phone
than on two hundred.

---

## What is in here

| Path | Purpose |
|---|---|
| `app/src/main/java/.../MainActivity.java` | the WebView wrapper |
| `app/src/main/AndroidManifest.xml` | app declaration; deliberately no permissions |
| `app/build.gradle` | build config, signing, version injection |
| `app/src/main/res/mipmap-*/` | launcher icons |
| `app/src/main/assets/` | **generated at build time** — not in git |

### Two settings that must not be removed from `MainActivity`

- `setJavaScriptEnabled(true)` — the guideline engine is JavaScript.
- `setDomStorageEnabled(true)` — the care-setting gate and the language choice use
  `localStorage`. Without it the app re-asks *"Where are you working?"* on every launch
  and forgets Urdu every time.

The app loads `file:///android_asset/index.html` on purpose. The page skips service-worker
registration on non-`http(s)` origins, so on `file://` it never attempts a network fetch
that would time out.

### Icons are placeholders

`mipmap-*/ic_launcher.png` is rendered from the inline SVG recreation of the Indus
Hospital mark, not the official brand asset. Replace all five sizes (48/72/96/144/192 px)
when the real logo is available.
