package com.fuzz.indicator;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.NonNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.KITKAT;

/**
 * Accessible variant of
 * {@code com.android.ide.common.rendering.api.LayoutLog}.
 * <p>
 *     The original class can not be linked against from
 *     within Android Studio; this class uses reflection
 *     to access and call methods on that class.
 * </p>
 *
 * @author Philip Cohn-Cort (Fuzz)
 */
public abstract class LayoutLogger {
    private static LayoutLogger stub = new LayoutLogger() {
        @Override
        public void logToLayoutLib(String tag, String message) {
            // Do nothing.
        }
    };

    private static LayoutLogger functional = new LayoutLogger() {

        protected Object layoutLog;
        protected Method warning;

        @TargetApi(Build.VERSION_CODES.KITKAT)
        public void logToLayoutLib(String tag, String message) {
            if (SDK_INT >= KITKAT) {
                try {
                    getWarningMethod().invoke(getLayoutLog(), tag, message, null);
                } catch (ReflectiveOperationException ignore) {
                }
            }
        }

        public Method getWarningMethod() throws ClassNotFoundException, NoSuchMethodException {
            if (warning == null) {
                Class<?> LayoutLog = getClass().getClassLoader().loadClass("com.android.ide.common.rendering.api.LayoutLog");
                warning = LayoutLog.getDeclaredMethod("warning", String.class, String.class, Object.class);
            }
            return warning;
        }

        public Object getLayoutLog() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
            if (layoutLog == null) {
                Class<?> Bridge = getClass().getClassLoader().loadClass("com.android.layoutlib.bridge.Bridge");
                Method getLog = Bridge.getDeclaredMethod("getLog");
                layoutLog = getLog.invoke(null);
            }
            return layoutLog;
        }
    };

    /**
     * Utility method for getting a proper LayoutLogger instance. In edit mode,
     * the returned object will use reflection to display messages in your IDE;
     * anywhere else, it'll be basically a noop.
     *
     * @param inEditMode    whether we are currently in edit mode.
     * @return a non-null LayoutLogger
     * @see android.view.View#isInEditMode()
     */
    @NonNull
    public static LayoutLogger getPreferred(boolean inEditMode) {
        if (inEditMode) {
            return functional;
        } else {
            return stub;
        }
    }

    /**
     * Edit-mode utility for logging warnings out to the preview screen.
     *
     * This method does nothing at runtime.
     *
     * @param tag        a logging tag, used to group output messages and hide duplicates
     * @param message    the desired message
     */
    public abstract void logToLayoutLib(String tag, String message);
}
