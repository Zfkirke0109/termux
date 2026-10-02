package com.termux.app.terminal;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import com.termux.R;
import com.termux.shared.interact.ShareUtils;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

import java.util.List;

/** Touch-friendly actions. Clipboard access occurs only after an explicit user action. */
public final class MobileTerminalActions {
    private MobileTerminalActions() {}

    public static void copyVisible(Activity activity, TerminalView view) {
        TerminalSession session = view.getCurrentSession();
        TerminalEmulator emulator = session == null ? null : session.getEmulator();
        if (emulator == null) return;
        int top = Math.max(-emulator.getScreen().getActiveTranscriptRows(), Math.min(0, view.getTopRow()));
        String text = emulator.getScreen().getSelectedText(0, top, emulator.mColumns - 1,
            Math.min(emulator.mRows - 1, top + emulator.mRows - 1), true);
        ShareUtils.copyTextToClipboard(activity, text, activity.getString(R.string.mobile_screen_copied));
    }

    public static void jumpToLatest(TerminalView view) {
        if (view.getCurrentSession() == null) return;
        view.stopTextSelectionMode();
        view.setTopRow(0);
        view.onScreenUpdated(true);
    }

    public static void search(Activity activity, TerminalView view) {
        TerminalSession session = view.getCurrentSession();
        TerminalEmulator emulator = session == null ? null : session.getEmulator();
        if (emulator == null) return;
        EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(256)});
        input.setHint(R.string.mobile_search_hint);
        new AlertDialog.Builder(activity).setTitle(R.string.mobile_search_title).setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.mobile_search_button, (dialog, which) -> {
                if (view.getCurrentSession() != session || session.getEmulator() != emulator) return;
                List<MobileTranscriptSearch.Match> matches = MobileTranscriptSearch.find(
                    row -> emulator.getScreen().getSelectedText(0, row, emulator.mColumns - 1, row, false),
                    -emulator.getScreen().getActiveTranscriptRows(), emulator.mRows - 1, input.getText().toString());
                if (matches.isEmpty()) {
                    Toast.makeText(activity, R.string.mobile_search_no_matches, Toast.LENGTH_LONG).show();
                    return;
                }
                String[] labels = new String[matches.size()];
                for (int i = 0; i < labels.length; i++) {
                    MobileTranscriptSearch.Match match = matches.get(i);
                    labels[i] = activity.getString(R.string.mobile_search_row, match.row) + " "
                        + match.text.substring(0, Math.min(160, match.text.length()));
                }
                new AlertDialog.Builder(activity).setTitle(R.string.mobile_search_results)
                    .setItems(labels, (results, index) -> {
                        MobileTranscriptSearch.Match match = matches.get(index);
                        if (view.getCurrentSession() != session || session.getEmulator() != emulator
                            || match.row < -emulator.getScreen().getActiveTranscriptRows()
                            || !match.text.equals(emulator.getScreen().getSelectedText(0, match.row,
                                emulator.mColumns - 1, match.row, false))) {
                            Toast.makeText(activity, R.string.mobile_search_changed, Toast.LENGTH_LONG).show();
                            return;
                        }
                        view.stopTextSelectionMode();
                        view.setTopRow(Math.min(0, match.row));
                        view.onScreenUpdated(true);
                    }).setNegativeButton(android.R.string.cancel, null).show();
            }).show();
    }
}
