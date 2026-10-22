# Security

To report a security problem, use [private vulnerability reporting](https://github.com/fballiano/quiethome/security/advisories/new). Please do not open a public issue.

Security fixes go into the latest release.

## Checking a release

Each release APK has a build provenance attestation. It shows that GitHub Actions built the APK from this repository:

```sh
gh attestation verify quiethome.apk --repo fballiano/quiethome
```

Releases are immutable: a published APK cannot be changed or replaced.
