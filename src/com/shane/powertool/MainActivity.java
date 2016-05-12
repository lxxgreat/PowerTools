
package com.shane.powertool;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import com.shane.powertool.utils.Config;
import com.shane.powertool.utils.FileUtil;
import com.umeng.update.UmengUpdateAgent;

import miui.util;

import java.io.DataOutputStream;

public class MainActivity extends PreferenceActivity implements OnPreferenceChangeListener,
        LoadDialog.DialogCallback {
    public static final String TAG = MainActivity.class.getName();
    private static final String SERVICE_PACKAGE_NAME = "com.shane.powertool";
    private static final String SERVICE_CLASS_NAME = "com.shane.powertool.CurrentNowService";

    private static final String ENABLE_SERVICE_KEY = "enable_service";
    private static final String CURRENT_NOW_KEY = "current_now";
    private static final String CURRENT_AVG_KEY = "current_avg";
    private static final String BATTERY_TEMP_KEY = "battery_temp";
    private static final String MODE_CPU_KEY = "mode_group_cpu";
    private static final String MODE_LCD_KEY = "mode_group_lcd";
    private static final String OTHER_FPS_KEY = "other_group_fps";
    private static final String OTHER_BRIGHTNESS_KEY = "other_group_brightness";
    private static final String OTHER_LIGHT_KEY = "other_group_light";
    private static final String OTHER_TOP_ACTIVITY_KEY = "other_group_top_activity";
    private static final String STATISTICS_CURRENT_AVG_KEY = "statistics_group_current_avg";

    private CheckBoxPreference mEnableService;
    private CheckBoxPreference mEnableCurrentNow;
    private CheckBoxPreference mEnableCurrentAvg;
    private CheckBoxPreference mEnableBatteryTemp;
    private CheckBoxPreference mEnableCpuMode;
    private CheckBoxPreference mEnableLcdMode;
    private CheckBoxPreference mEnableOtherFps;
    private CheckBoxPreference mEnableOtherBrightness;
    private CheckBoxPreference mEnableOtherLight;
    private CheckBoxPreference mEnableOtherTopActivity;
    private CheckBoxPreference mEnableStatisticsCurrentAvg;

    private boolean mIsBindService;
    private Messenger mService;
    private Context mContext;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UmengUpdateAgent.update(this);
        //当前应用的代码执行目录
        upgradeRootPermission(getPackageCodePath());
        Shell.runShell("setenforce 0");
        mContext = this;
        addPreferencesFromResource(R.layout.main);
    }

    /**
     * 应用程序运行命令获取 Root权限，设备必须已破解(获得ROOT权限)
     * 
     * @return 应用程序是/否获取Root权限
     */
    public static boolean upgradeRootPermission(String pkgCodePath) {
        Process process = null;
        DataOutputStream os = null;
        try {
            String cmd="chmod 777 " + pkgCodePath;
            process = Runtime.getRuntime().exec("su"); //切换到root帐号
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
            }
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
    }

    @SuppressWarnings("deprecation")
    private void initView() {
        // init background
        mEnableService = (CheckBoxPreference) findPreference(ENABLE_SERVICE_KEY);
        mEnableCurrentNow = (CheckBoxPreference) findPreference(CURRENT_NOW_KEY);
        mEnableCurrentAvg = (CheckBoxPreference) findPreference(CURRENT_AVG_KEY);
        mEnableBatteryTemp = (CheckBoxPreference) findPreference(BATTERY_TEMP_KEY);
        mEnableCpuMode = (CheckBoxPreference) findPreference(MODE_CPU_KEY);
        mEnableLcdMode = (CheckBoxPreference) findPreference(MODE_LCD_KEY);
        mEnableOtherFps = (CheckBoxPreference) findPreference(OTHER_FPS_KEY);
        mEnableOtherBrightness = (CheckBoxPreference) findPreference(OTHER_BRIGHTNESS_KEY);
        mEnableOtherLight = (CheckBoxPreference) findPreference(OTHER_LIGHT_KEY);
        mEnableOtherTopActivity = (CheckBoxPreference) findPreference(OTHER_TOP_ACTIVITY_KEY);
        mEnableStatisticsCurrentAvg = (CheckBoxPreference) findPreference(STATISTICS_CURRENT_AVG_KEY);
        enableViews(mEnableService.isChecked());
        startRemoteService(mEnableService.isChecked());
        Log.i(TAG, "mEnableService.isChecked():" + mEnableService.isChecked());
    }

    private void enableViews(boolean enable) {
        mEnableCurrentNow.setEnabled(enable);
        mEnableCurrentAvg.setEnabled(enable);
        mEnableBatteryTemp.setEnabled(enable);
        mEnableCpuMode.setEnabled(enable);
        mEnableLcdMode.setEnabled(enable);
        mEnableOtherFps.setEnabled(enable);
        mEnableOtherBrightness.setEnabled(enable);
        mEnableOtherLight.setEnabled(enable);
        mEnableOtherTopActivity.setEnabled(enable);
        mEnableStatisticsCurrentAvg.setEnabled(enable);
    }

    private void updateCheckboxChanges() {
        updateCheckboxChange(CurrentNowService.MSG_DISPLAY_CURRENT_NOW,
                mEnableCurrentNow.isChecked());
        updateCheckboxChange(CurrentNowService.MSG_DISPLAY_CURRENT_AVG,
                mEnableCurrentAvg.isChecked());
        updateCheckboxChange(CurrentNowService.MSG_DISPLAY_BATTERY_TEMP,
                mEnableBatteryTemp.isChecked());
        updateCheckboxChange(CurrentNowService.MSG_DISPLAY_MODE_CPU,
                mEnableCpuMode.isChecked());
        updateCheckboxChange(CurrentNowService.MSG_DISPLAY_MODE_LCD,
                mEnableLcdMode.isChecked());
        updateCheckboxChange(CurrentNowService.MSG_DISPLAY_FPS,
                mEnableOtherFps.isChecked());
        updateCheckboxChange(CurrentNowService.MSG_DISPLAY_BRIGHTNESS,
                mEnableOtherBrightness.isChecked());
        updateCheckboxChange(CurrentNowService.MSG_DISPLAY_LIGHT,
                mEnableOtherLight.isChecked());
        updateCheckboxChange(CurrentNowService.MSG_DISPLAY_TOP_ACTIVITY,
                mEnableOtherTopActivity.isChecked());
        updateCheckboxChange(CurrentNowService.MSG_DISPLAY_STATISTICS_CURRENT_AVG,
                mEnableStatisticsCurrentAvg.isChecked());
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (ENABLE_SERVICE_KEY.equals(preference.getKey())) {
            startRemoteService(mEnableService.isChecked());
        } else if (CURRENT_NOW_KEY.equals(preference.getKey())) {
            updateCheckboxChange(CurrentNowService.MSG_DISPLAY_CURRENT_NOW,
                    mEnableCurrentNow.isChecked());
        } else if (CURRENT_AVG_KEY.equals(preference.getKey())) {
            updateCheckboxChange(CurrentNowService.MSG_DISPLAY_CURRENT_AVG,
                    mEnableCurrentAvg.isChecked());
        } else if (BATTERY_TEMP_KEY.equals(preference.getKey())) {
            updateCheckboxChange(CurrentNowService.MSG_DISPLAY_BATTERY_TEMP,
                    mEnableBatteryTemp.isChecked());
        } else if (MODE_CPU_KEY.equals(preference.getKey())) {
            updateCheckboxChange(CurrentNowService.MSG_DISPLAY_MODE_CPU,
                    mEnableCpuMode.isChecked());
        } else if (MODE_LCD_KEY.equals(preference.getKey())) {
            updateCheckboxChange(CurrentNowService.MSG_DISPLAY_MODE_LCD,
                    mEnableLcdMode.isChecked());
        } else if (OTHER_FPS_KEY.equals(preference.getKey())) {
            if (mEnableOtherFps.isChecked()) {
                LoadDialog.getSimpleDialog(mContext,
                        mContext.getString(R.string.dialog_display_fps_title),
                        mContext.getString(R.string.dialog_display_fps_content), false, this)
                        .show();
            } else {
                updateCheckboxChange(CurrentNowService.MSG_DISPLAY_FPS,
                        mEnableOtherFps.isChecked());
            }
        } else if (OTHER_BRIGHTNESS_KEY.equals(preference.getKey())) {
            updateCheckboxChange(CurrentNowService.MSG_DISPLAY_BRIGHTNESS,
                    mEnableOtherBrightness.isChecked());
        } else if (OTHER_LIGHT_KEY.equals(preference.getKey())) {
            updateCheckboxChange(CurrentNowService.MSG_DISPLAY_LIGHT,
                    mEnableOtherLight.isChecked());
        } else if (OTHER_TOP_ACTIVITY_KEY.equals(preference.getKey())) {
            updateCheckboxChange(CurrentNowService.MSG_DISPLAY_TOP_ACTIVITY,
                    mEnableOtherTopActivity.isChecked());
        } else if (STATISTICS_CURRENT_AVG_KEY.equals(preference.getKey())) {
            updateCheckboxChange(CurrentNowService.MSG_DISPLAY_STATISTICS_CURRENT_AVG,
                    mEnableStatisticsCurrentAvg.isChecked());
        }
        return true;
    }

    public void ok(Dialog dialog) {
        updateCheckboxChange(CurrentNowService.MSG_DISPLAY_FPS,
                mEnableOtherFps.isChecked());
        dialog.dismiss();
    }

    public void cancel(Dialog dialog) {
        mEnableOtherFps.setChecked(false);
        dialog.dismiss();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }

    private void startRemoteService(boolean start) {
        if (!Config.haveCurrentNowPath()) {
            Toast.makeText(mContext, mContext.getResources().getString(R.string.not_support),
                    Toast.LENGTH_LONG).show();
            mEnableService.setChecked(false);
            mEnableService.setEnabled(false);
            enableViews(false);
            return;
        }
        Intent intent = new Intent();
        intent.setClassName(SERVICE_PACKAGE_NAME, SERVICE_CLASS_NAME);
        if (start) {
            mIsBindService = bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
            // 防止按back键后service退出
            startService(intent);
        } else {
            if (mIsBindService) {
                unbindService(mServiceConnection);
                mIsBindService = false;
                stopService(intent);
            }
        }
        enableViews(start);
    }

    private void updateCheckboxChange(int what, boolean enable) {
        if (!mIsBindService) {
            return;
        }
        Message msg = Message.obtain();
        Bundle b = new Bundle();
        b.putBoolean(CurrentNowService.DISPLAY_KEY, enable);
        msg.what = what;
        msg.setData(b);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mIsBindService = false;
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service == null) {
                return;
            }
            mIsBindService = true;
            mService = new Messenger(service);
            updateCheckboxChanges();
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIsBindService) {
            unbindService(mServiceConnection);
            mIsBindService = false;
        }
    }
}
