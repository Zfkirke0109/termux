package com.termux.app.activities;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.util.AtomicFile;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.RequiresApi;
import com.termux.R;
import com.termux.app.terminal.MobileCommandRunner;
import com.termux.app.terminal.MobileRecovery;
import com.termux.shared.termux.TermuxConstants;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/** Device-local, opt-in recovery assistance; a checkpoint restores text, not processes. */
public class MobileRecoveryActivity extends AppCompatActivity {
    public static final String EXTRA_VISIBLE_OUTPUT = "visible_output";
    public static final String EXTRA_TMUX_ID = "tmux_id";
    private LinearLayout content;
    private TextView history, checkpointStatus;
    private boolean busy;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle(R.string.mobile_recovery_title);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(16 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding, padding, padding);
        scroll.addView(content);
        setContentView(scroll);
        text(getString(R.string.mobile_recovery_explanation));
        history = text(getString(R.string.mobile_services_loading));
        button(R.string.mobile_refresh, this::loadHistory);
        checkpointStatus = text(getString(R.string.mobile_checkpoint_explanation));
        button(R.string.mobile_checkpoint_save, this::saveCheckpoint);
        button(R.string.mobile_checkpoint_view, this::viewCheckpoint);
        button(R.string.mobile_checkpoint_delete, () -> new AlertDialog.Builder(this)
            .setMessage(R.string.mobile_checkpoint_delete_confirm).setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.mobile_checkpoint_delete, (dialog, which) -> {
                checkpointFile().delete();
                checkpointStatus.setText(R.string.mobile_checkpoint_deleted);
            }).show());
        button(R.string.mobile_tmux_reconnect, this::listTmux);
        loadHistory();
    }
    @Override public boolean onSupportNavigateUp() { finish(); return true; }
    private TextView text(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextIsSelectable(true);
        text.setPadding(0, 12, 0, 12);
        content.addView(text);
        return text;
    }
    private void button(int label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(v -> { if (!busy) action.run(); });
        content.addView(button);
    }
    private void loadHistory() {
        busy = true;
        MobileCommandRunner.BACKGROUND.execute(() -> {
            String result;
            try {
                Debug.MemoryInfo memory = new Debug.MemoryInfo();
                Debug.getMemoryInfo(memory);
                result = getString(R.string.mobile_memory_sample, memory.getTotalPss()) + "\n\n"
                    + (Build.VERSION.SDK_INT >= 30 ? exitHistory() : getString(R.string.mobile_exit_history_unsupported));
            } catch (RuntimeException error) { result = getString(R.string.mobile_operation_failed, error.getMessage()); }
            String value = result;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                busy = false;
                history.setText(value);
            });
        });
    }
    @RequiresApi(30) private String exitHistory() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (manager == null) return getString(R.string.mobile_exit_history_empty);
        List<ApplicationExitInfo> exits = manager.getHistoricalProcessExitReasons(getPackageName(), 0, 8);
        StringBuilder report = new StringBuilder(getString(R.string.mobile_exit_history_header));
        int[] reasons = {R.string.mobile_exit_unknown, R.string.mobile_exit_self, R.string.mobile_exit_signal,
            R.string.mobile_exit_memory, R.string.mobile_exit_crash, R.string.mobile_exit_native_crash,
            R.string.mobile_exit_anr, R.string.mobile_exit_initialization, R.string.mobile_exit_permission,
            R.string.mobile_exit_resource, R.string.mobile_exit_user, R.string.mobile_exit_user_stopped,
            R.string.mobile_exit_dependency, R.string.mobile_exit_other, R.string.mobile_exit_freezer,
            R.string.mobile_exit_package_state, R.string.mobile_exit_update, R.string.mobile_exit_memory_limiter};
        if (exits.isEmpty()) return getString(R.string.mobile_exit_history_empty);
        for (ApplicationExitInfo exit : exits) {
            String description = exit.getDescription();
            report.append("\n\n").append(DateFormat.getDateTimeInstance().format(new Date(exit.getTimestamp())))
                .append("\n").append(getString(reasons[MobileRecovery.classifyExit(exit.getReason(), description)]))
                .append("\n").append(getString(R.string.mobile_exit_sample, exit.getPid(), exit.getStatus(), exit.getPss(), exit.getRss(), exit.getReason()));
            if (description != null) report.append("\n").append(description.substring(0, Math.min(1024, description.length())));
        }
        return report.toString();
    }
    private AtomicFile checkpointFile() { return new AtomicFile(new File(getFilesDir(), "mobile-visible-checkpoint.txt")); }
    private void saveCheckpoint() {
        String visible = getIntent().getStringExtra(EXTRA_VISIBLE_OUTPUT);
        if (visible == null) { checkpointStatus.setText(R.string.mobile_checkpoint_no_screen); return; }
        busy = true;
        MobileCommandRunner.BACKGROUND.execute(() -> {
            AtomicFile file = checkpointFile();
            FileOutputStream stream = null;
            String message;
            try {
                String saved = DateFormat.getDateTimeInstance().format(new Date()) + "\n\n" + MobileRecovery.boundedCheckpoint(visible);
                stream = file.startWrite();
                stream.write(saved.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                file.finishWrite(stream);
                message = getString(R.string.mobile_checkpoint_saved);
            } catch (Exception error) {
                if (stream != null) file.failWrite(stream);
                message = getString(R.string.mobile_operation_failed, error.getMessage());
            }
            showCheckpointStatus(message);
        });
    }
    private void showCheckpointStatus(String message) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            busy = false;
            checkpointStatus.setText(message);
        });
    }
    private void viewCheckpoint() {
        busy = true;
        MobileCommandRunner.BACKGROUND.execute(() -> {
            try {
                File file = checkpointFile().getBaseFile();
                if (file.length() > 110000) throw new java.io.IOException("Checkpoint exceeds the size limit");
                String saved = new String(checkpointFile().readFully(), java.nio.charset.StandardCharsets.UTF_8);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    busy = false;
                    TextView text = new TextView(this);
                    text.setText(saved);
                    text.setTextIsSelectable(true);
                    ScrollView scroll = new ScrollView(this);
                    scroll.addView(text);
                    new AlertDialog.Builder(this).setTitle(R.string.mobile_checkpoint_view).setView(scroll)
                        .setPositiveButton(android.R.string.ok, null).show();
                });
            } catch (Exception error) { showCheckpointStatus(getString(R.string.mobile_checkpoint_missing)); }
        });
    }
    private void listTmux() {
        String tmux = TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/tmux";
        if (!new File(tmux).canExecute()) { checkpointStatus.setText(R.string.mobile_tmux_install); return; }
        busy = true;
        MobileCommandRunner.BACKGROUND.execute(() -> {
            try {
                MobileCommandRunner.Result result = MobileCommandRunner.run(getApplicationContext(),
                    Arrays.asList(tmux, "list-sessions", "-F", "#{session_id}\t#{session_name}"));
                if (result.timedOut || result.exitCode != 0) { showCheckpointStatus(getString(R.string.mobile_tmux_none)); return; }
                List<MobileRecovery.TmuxSession> sessions = MobileRecovery.parseTmuxSessions(result.output);
                if (sessions.isEmpty()) { showCheckpointStatus(getString(R.string.mobile_tmux_none)); return; }
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    busy = false;
                    String[] labels = new String[sessions.size()];
                    for (int i = 0; i < labels.length; i++) labels[i] = sessions.get(i).name + " (" + sessions.get(i).id + ")";
                    new AlertDialog.Builder(this).setTitle(R.string.mobile_tmux_reconnect).setItems(labels, (dialog, index) -> {
                        setResult(RESULT_OK, new Intent().putExtra(EXTRA_TMUX_ID, sessions.get(index).id));
                        finish();
                    }).setNegativeButton(android.R.string.cancel, null).show();
                });
            } catch (Exception error) { showCheckpointStatus(getString(R.string.mobile_operation_failed, error.getMessage())); }
        });
    }
}
