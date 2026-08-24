# EasyLink Play Store release checklist

## Replace placeholders

- Publish `docs/PRIVACY_POLICY.md` on the EasyLink website.
- Replace every value in `AppLinks.kt` with the production website and support address.
- Replace the privacy-policy effective date and publisher identity.

## Product verification

- Test notification access onboarding on Android 9, 12, 13, 14, 15, and the current target version.
- Test WhatsApp, Telegram, Messages, Gmail, and at least one unknown notification layout.
- Verify share-sheet capture, multiple-link capture, duplicates, reactions, edited notifications, suspicious-link confirmation, and disabled source apps.
- Test light, dark, and system appearance with large fonts and screen readers.
- Test database migration from version 1 and fresh installation.
- Test offline behavior, slow previews, redirects, oversized pages/images, and preview-disabled mode.
- Review Android vitals during closed testing.

## Build and listing

- Create and securely back up a production upload key; never commit it.
- Increment `versionCode` for every uploaded build.
- Build and upload the release `.aab`.
- Complete Data Safety using the actual final behavior.
- Provide the published privacy-policy URL.
- Complete content rating, app access, ads, target audience, and notification-access disclosure sections.
- Prepare phone/tablet screenshots, 512×512 icon, feature graphic, short description, and full description.
- Complete any closed-testing requirement shown for this Play Console account.

## Release gates

- `./gradlew testDebugUnitTest`
- `./gradlew lintRelease`
- `./gradlew assembleDebug assembleRelease bundleRelease`
- `./gradlew connectedDebugAndroidTest` on an emulator/device
- Manual smoke test on at least two physical phones before production rollout
