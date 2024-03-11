package ca.chris_macleod.passiverecorder;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;

public class MainActivityTest {
    private final static String PACKAGE_NAME = "ca.chris_macleod.passiverecorder";
    private final static int DEFAULT_TIMEOUT = 5000;

    private UiDevice device;

    @BeforeEach
    public void startMainActivityFromHomeScreen() {
        device = UiDevice.getInstance(getInstrumentation());
        device.pressHome();

        final Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_HOME);


        PackageManager pm = getApplicationContext().getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY);
        MatcherAssert.assertThat(resolveInfo, notNullValue());
        final String launcherPackage = resolveInfo.activityInfo.packageName;
        MatcherAssert.assertThat(launcherPackage, notNullValue());
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), DEFAULT_TIMEOUT);

        // Launch the app
        Context context = getApplicationContext();
        final Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(PACKAGE_NAME);
        MatcherAssert.assertThat(intent, notNullValue());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear out any previous instances
        context.startActivity(intent);

        // Wait for the app to appear
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), DEFAULT_TIMEOUT);
    }

    @Test
    public void checkPreconditions() {
        MatcherAssert.assertThat(device, notNullValue());
    }

    @Test
    public void saveButtonDisabledWhenNotRecording() {
        onView(withId(R.id.saveButton)).check(matches(not(isEnabled())));
    }
}
