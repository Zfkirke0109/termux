# Android 17 / One UI 9 review

## Initial device evidence (2026-10-02)

- Samsung SM-S918U1, arm64-v8a, Android 17 / API 37, build CP2A.260605.016.S918U1UEU8ZZI8. The supplied screenshot shows One UI 9.0.
- `getconf PAGESIZE`: 4096. This handset currently uses 4 KB pages; 16 KB support is not demonstrated by this build.
- Installed baseline: `com.termux` 0.118.0+4584488, version code 118, target SDK 28. This differs from the requested beta.3 base.
- Current process RSS approximately 99 MB, PSS approximately 53 MB; a single sample does not establish a leak.
- The 16 inspected exit records show package update, low-memory and background/empty-process kills (including Samsung Chimera), not a Java/native crash. No `MemoryLimiter` exit description appeared; `am memory-limiter status` reports disabled.
- Background app-op is allowed and Termux is already battery-optimization allowlisted. No settings or global memory/process safeguards were changed for this task.
- Installed signature's PackageManager hash is consistent with the upstream test certificate, but a 32-bit hash is not cryptographic certificate verification. Android's package installer must still verify an in-place update.

## Findings and fixes

| Severity | Finding | Resolution |
|---|---|---|
| Required | DEL/CAN/SUB can put invisible DEL in copied text | Exact PR #5357 backport and its regression test |
| Required | JNI releases `cmd_cwd` with `cmd` rather than matching `cwd` reference | Correct reference; compile in CI. No claim that this caused the observed low-memory exits |
| Required | New output pulls a reader out of older scrollback | Preserve reading position, clamping at evicted history; explicit jump-latest action |
| Required | New search could scan/copy an unbounded transcript | Limit rows, characters, query length and results; search once per explicit submit |
| Required | Live output/session changes can invalidate result row numbers | Recheck session, emulator, history bounds and row text before jumping |
| Required | SDK setup requests retired `tools` package | Request `platform-tools` explicitly |
| Required | Publication rejects preserved upstream whitespace | Whitespace checks apply to our seed changes; upstream import is left unchanged |
| FYI | GitHub shared test key provides compatibility, not source authenticity | Explicit unofficial labeling, pinned sources and checksum/certificate verification |
| FYI | Upstream targets SDK 28 intentionally | Retained. Raising to API 37 is not a compatibility fix and changes executable-app-data, storage, service and permission behavior |

## Five-axis review

- Correctness: PR behavior preserved; copy uses inclusive row bounds; result navigation checks stale output. Search is literal, not executable code or a user-supplied regex.
- Readability: mobile actions and bounded search live in small app-owned classes, not expanded general-purpose clipboard code.
- Architecture: UI actions reuse `ShareUtils`, `TerminalBuffer` selection and `TerminalView`; no new dependencies or shell-command injection.
- Security: clipboard access only on explicit user action; no passwords, terminal output or device logs are committed; public upstream test keystore is intentionally retained.
- Performance: bounded search (2,000 visual rows / 250,000 characters / 100 results), no per-keystroke scan. Actual UI latency under worst-case combining-character output remains unmeasured.

## Verification and limits

- Baseline beta.3 + PR #5357: 146 emulator tests passed; all debug APKs compiled and their shared-key certificate verification passed in run [37001425462](https://github.com/Zfkirke0109/termux/actions/runs/37001425462). That run's final publication step failed only on upstream whitespace.
- Five new pure-Java search tests passed locally: literal case-insensitive search/order, empty/multiline rejection, row limits, character/result bounds, Unicode/null rows.
- Final mobile edition: [CI run 37002802377](https://github.com/Zfkirke0109/termux/actions/runs/37002802377) passed. All 153 tests (146 emulator + 7 app, including 5 new search tests) passed with no failures/errors. Debug APK build, certificate checks, artifact upload and source/APK publication succeeded.
- Source and APKs published at commit `f3c8e845aa05e5233cbefdc7657b7261a9a854fe`; 341 source files are present, without generated build trees. The published custom arm64 APK was downloaded independently and verified with `apksig`: verified, no errors, expected shared-key certificate.
- Custom arm64 APK SHA-256: `bdc590d46e150796e31cd39c9178ddfe72b5373383bd4ada7a81113dbcbc0352`. Its version name contains `pr5357.mobile.251f35d`.
- The first custom mobile build was subsequently installed by the user and exercised on the physical handset: version `0.119.0-beta.3+pr5357.mobile.251f35d`, version code 1022, target 28. Shell/cwd, Ctrl-C, literal search, clipboard including DEL/Unicode/indentation/wraps, stale-result handling, stable scrollback during live output, jump-latest, rotation/Samsung Keyboard and Termux:API clipboard checks passed.
- App-scoped logcat and exit history showed no reproduced Java/native crash during that test window. Vendor graphics warnings and a missing optional settings value were observed. Short-lived memory/dialog peaks and cumulative jank do not establish a leak or battery improvement. No global process/memory/battery settings were changed.
- The new mobile2 features added after that device pass require a separate update and the [mobile2 checklist](MOBILE2_TEST_CHECKLIST.md). Split-screen/DeX, 16 KB and Android 18 certification remain unproven. Updating Termux restarts existing sessions; save active work before updating.

## Device smoke test after an approved in-place update

1. Confirm the version name contains `pr5357.mobile` and that old home/usr files remain present.
2. Start a terminal with a working directory different from its shell binary; confirm prompt, echo, Ctrl-C and subprocess exit.
3. Print `ab\177c` and confirm copied text is `abc` with no DEL.
4. Print numbered output, scroll up during continued output, verify the reading position stays stable, then use Jump to latest.
5. Search existing text; select a hit. Repeat while output changes and check the stale-result guard.
6. Copy visible output containing indentation, Unicode and wrapped lines into a scratch text field; do not execute copied content.
7. Rotate, open/close Samsung Keyboard, switch sessions and use split-screen. Capture only app-specific crash/exit information if a failure occurs.

## Platform sources

- [Android 17 all-app behavior changes](https://developer.android.com/about/versions/17/behavior-changes-all): memory limits, same-profile versus cross-profile loopback, keyboard visibility after unhandled configuration changes.
- [Android 17 target-specific changes](https://developer.android.com/about/versions/17/behavior-changes-17): distinguish runtime version from target API.
- [16 KB page-size guidance](https://developer.android.com/guide/practices/page-sizes): device and native-library verification is required.
- [Android 10 executable app-data restriction](https://developer.android.com/about/versions/10/behavior-changes-10#execute-permission): applies to apps targeting API 29+.

No root, SELinux bypass, uninstall, global memory-limiter change, or automatic package upgrade is part of this review.
