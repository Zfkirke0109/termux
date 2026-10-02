package com.termux.app.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.termux.R;
import com.termux.app.terminal.MobileCommandRunner;
import com.termux.app.terminal.MobileServiceStatus;
import com.termux.shared.termux.TermuxConstants;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Explicit controls for existing termux-services; opening this panel never starts a service. */
public class MobileServicesActivity extends AppCompatActivity {
    private LinearLayout content;
    private boolean busy;
    private static final String BIN = TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/";
    private static final File ROOT = new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH, "var/service");
    private static final int MAX_SERVICES = 64;

    private static final class Entry {
        final String name;
        final MobileServiceStatus status;
        Entry(String name, MobileServiceStatus status) { this.name = name; this.status = status; }
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle(R.string.mobile_services_title);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(16 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding, padding, padding);
        scroll.addView(content);
        setContentView(scroll);
        refresh();
    }
    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    private TextView label(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextIsSelectable(true);
        text.setPadding(0, 12, 0, 12);
        content.addView(text);
        return text;
    }
    private void button(LinearLayout parent, int title, Runnable action) {
        Button button = new Button(this);
        button.setText(title);
        button.setOnClickListener(v -> { if (!busy) action.run(); });
        parent.addView(button, new LinearLayout.LayoutParams(0, -2, 1));
    }

    private File service(String name) throws IOException {
        if (!MobileServiceStatus.isValidName(name)) throw new IOException("Invalid service name");
        File file = new File(ROOT, name);
        // Private home symlinks are supported; shared-storage service paths are excluded.
        if (!file.getCanonicalPath().startsWith(new File(TermuxConstants.TERMUX_FILES_DIR_PATH).getCanonicalPath() + "/"))
            throw new IOException("Service path is outside Termux's private files");
        return file;
    }

    private void refresh() {
        busy = true;
        content.removeAllViews();
        label(getString(R.string.mobile_services_loading));
        MobileCommandRunner.BACKGROUND.execute(() -> {
            try {
                File[] directories = ROOT.listFiles(file -> file.isDirectory() && MobileServiceStatus.isValidName(file.getName()));
                if (directories == null || !new File(BIN + "sv").canExecute()) {
                    runOnUiThread(() -> showUnavailable());
                    return;
                }
                Arrays.sort(directories, Comparator.comparing(File::getName));
                int count = Math.min(directories.length, MAX_SERVICES);
                List<String> command = new ArrayList<>(Arrays.asList(BIN + "sv", "status"));
                for (int i = 0; i < count; i++) command.add(service(directories[i].getName()).getPath());
                String statusOutput = count == 0 ? "" : MobileCommandRunner.run(getApplicationContext(), command).output;
                List<Entry> entries = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    String name = directories[i].getName();
                    String prefix = service(name).getPath() + ":";
                    String status = "";
                    for (String line : statusOutput.split("\\n")) {
                        if (line.startsWith("run: " + prefix) || line.startsWith("down: " + prefix)) { status = line; break; }
                    }
                    entries.add(new Entry(name, MobileServiceStatus.parse(status, !new File(service(name), "down").exists())));
                }
                runOnUiThread(() -> show(entries, directories.length > MAX_SERVICES));
            } catch (Exception error) {
                runOnUiThread(() -> failure(error));
            }
        });
    }
    private void showUnavailable() {
        if (isFinishing() || isDestroyed()) return;
        busy = false;
        content.removeAllViews();
        label(getString(R.string.mobile_services_install));
        addRefresh();
    }
    private void show(List<Entry> entries, boolean limited) {
        if (isFinishing() || isDestroyed()) return;
        busy = false;
        content.removeAllViews();
        label(getString(R.string.mobile_services_explanation));
        if (entries.isEmpty()) label(getString(R.string.mobile_services_empty));
        if (limited) label(getString(R.string.mobile_services_limit));
        for (Entry entry : entries) {
            int stateText = entry.status.pid > 0 ? R.string.mobile_service_running
                : "stopped".equals(entry.status.state) ? R.string.mobile_service_stopped : R.string.mobile_service_unavailable;
            label(entry.name + "\n" + getString(stateText, entry.status.pid) + "\n"
                + getString(entry.status.startupEnabled ? R.string.mobile_startup_enabled : R.string.mobile_startup_disabled));
            LinearLayout controls = new LinearLayout(this);
            content.addView(controls);
            button(controls, R.string.mobile_service_start, () -> change(entry.name, "up"));
            button(controls, R.string.mobile_service_stop, () -> new AlertDialog.Builder(this)
                .setMessage(getString(R.string.mobile_service_stop_confirm, entry.name))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.mobile_service_stop, (dialog, which) -> change(entry.name, "down")).show());
            LinearLayout startup = new LinearLayout(this);
            content.addView(startup);
            button(startup, entry.status.startupEnabled ? R.string.mobile_disable_startup : R.string.mobile_enable_startup,
                () -> change(entry.name, entry.status.startupEnabled ? "disable" : "enable"));
        }
        addRefresh();
    }
    private void addRefresh() {
        LinearLayout row = new LinearLayout(this);
        content.addView(row);
        button(row, R.string.mobile_refresh, this::refresh);
    }
    private void change(String name, String operation) {
        busy = true;
        label(getString(R.string.mobile_services_loading));
        MobileCommandRunner.BACKGROUND.execute(() -> {
            try {
                File directory = service(name);
                if (!directory.isDirectory()) throw new IOException("Service was removed");
                if ("enable".equals(operation) || "disable".equals(operation)) {
                    File down = new File(directory, "down");
                    if ("enable".equals(operation)) {
                        if (down.exists() && !down.delete()) throw new IOException("Could not enable startup");
                    } else if (!down.exists() && !down.createNewFile()) throw new IOException("Could not disable startup");
                } else {
                    MobileCommandRunner.Result result = MobileCommandRunner.run(getApplicationContext(),
                        Arrays.asList(BIN + "sv", "-w", "5", operation, directory.getPath()));
                    if (result.timedOut || result.exitCode != 0) throw new IOException(result.output.isEmpty()
                        ? "Service command did not complete" : result.output);
                }
                runOnUiThread(() -> { if (!isFinishing() && !isDestroyed()) refresh(); });
            } catch (Exception error) { runOnUiThread(() -> failure(error)); }
        });
    }
    private void failure(Exception error) {
        if (isFinishing() || isDestroyed()) return;
        busy = false;
        label(getString(R.string.mobile_operation_failed, error.getMessage()));
        addRefresh();
    }
}
