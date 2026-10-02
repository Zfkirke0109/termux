package com.termux.app.terminal;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import com.termux.terminal.WcWidth;

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
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(256)});
        input.setHint(R.string.mobile_search_hint);
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(R.string.mobile_search_title).setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.mobile_search_button, null).create();
        Runnable submit = () -> {
            if (view.getCurrentSession() != session || session.getEmulator() != emulator) {
                dialog.dismiss();
                return;
            }
            String query = input.getText().toString();
            if (query.isEmpty()) { input.setError(activity.getString(R.string.mobile_search_hint)); return; }
            MobileTranscriptSearch.Rows rows = rows(emulator);
            List<MobileTranscriptSearch.Match> matches = MobileTranscriptSearch.find(rows,
                -emulator.getScreen().getActiveTranscriptRows(), emulator.mRows - 1, query);
            InputMethodManager keyboard = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (keyboard != null) keyboard.hideSoftInputFromWindow(input.getWindowToken(), 0);
            dialog.dismiss();
            if (matches.isEmpty()) {
                Toast.makeText(activity, R.string.mobile_search_no_matches, Toast.LENGTH_LONG).show();
            } else {
                showMatches(activity, view, session, emulator, rows, matches);
            }
        };
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> submit.run());
            input.requestFocus();
            if (dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            input.post(() -> {
                InputMethodManager keyboard = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (keyboard != null) keyboard.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            });
        });
        input.setOnEditorActionListener((v, action, event) -> {
            if (action != EditorInfo.IME_ACTION_SEARCH) return false;
            submit.run();
            return true;
        });
        dialog.show();
    }

    private static MobileTranscriptSearch.Rows rows(TerminalEmulator emulator) {
        return new MobileTranscriptSearch.Rows() {
            public String textAt(int row) {
                return emulator.getScreen().getSelectedText(0, row, emulator.mColumns - 1, row, false);
            }
            public boolean wrapsAt(int row) { return emulator.getScreen().getLineWrap(row); }
        };
    }

    private static void showMatches(Activity activity, TerminalView view, TerminalSession session,
                                    TerminalEmulator emulator, MobileTranscriptSearch.Rows rows,
                                    List<MobileTranscriptSearch.Match> matches) {
        int[] selected = {0};
        String[] labels = new String[matches.size()];
        for (int i = 0; i < labels.length; i++) {
            MobileTranscriptSearch.Match match = matches.get(i);
            labels[i] = activity.getString(R.string.mobile_search_row, match.row) + " " + match.text;
        }
        AlertDialog results = new AlertDialog.Builder(activity).setTitle(R.string.mobile_search_results)
            .setSingleChoiceItems(labels, 0, (dialog, index) -> selected[0] = index)
            .setNegativeButton(R.string.mobile_search_close, null)
            .setNeutralButton(R.string.mobile_search_previous, null)
            .setPositiveButton(R.string.mobile_search_next, null).create();
        Runnable jump = () -> {
            MobileTranscriptSearch.Match match = matches.get(selected[0]);
            if (view.getCurrentSession() != session || session.getEmulator() != emulator
                || !match.isCurrent(rows, -emulator.getScreen().getActiveTranscriptRows(), emulator.mRows - 1)) {
                view.clearSearchHighlight();
                Toast.makeText(activity, R.string.mobile_search_changed, Toast.LENGTH_LONG).show();
                results.dismiss();
                return;
            }
            view.stopTextSelectionMode();
            view.setTopRow(Math.min(0, match.row));
            view.onScreenUpdated(true);
            view.setSearchHighlight(match.row, match.endRow,
                columnForIndex(rows.textAt(match.row), match.startIndex, false),
                columnForIndex(rows.textAt(match.endRow), match.endIndex, true));
            results.setTitle(activity.getString(R.string.mobile_search_position, selected[0] + 1, matches.size()));
        };
        results.setOnShowListener(ignored -> {
            // Keep the terminal and its highlight visible behind the navigation panel.
            if (results.getWindow() != null) {
                results.getWindow().setDimAmount(0.15f);
                results.getWindow().setGravity(android.view.Gravity.BOTTOM);
                results.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    Math.round(activity.getResources().getDisplayMetrics().heightPixels * 0.42f));
            }
            results.getListView().setOnItemClickListener((parent, item, position, id) -> {
                selected[0] = position;
                jump.run();
            });
            results.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                selected[0] = (selected[0] + 1) % matches.size();
                results.getListView().setItemChecked(selected[0], true);
                results.getListView().smoothScrollToPosition(selected[0]);
                jump.run();
            });
            results.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                selected[0] = (selected[0] + matches.size() - 1) % matches.size();
                results.getListView().setItemChecked(selected[0], true);
                results.getListView().smoothScrollToPosition(selected[0]);
                jump.run();
            });
            jump.run();
        });
        results.show();
    }

    static int columnForIndex(String text, int index, boolean endExclusive) {
        int column = 0;
        for (int i = 0; i < index && i < text.length();) {
            int codePoint = text.codePointAt(i);
            column += Math.max(0, WcWidth.width(codePoint));
            i += Character.charCount(codePoint);
        }
        if (endExclusive || (index < text.length() && WcWidth.width(text.codePointAt(index)) == 0)) column--;
        return Math.max(0, column);
    }
}
