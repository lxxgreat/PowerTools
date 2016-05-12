
package com.shane.powertool.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.shane.powertool.R;

import java.util.HashMap;

public class Config {
    public static final String TAG = "Config";
    private static final String CONFIG_FILE = "miui.power.config";

    private Config() {
    }

    static HashMap<String, String> sCpuModeMap = null;
    static HashMap<String, String> sLcdModeMap = null;
    private static SharedPreferences sConfig = null;
    private static SharedPreferences.Editor sEditor;

    public static void init(Context context) {
        if (sCpuModeMap == null) {
            sCpuModeMap = new HashMap<String, String>();
            String[] keys = context.getResources().getStringArray(R.array.cpu_mode_keys);
            String[] values = context.getResources().getStringArray(R.array.cpu_mode_values);
            for (int i = 0; i < keys.length; i++) {
                sCpuModeMap.put(keys[i], values[i]);
            }
        }

        if (sLcdModeMap == null) {
            sLcdModeMap = new HashMap<String, String>();
            String[] keys = context.getResources().getStringArray(R.array.lcd_mode_new_keys);
            String[] values = context.getResources().getStringArray(R.array.lcd_mode_values);
            for (int i = 0; i < keys.length; i++) {
                sLcdModeMap.put(keys[i], values[i]);
            }
        }

        if (sConfig == null) {
            sConfig = context.getApplicationContext().getSharedPreferences(CONFIG_FILE,
                    Context.MODE_PRIVATE);
            sEditor = sConfig.edit();
            if (FileUtil.readSystemFile(Constants.CURRENT_NOW).isEmpty()) {
                storeHaveCurrentNowPath(false);
            } else {
                storeHaveCurrentNowPath(true);
            }
        }
    }

    public static String getLcdModeValues(String key) {
        if (sLcdModeMap != null) {
            return sLcdModeMap.get(key);
        } else {
            return null;
        }
    }

    public static String getCpuModeValues(String key) {
        if (sCpuModeMap != null) {
            return sCpuModeMap.get(key);
        } else {
            return null;
        }
    }

    public static boolean haveCurrentNowPath() {
        return true;
        //return sConfig.getBoolean(Constants.KEY_HAVE_CURRENT_NOW_PATH, true);
    }

    public static void storeHaveCurrentNowPath(boolean have) {
        sEditor.putBoolean(Constants.KEY_HAVE_CURRENT_NOW_PATH, have);
        sEditor.commit();
    }
}
