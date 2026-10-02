# Mobile2 device smoke checklist

Use a compatible in-place update after saving live work. Keep the existing GitHub test signing certificate and `com.termux` package identity. Do not uninstall to work around a certificate mismatch.

1. **Upgrade:** verify the installed version contains `pr5357.mobile2`, HOME/PREFIX remain present, and Termux:API still works. Open a fresh shell, execute a subprocess from a different directory, and interrupt it with Ctrl-C.
2. **Extra keys:** open Samsung Keyboard in portrait and landscape. Check SHIFT/PgUp/PgDn at normal and large font scales. Verify modifiers, repeat and popup keys still send the intended key/macro.
3. **Search:** print a phrase split by a soft wrap and the same fragments separated by a hard newline. Search with the keyboard Search action. Only the soft-wrapped phrase should match. Check Previous/Next, Unicode highlights and close-to-view behavior. Append/edit/reflow output before clicking a result and expect the stale-result message.
4. **Scroll/copy:** scroll back during live output; copy the visible region with indentation, wide/combining text and wraps. Jump to latest and verify it returns to the live screen.
5. **Service panel:** check sshd/layla-relay current state and PID against `sv status`. Use a disposable service to test Start, Stop/cancel, and separate startup enable/disable. Verify that opening/refreshing the panel never changes running services, and the startup toggle leaves current state alone. Restore its original startup marker.
6. **Recovery:** confirm Android update/low-memory/crash classifications match available exit records. Save visible text, view it, overwrite it, then delete it. It should never execute saved text. Attach to a disposable surviving tmux session by selecting it; after the server is killed/rebooted, reconnect should report no surviving session.
7. **Logging:** start Debug for 15 minutes, confirm key logging is off, wait for Normal or select Normal now. Test a process restart during the interval. Set a manual log level in Settings and verify the timer no longer overrides it.
8. **Platform matrix:** cover split screen/DeX, TalkBack, background/locked screen, receiving shared files, and both page sizes in the modernization lane. Record device/build and exact flows; avoid blanket Android 18 certification.

Collect app-scoped crash/exit evidence and service state if a step fails. Avoid exporting terminal contents or global device logs automatically. The current release CI verifies build/tests/signatures; it cannot replace these device checks.
