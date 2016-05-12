package miui.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import miui.util.ReflectionUtils;
/*
 * Compat for miui.shell service
 * Version 6: there is on android.miui.shell in miuiFramework.jar
 * Version 5-: on miui.os.shell in framework_ext.jar
 */
public class Shell {
    private static Class<?> sShell;
    private static Method sMkdirs;
    private static Method sChown;
    private static Method sRunShell;

    static {
        try {
            sShell = ReflectionUtils.findClass("miui.os.Shell", null);  
        } catch (Throwable e) {
            //pass
        }
        if (sShell == null) {
            try {
                sShell = ReflectionUtils.findClass("android.miui.Shell", null);
            } catch (Throwable e) {
              //pass
            }
        }
        if (sShell != null) {
            try {
                sMkdirs = ReflectionUtils.findMethodExact(sShell, "mkdirs", String.class);
                sChown = ReflectionUtils.findMethodExact(sShell, "chown", String.class, int.class, int.class);
                sRunShell = ReflectionUtils.findMethodExact(sShell, "runShell", String.class, Object[].class);
            } catch (Throwable e) {
                //pass
            }
        }
    }

    public static boolean mkdirs(String path) {
        Boolean ret = false;
        if (sMkdirs != null) {
            try {
                ret = (Boolean)sMkdirs.invoke(null, path);
            } catch (Throwable e) {
                //pass
            }
        }
        return ret;
    }

    public static boolean chown(String path, int owner, int group) {
        Boolean ret = false;
        if (sChown != null) {
            try {
                ret = (Boolean)sChown.invoke(null, path, owner, group);
            } catch (Throwable e) {
                //pass
            }
        }
        return ret;
    }

    public static boolean runShell(String cmd) {
        Boolean ret = false;
        if (sRunShell != null) {
            try {
                ret = (Boolean)sRunShell.invoke(null, cmd, new Object[0]);
            } catch (Throwable e) {
                //pass
            }
        }
        return ret;
    }
}
