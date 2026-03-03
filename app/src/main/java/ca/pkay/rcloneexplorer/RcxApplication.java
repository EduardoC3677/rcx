package ca.pkay.rcloneexplorer;

import android.app.Application;
import com.google.android.material.color.DynamicColors;

public class RcxApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Apply Material You dynamic colors (Monet Engine) on Android 12+ devices
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
