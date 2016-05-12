
package com.shane.powertool.utils;

public class Constants {
    public static String CURRENT_NOW;
    public static int CURRENT_MULTI = 1;
    public static final String NORMAL_CURRENT_NOW = "/sys/class/power_supply/battery/current_now";
    public static final String HERMES_CURRENT_NOW = "/sys/class/power_supply/usb/device/FG_Battery_CurrentConsumption";
    public static final String BATTERY_TEMP = "/sys/class/power_supply/battery/temp";
    public static final String BRIGHTNESS_PATH = "/sys/class/leds/lcd-backlight/brightness";
    public static final String DEVICE_PRODUCT = "ro.build.product";

    public static final String SCALING_CUR_FREQ = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq";
    public static final String SCALING_MAX_FREQ = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq";

    public static final String LCD_MODE_PROPERTY = "persist.sys.display_cabc";
    public static final String POWERMODE_PROPERTY = "persist.sys.aries.power_profile";

    public static final int AVG_DURATION = 5;

    public static final String KEY_HAVE_CURRENT_NOW_PATH = "current_now";

    private Constants() {
    }
}
