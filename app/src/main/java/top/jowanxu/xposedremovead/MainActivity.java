package top.jowanxu.xposedremovead;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Resources localResources = getResources();
        Configuration localConfiguration = localResources.getConfiguration();
        localConfiguration.fontScale = 1.0F;
        localResources.updateConfiguration(localConfiguration, localResources.getDisplayMetrics());
        finish();
    }
}
