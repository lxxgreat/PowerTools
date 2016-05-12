
package com.shane.powertool;

import android.app.Application;

import com.shane.powertool.utils.Config;
import com.shane.powertool.utils.FileUtil;

public class CurrentNowApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FileUtil.initCurrentNow();
        Config.init(this);
    }

    @Override
    public void onTerminate() {
    }
}
