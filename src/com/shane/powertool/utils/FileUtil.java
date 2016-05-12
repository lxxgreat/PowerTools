
package com.shane.powertool.utils;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileUtil {
    public static void appendLog(String text) {
        File logFile = new File("sdcard/current.log");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            // BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readSystemFile(String file) {
        BufferedReader buffered_reader = null;
        String result = "";
        try {
            buffered_reader = new BufferedReader(new FileReader(file));
            String line = "";
            while ((line = buffered_reader.readLine()) != null) {
                result += line;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            result = "";
        } catch (Exception e) {
            e.printStackTrace();
            result = "";
        } finally {
            try {
                if (buffered_reader != null)
                    buffered_reader.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }

    public static String getSystemProperty(String key) {
        String cmd = "getprop " + key;
        return execCmd(cmd);
    }
    
    public static void initCurrentNow() {
        String res = readSystemFile(Constants.HERMES_CURRENT_NOW);
        System.out.println("currentnow: " + res);
        if (!TextUtils.isEmpty(res)) {
            Constants.CURRENT_MULTI = 10;
            Constants.CURRENT_NOW = Constants.HERMES_CURRENT_NOW;
        } else {
            Constants.CURRENT_MULTI = 1000;
            Constants.CURRENT_NOW = Constants.NORMAL_CURRENT_NOW;
        }
    }

    public static void initCurrentNow2() {
        String cmd = "getprop " + Constants.DEVICE_PRODUCT;
        String res = execCmd(cmd);
        Log.i("initCurrentNow", res);
        if (res.contains("hermes") || res.contains("HM2013023") || res.contains("hennessy")) {
            Constants.CURRENT_MULTI = 10;
            Constants.CURRENT_NOW = Constants.HERMES_CURRENT_NOW;
        } else {
            Constants.CURRENT_MULTI = 1000;
            Constants.CURRENT_NOW = Constants.NORMAL_CURRENT_NOW;
        }
    }
    
    public static String getTopActivity() {
        String cmd = "dumpsys activity activities";
        return execCmd(cmd);
    }

    public static String execCmd(final String cmd) {
        Runtime runtime = Runtime.getRuntime();
        Process proc;
        try {
            proc = runtime.exec(cmd);
            if (proc.waitFor() != 0) {
                System.err.println("exit value = " + proc.exitValue());
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    proc.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String line = null;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (InterruptedException e) {
            System.err.println(e);
            return null;
        } catch (IOException e) {
            System.err.println(e);
            return null;
        }
    }
}
