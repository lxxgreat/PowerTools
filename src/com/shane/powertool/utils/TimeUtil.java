
package com.shane.powertool.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class TimeUtil {
    static SimpleDateFormat sFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static Calendar sCalendar = Calendar.getInstance();
    static final long SYSTEM_BOOT_TIME = java.lang.System.currentTimeMillis()
            - android.os.SystemClock.elapsedRealtime();

    public static String getDateAndTime(long timeInMills) {
        sCalendar.setTimeInMillis(timeInMills);
        return sFormat.format(sCalendar.getTime());
    }

    public static String getFormatCurrentTime() {
        return getDateAndTime(System.currentTimeMillis());
    }

    public static String getFormatElapsedRealtime() {
        long currentTimeInMills = SYSTEM_BOOT_TIME + android.os.SystemClock.elapsedRealtime();
        return getDateAndTime(currentTimeInMills);
    }
}
