package ca.pkay.rcloneexplorer;

import android.app.Application;
import android.os.Build;

import com.google.android.material.color.DynamicColors;

/**
 * Application class that enables Material You (Monet engine) dynamic colors
 * on Android 12+ devices. On older devices, the default Material3 theme is used.
 */
public class RcxApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Apply dynamic colors (Material You / Monet engine) on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DynamicColors.applyToActivitiesIfAvailable(this);
        }
    }
}
