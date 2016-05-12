
package com.shane.powertool;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.shane.powertool.utils.Constants;
import com.shane.powertool.utils.DeviceUtil;
import com.shane.powertool.utils.FileUtil;
import com.shane.powertool.utils.TimeUtil;

import java.util.ArrayList;

public class CurrentAvgService extends Service {
    public static final String TAG = "CurrentAvgService";
    static ArrayList<Integer> mCacheCurrentList = new ArrayList<Integer>();
    static final int MSG_UPDATE_DATA = 0x01;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_UPDATE_DATA) {
                String current = FileUtil.readSystemFile(Constants.CURRENT_NOW);
                if (!current.isEmpty()) {
                    Integer cur = Integer.parseInt(current) / Constants.CURRENT_MULTI;
                    if (cur == 0) cur = Integer.parseInt(current); // may be mA
                    mCacheCurrentList.add(cur);
                    FileUtil.appendLog(TimeUtil.getFormatElapsedRealtime() + " : "
                            + String.valueOf(cur) + "mA");
                    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_DATA, 1000);
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        // 提高process优先级
        Uri uri = Settings.System.getUriFor(ACTIVITY_SERVICE);
        System.out.print("uri:" + uri);
        Log.d(TAG, "XiaomiDeviceName:" + DeviceUtil.getXiaomiDeviceName());
        FileUtil.appendLog("=start=" + TimeUtil.getFormatElapsedRealtime());
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_DATA, 1000);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    Integer computeAvg() {
        Integer sum = 0;
        for (Integer i : mCacheCurrentList) {
            sum += i;
        }
        if (mCacheCurrentList.size() > 0) {
            sum = sum / mCacheCurrentList.size();
        }
        mCacheCurrentList.clear();
        return sum;
    }

    private void WriteToFile() {
        Integer avg = computeAvg();
        FileUtil.appendLog("=end=" + TimeUtil.getFormatElapsedRealtime() + "==avg:" + avg + "mA");
        Toast.makeText(this, getString(R.string.avg, avg), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHandler != null) {
            mHandler.removeMessages(MSG_UPDATE_DATA);
            WriteToFile();
            mHandler = null;
        }
    }
}
