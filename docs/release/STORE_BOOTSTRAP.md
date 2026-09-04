# Store Bootstrap

This is the one-time account and app-record setup for Kvace 1.0.0. Complete it before approving a production workflow. The repository intentionally cannot create store accounts, accept legal agreements, grant production access, or mint long-lived credentials.

Never commit certificates, provisioning profiles, keystores, passwords, App Store Connect private keys, service-account JSON, access tokens, or decoded secret files. Production values belong only in protected GitHub environment secrets.

## Google Play

### 1. Establish the developer account

1. Register or select the developer account in [Play Console](https://play.google.com/console/), complete identity/contact verification, and pay any required registration fee.
2. Determine whether the account is an organization or personal account and record its creation date in the private release issue.
3. Accept the current Google Play Developer Distribution Agreement and complete all dashboard verification tasks.

Mandatory fork: if this is a **personal developer account created after 13 November 2023**, Production and pre-registration remain unavailable until Kvace runs a closed test with **at least 12 testers opted in continuously for at least 14 days**, followed by a successful production-access application. This is Google's current [testing requirement for new personal accounts](https://support.google.com/googleplay/android-developer/answer/14151465). Do not approve the direct-Production workflow until Play Console grants Production access. Older personal accounts and organization accounts follow the requirements shown in their own Console.

### 2. Create and complete the app record

1. Create the app in Play Console with package name `com.softartdev.kvace`, default language `en-US`, app name `Kvace`, and the correct app/game and free/paid declarations. Follow Google's [Create and set up your app](https://support.google.com/googleplay/android-developer/answer/9859152).
2. Enroll in [Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756). Securely create the upload keystore, keep its offline backup and recovery information outside Git, and upload the public certificate when requested.
3. Complete Main store listing from `distribution/listing-en.md`, app category, contact details, privacy URL, countries/regions, pricing, content rating, target audience, ads declaration, news declaration if shown, and every App content task.
4. Complete [Data safety](https://support.google.com/googleplay/android-developer/answer/10787469) using `distribution/privacy-data.md`, checking the shipped binary and every provider flow rather than copying claims blindly.
5. Upload an initial signed AAB manually if Console requires it to initialize the application before Publishing API access. For a new post-2023 personal account, use the required closed track and complete the 12-tester/14-day gate first.

### 3. Configure Android Publisher API with WIF

1. Enable the [Google Play Android Developer API](https://console.cloud.google.com/apis/library/androidpublisher.googleapis.com) in a dedicated Google Cloud project and review the official [Google Play Developer API setup](https://developers.google.com/android-publisher/getting_started).
2. Create a dedicated service account with no broad Google Cloud roles. In Play Console, invite its email under Users and permissions and grant only the Kvace app permissions necessary to manage store presence and release to Production.
3. Configure a Workload Identity Pool/provider restricted to the exact GitHub organization/repository and, where policy allows, the protected `google-play-production` environment. Follow Google's [GitHub Actions authentication guidance](https://github.com/google-github-actions/auth#workload-identity-federation) and [WIF deployment-pipeline guidance](https://cloud.google.com/iam/docs/workload-identity-federation-with-deployment-pipelines).
4. Permit the GitHub principal to impersonate only the dedicated service account. Do not create or store a service-account JSON key.
5. Create the protected GitHub environment `google-play-production`, require reviewers, disable administrator bypass where policy permits, and add:

   - `ANDROID_KEYSTORE_BASE64`
   - `ANDROID_KEYSTORE_PASSWORD`
   - `ANDROID_KEY_ALIAS`
   - `ANDROID_KEY_PASSWORD`
   - `GOOGLE_WORKLOAD_IDENTITY_PROVIDER`
   - `GOOGLE_PLAY_SERVICE_ACCOUNT`

The workflow exchanges GitHub's OIDC token for a short-lived Google access token, then uses the official Android Publisher edits API. An approval commits listing, screenshots, AAB version code 1, and a `completed` release directly to Production at 100%; there is no staged rollout.

### 4. Bootstrap verification

- Confirm package name, version `1.0.0`, version code `1`, upload certificate, Play App Signing certificate, and Production access.
- Confirm the service account can access only Kvace and has no unnecessary financial, account-admin, or other-app permissions.
- Confirm Data safety/privacy/listing content matches the actual 1.0.0 binary.
- If the mandatory personal-account fork applies, attach evidence of 12 continuously opted-in testers for the preceding 14 days and the Production-access approval to the private release issue.

## Apple App Store

### 1. Establish the developer account

1. Enroll the legal entity in the [Apple Developer Program](https://developer.apple.com/programs/enroll/), complete identity/organization verification, and ensure membership will remain active through review.
2. In App Store Connect, have the Account Holder accept current agreements and complete tax and banking tasks under Agreements, Tax, and Banking.
3. Record the Team ID and restrict operational access to the minimum maintainers.

### 2. Register identifiers and the app

1. Register the explicit App ID `com.softartdev.kvace` under [Certificates, Identifiers & Profiles](https://developer.apple.com/account/resources/identifiers/list). Enable only capabilities used by the shipped app.
2. [Add the App Store Connect app record](https://developer.apple.com/help/app-store-connect/create-an-app-record/add-a-new-app) named `Kvace`, platform iOS, bundle ID `com.softartdev.kvace`, SKU chosen by the maintainer, and primary language English (U.S.). The API cannot create this initial app record.
3. Complete primary/secondary categories, age rating, App Privacy from `distribution/privacy-data.md`, pricing, availability, export-compliance answers, content rights, privacy/support/marketing URLs, and every unresolved App Store Connect prompt.

### 3. Create signing and API credentials

1. Create an Apple Distribution certificate using a protected private key. Export the certificate and private key as a password-protected PKCS#12 file; store the original and password outside Git.
2. Create an App Store distribution provisioning profile for `com.softartdev.kvace`, tied to the distribution certificate. Record its exact profile name; it becomes `IOS_PROVISIONING_PROFILE_NAME`.
3. Request App Store Connect API access if it is not enabled, then create the minimum-role team key under Users and Access > Integrations. Apple's [App Store Connect API setup](https://developer.apple.com/help/app-store-connect/get-started/app-store-connect-api/) explains the Account Holder/Admin requirements. Download the `.p8` once and store it outside Git.
4. Ensure the API role can update app metadata/screenshots and submit versions for review. Upload authority is also used by Apple's CLI. Apple documents build upload options in [Upload builds](https://developer.apple.com/help/app-store-connect/manage-builds/upload-builds).
5. Create the protected GitHub environment `app-store-production`, require reviewers, disable administrator bypass where policy permits, and add:

   - `IOS_CERTIFICATE_BASE64`
   - `IOS_CERTIFICATE_PASSWORD`
   - `IOS_PROVISIONING_PROFILE_BASE64`
   - `IOS_PROVISIONING_PROFILE_NAME`
   - `APPLE_TEAM_ID`
   - `APP_STORE_CONNECT_KEY_ID`
   - `APP_STORE_CONNECT_ISSUER_ID`
   - `APP_STORE_CONNECT_PRIVATE_KEY`
   - `APP_REVIEW_CONTACT_FIRST_NAME`
   - `APP_REVIEW_CONTACT_LAST_NAME`
   - `APP_REVIEW_CONTACT_PHONE`
   - `APP_REVIEW_CONTACT_EMAIL`

The workflow exports an IPA with Xcode, uploads through Apple's CLI, manages metadata/screenshots through the official App Store Connect API, associates processed build 1 with version 1.0.0, and submits directly to App Review with automatic release after approval. It does not create a TestFlight group or test.

### 4. Bootstrap verification

- Confirm bundle ID, SKU, Team ID, version `1.0.0`, build `1`, certificate expiry, and provisioning-profile name all match.
- Confirm agreements, tax/banking, App Privacy, age rating, export compliance, pricing, availability, and review contact are complete.
- Perform a signed physical-device smoke test and inspect the archive's signing identity and entitlements before approving production.
- Verify the API key is recoverably stored, minimally privileged, and absent from repository history and workflow logs.

## Shared release controls

Create `github-release-production` and `github-pages` environments with required reviewers. Follow [Release Guide](RELEASE_GUIDE.md) to generate the gitignored screenshots, package the deterministic store archive, push the signed tag, create the matching draft GitHub Release, and upload assets without replacement. Both mobile workflows fail closed unless that exact draft contains a matching archive and checksum.
