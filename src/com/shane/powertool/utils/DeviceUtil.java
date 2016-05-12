
package com.shane.powertool.utils;

import android.os.Build;

public class DeviceUtil {
    public static String getXiaomiDeviceName() {
        String model = Build.MODEL;
        return (model == null) ? "" : model;
    }
}
