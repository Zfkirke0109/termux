# Android 17 / One UI 9 and Android 18 modernization

Reviewed 2026-10-02. This is an unofficial Termux beta.3 fork. Running on Android 17 is different from targeting API 37. The five mobile improvements are implemented; the platform projects below remain work to do. Android 18 compatibility is not certified.

## Implemented in mobile2

| Improvement | Behavior | Limits |
|---|---|---|
| Extra-key labels | One line, adaptive size, full accessibility label | Very long custom labels and large font scales still need device coverage |
| Search | Keyboard focus, IME Search, soft-wrap matching, highlights, Previous/Next | 2,000 rows, 250,000 characters, 256-character query, 100 occurrences; stale results are rejected |
| Services | Existing runit services, state/PID, Start/Stop, separate startup marker | No automatic supervisor start; first 64 valid service names; private Termux paths only |
| Recovery | Recent Android exits, sampled app memory, private visible-text checkpoint, surviving tmux reconnect | Android 11+ for exit history; one 25,000-character checkpoint; default tmux server only; no process resurrection |
| Logging | Opt-in Debug for 15 minutes, Normal now, key logging off during timed debug | Manual settings cancel the timer; no automatic device log export |

Opening a panel does not change service state. Stopping a service and deleting a checkpoint require a specific confirmation in the app. Reconnect passes a validated tmux session ID as an argument to a new terminal; historical shell text is never executed.

## Highest-priority platform projects

| Priority | Project | Concrete next implementation | Acceptance gate |
|---|---|---|---|
| P0 | Modern SDK and executable architecture | Raise **compile SDK** in an isolated build-tool migration first. Design how apt-installed binaries, writable HOME/PREFIX execution and plugins work before raising **target SDK**. Keep the existing package, data and signing identity for compatible updates. | Fresh install plus in-place upgrade; bash, Python, sshd, tmux, proot and every installed plugin work without data loss |
| P0 | 16 KB native support | Audit both JNI libraries and all APK native dependencies; move to a suitable AGP/NDK combination or add verified linker alignment. Package/bootstrap binaries need their own audit. | ELF segment and APK alignment checks, plus 16 KB emulator/device tests of PTYs, sockets and package binaries; 4 KB regression pass |
| P0 | Memory and lifecycle | Establish controlled PSS/RSS and renderer baselines, bound diagnostics/checkpoint retention, test process/session teardown and limit scrollback allocations. Add a scoped profiling adapter after the SDK migration. | Long output, many sessions, search, selection and repeated dialogs reach a stable plateau; force-kill and memory-pressure cases recover honestly |
| P1 | Samsung Keyboard, DeX and large screens | Replace legacy layout guesses with an insets-aware keyboard/layout controller. Retain IME intent across recreation; support split screen, freeform/DeX, hardware keyboard, font scaling and fold transitions. | Prompt/cursor visible through resize and rotation, no clipped controls, preserved session/viewport and predictable keyboard state |
| P1 | Network permissions and relay health | Add an API-aware capability screen for LAN, loopback/profile boundaries and background availability. Show bind address/port, health and bounded recent logs for sshd/relay in a later service-panel increment. | LAN allow/deny flows, same-profile loopback and work-profile failure messages tested; permission denial does not produce silent hangs |
| P1 | Foreground work and plugins | Review foreground-service starts/types, notification controls, boot behavior and background activity launches together with Termux:API/Boot/Tasker. Preserve user-managed runit jobs rather than silently restarting them. | Background/locked-screen/Doze tests; explicit stopped-state behavior; API, Boot and Tasker integration checks |
| P1 | File sharing and storage | Verify explicit URI grants through direct launch and chooser flows; add scoped-storage/SAF adapters for app-owned exports/imports. Keep arbitrary terminal paths inside the shell environment rather than forcing them through UI document APIs. | Receiving app can open shared files with the precise grant, and loses access appropriately; no broad provider exposure |
| P2 | Input and accessibility | Expand tests for CJK composition, combining characters, emoji, dead keys, paste size, TalkBack, mouse/touchpad and keyboard shortcuts. Consider configurable larger or scrollable extra keys. | No lost input, duplicate composition or incorrect search/copy positions; usable at large font sizes |
| P2 | Maintenance and diagnostics | Rebase selected fixes onto an actively maintained upstream version after a reviewed diff. Modernize dependencies incrementally. Add redacted, scoped issue export with retention controls and opt-in secure-screen behavior. | No terminal content in routine logs; no unrelated dependency churn; reproducible build and signature/provenance checks |

These are engineering recommendations for this fork, not claims of measured improvements or unavoidable failures on the current handset.

## Platform facts informing the work

- [Android 17 all-app changes](https://developer.android.com/about/versions/17/behavior-changes-all) document memory limits, changed keyboard visibility after unhandled recreation, and cross-profile loopback restrictions. Same-profile loopback remains supported. The recovery classifier handles the documented `OTHER` + `MemoryLimiter` description and the newer numeric reason without requiring new APIs at runtime.
- [Target-37 changes](https://developer.android.com/about/versions/17/behavior-changes-17) require LAN permission handling, restrict writable native libraries loaded through `System.load`, and change MessageQueue internals. These are **target-dependent**; merely increasing a version number or adding a manifest permission is not a migration.
- [Android 10 executable restrictions](https://developer.android.com/about/versions/10/behavior-changes-10#execute-permission) and [Termux's architecture explanation](https://github.com/termux/termux-packages/wiki/Termux-and-Android-10) explain why this build retains target 28. Treat a higher target as an architecture project with a plugin/data migration plan.
- [16 KB guidance](https://developer.android.com/guide/practices/page-sizes) covers native alignment, packaging, dependencies and runtime assumptions. AGP 8.5.1+ / NDK r28+ are documented defaults, not proof that Termux's downloaded packages are compatible. This phone was measured at 4 KB pages.
- [ApplicationExitInfo](https://developer.android.com/reference/android/app/ApplicationExitInfo) reports historical process exits and sampled memory. A retained ANR trace does not by itself mean an ANR caused a later death. Do not mislabel an app update or vendor background kill as a Java crash.

## Android 18: announced change and future gates

The Android 17 [all-app documentation, URI-grant section](https://developer.android.com/about/versions/17/behavior-changes-all#restrict-implicit-uri-grants) already announces removal of implicit sharing/capture URI grants in Android 18. The inspected `TermuxOpenReceiver` sets `FLAG_GRANT_READ_URI_PERMISSION` for file sharing; `ShareUtils.shareText` sends text without a file URI. Still test grant propagation through the chooser and receiving app, and audit plugins separately. Do not claim readiness based on source inspection alone.

A complete Android 18 behavior/API baseline was not available in the official pages checked for this pass. When official previews/SDKs are available:

1. Add the published SDK to CI while retaining the previous supported build/runtime lane.
2. Review all-app and target-specific changes independently; avoid inventing requirements from API-number guesses.
3. Run an Android 18 emulator matrix and the same Samsung upgrade, keyboard, clipboard, service, plugin, URI-sharing and process-death flows.
4. Include both 4 KB and 16 KB page-size environments where available.
5. Label compatibility by tested build/device/flow, and publish reproducible source, checksums and signing evidence before distribution.

## Verification record

- 19 pure-Java regression tests passed locally for search, runit parsing, recovery parsing and debug deadline policy.
- Full Android build, Robolectric UI tests and certificate checks are required in the root Actions workflow before an APK is published.
- The previous mobile build was physically exercised on the S23 Ultra / Android 17 handset. The newly added mobile2 screens require a separate in-place update and device smoke test; updating Termux ends existing app processes/sessions.

See [the device review](ANDROID17_REVIEW.md) for prior physical evidence and [the mobile2 smoke checklist](MOBILE2_TEST_CHECKLIST.md) for the new flows.
