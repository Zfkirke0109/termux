package com.termux.app.terminal;

import android.app.Application;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.TextView;
import com.termux.R;
import com.termux.shared.termux.extrakeys.ExtraKeysConstants;
import com.termux.shared.termux.extrakeys.ExtraKeysInfo;
import com.termux.shared.termux.extrakeys.ExtraKeysView;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.logger.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class MobileControlsUiTest {
    private Context context;
    @Before public void setup() {
        context = RuntimeEnvironment.getApplication();
        MobileDebugLogging.normal(context);
    }
    @After public void cleanup() { MobileDebugLogging.normal(context); }

    @Test public void narrowExtraKeysKeepLabelsOnOneLineAndPreserveKeyCodes() throws Exception {
        Context theme = new ContextThemeWrapper(context, R.style.Theme_TermuxApp_DayNight_DarkActionBar);
        ExtraKeysView keys = new ExtraKeysView(theme, null);
        ExtraKeysInfo info = new ExtraKeysInfo("[['SHIFT','PGUP','PGDN']]", "none", ExtraKeysConstants.CONTROL_CHARS_ALIASES);
        keys.reload(info, 48);
        keys.measure(View.MeasureSpec.makeMeasureSpec(240, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(64, View.MeasureSpec.EXACTLY));
        keys.layout(0, 0, 240, 64);
        for (int i = 0; i < 3; i++) {
            TextView button = (TextView) keys.getChildAt(i);
            assertEquals(1, button.getMaxLines());
            assertEquals(info.getMatrix()[0][i].getDisplay(), button.getContentDescription());
            assertNotNull(button.getLayout());
            assertEquals(1, button.getLayout().getLineCount());
        }
        assertEquals("SHIFT", info.getMatrix()[0][0].getKey());
        assertEquals("PGUP", info.getMatrix()[0][1].getKey());
        assertEquals("PGDN", info.getMatrix()[0][2].getKey());
    }

    @Test public void highlightColumnsAccountForWideAndCombiningCharacters() {
        assertEquals(2, MobileTerminalActions.columnForIndex("終error", 1, false));
        assertEquals(1, MobileTerminalActions.columnForIndex("終error", 1, true));
        assertEquals(0, MobileTerminalActions.columnForIndex("a\u0301b", 1, false));
        assertEquals(0, MobileTerminalActions.columnForIndex("a\u0301b", 2, true));
        assertEquals(2, MobileTerminalActions.columnForIndex("😀error", 2, false));
    }

    @Test public void timedDebuggingNeverEnablesKeyLoggingAndNormalEndsIt() {
        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(context);
        assertNotNull(preferences);
        preferences.setTerminalViewKeyLoggingEnabled(true);
        MobileDebugLogging.start(context);
        assertEquals(Logger.LOG_LEVEL_DEBUG, preferences.getLogLevel());
        assertFalse(preferences.isTerminalViewKeyLoggingEnabled());
        assertTrue(MobileDebugLogging.isActive(context));
        MobileDebugLogging.normal(context);
        assertEquals(Logger.LOG_LEVEL_NORMAL, preferences.getLogLevel());
        assertFalse(MobileDebugLogging.isActive(context));
    }

    @Test public void manualLogLevelOverridesArePreserved() {
        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(context);
        MobileDebugLogging.start(context);
        preferences.setLogLevel(null, Logger.LOG_LEVEL_VERBOSE);
        MobileDebugLogging.check(context);
        assertFalse(MobileDebugLogging.isActive(context));
        assertEquals(Logger.LOG_LEVEL_VERBOSE, preferences.getLogLevel());
    }
}
