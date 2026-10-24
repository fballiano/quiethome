# Security

## Report a problem

To report a security problem, use [private vulnerability reporting](https://github.com/fballiano/quiethome/security/advisories/new). Do not open a public issue.

Security fixes go into the next release. Only the latest release gets fixes.

## What the app can do

QuietHome asks for few rights. Each right has one job. The Google Play version does not have `INTERNET` and `REQUEST_INSTALL_PACKAGES`, because Google Play updates it. So that version never uses the network.

| Right | Job |
| --- | --- |
| `INTERNET` | Only in the GitHub version. Only for Settings → Update QuietHome, and only when you select it. QuietHome reads the latest release from the GitHub API (`api.github.com`) and downloads `quiethome.apk` from that release. It sends nothing about you or your TV. It has no analytics and no ads. |
| `REQUEST_INSTALL_PACKAGES` | Only in the GitHub version. Gives the downloaded update to Android's installer. Before that, QuietHome checks the SHA-256 digest of the file against the digest that GitHub publishes, and checks that the signature is the same as the installed app. Android asks you to confirm each update, and installs it only with the same signature. |
| `KILL_BACKGROUND_PROCESSES` | Hold OK on an app to close it. |
| `WRITE_SECURE_SETTINGS` | Only when you grant it with ADB. QuietHome then uses it to turn its own service on again, for example after a force stop. It changes no other setting. |
| Accessibility service | It receives key presses and one event type, `typeWindowStateChanged`, which gives the package and activity name of the app in front. It cannot read the screen (`canRetrieveWindowContent="false"`). It acts on Home, on the buttons that you map, and on the stock launcher's home screen, which it replaces with QuietHome. It acts on nothing else. |
| Configuration receiver | It needs the `DUMP` permission. Only ADB and system apps have that permission, so other apps cannot read or change the configuration. |

QuietHome keeps its settings in its private app storage. Backups are off (`allowBackup="false"`).

## How a release is made

- **No dependencies.** The app uses only the Android SDK. The build uses only `build.sh` and the SDK tools. There is no Gradle, and no library code goes into the APK.
- **Built by GitHub.** A version tag (for example `v0.2.0`) starts `.github/workflows/build.yml`. A person does not build or upload the APK.
- **Signing key.** The release key is kept in the repository secrets `KEYSTORE_BASE64` and `KEYSTORE_PASS`. Only the build of a tag reads them. Pull requests and branch builds use a test key.
- **Google Play.** The same workflow run builds the App Bundle for Google Play (`STORE=play`). A person downloads it from the run and uploads it in the Play Console.
- **Build provenance.** Each release APK gets a signed attestation. The attestation records the repository, the commit and the workflow that built the APK.
- **Immutable releases.** After a release is published, nobody can change or replace its APK or move its tag.

## How to check a release

Make sure GitHub built the APK from this repository:

```sh
gh attestation verify quiethome.apk --repo fballiano/quiethome
```

Make sure the APK has the release signature:

```sh
apksigner verify --print-certs quiethome.apk
```

The SHA-256 digest of the signer must be `983875f88bf502ef5f006f238a6fc58bade0abae0313bd6affc3684183516830`.

## How the repository is protected

| Protection | What it does |
| --- | --- |
| Pinned actions | Each GitHub action is pinned to a full commit SHA, so a changed tag cannot change the build. |
| Least privilege | The workflow token has no rights by default. The build job can only read. Only the release job can write, and it runs only for tags. |
| Dependabot | Each week, it proposes updates for the pinned actions. It also sends security alerts and fixes. |
| CodeQL | It checks the Java code, the Python tool and the workflows on each change. |
| Secret scanning | It looks for secrets in the code. Push protection stops a push that contains a secret. |
| Ruleset for `main` | Nobody can force-push to `main` or delete it. |
| Ruleset for `v*` tags | Nobody can move or delete a release tag. |
| Default token permission | New workflows get a read-only token, and they cannot approve pull requests. |
