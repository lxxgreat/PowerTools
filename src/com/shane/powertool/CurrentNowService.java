
package com.shane.powertool;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.shane.powertool.utils.Config;
import com.shane.powertool.utils.Constants;
import com.shane.powertool.utils.DeviceUtil;
import com.shane.powertool.utils.FileUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class CurrentNowService extends Service implements View.OnClickListener {
    public static final String TAG = "CurrentNowService";
    public static final String DISPLAY_KEY = "display";
    public static final int MSG_DISPLAY_CURRENT_NOW = 0x01;
    public static final int MSG_DISPLAY_CURRENT_AVG = 0x02;
    public static final int MSG_DISPLAY_BATTERY_TEMP = 0x03;
    public static final int MSG_DISPLAY_MODE_CPU = 0x04;
    public static final int MSG_DISPLAY_MODE_LCD = 0x05;
    public static final int MSG_DISPLAY_STATISTICS_CURRENT_AVG = 0x06;
    public static final int MSG_DISPLAY_FPS = 0x07;
    public static final int MSG_DISPLAY_TOP_ACTIVITY = 0x08;
    public static final int MSG_DISPLAY_BRIGHTNESS = 0x09;
    public static final int MSG_DISPLAY_LIGHT = 0x0a;

    private final static int TEXT_VIEW_ID = 1;
    private final static int START_BTN_ID = 2;
    private final static int STOP_BTN_ID = 3;

    boolean mIsDisplayCurrentNow;
    boolean mIsDisplayCurrentAvg;
    boolean mIsDisplayBatteryTemp;
    boolean mIsDisplayCpuMode;
    boolean mIsDisplayLcdMode;
    boolean mIsDisplayStatisticsCurrentAvg;
    boolean mIsDisplayFps;
    boolean mIsDisplayTopActivity;
    boolean mIsDisplayBrightness;
    boolean mIsDisplayLight;
    private SensorManager mSensorManager;
    private Sensor mLightSensor = null;
    private LightSensorListener mLightSensorListener;
    private int mLightValue;

    // 定义浮动窗口布局
    WindowManager.LayoutParams mTextLayoutParams;
    RelativeLayout mBtnLayout;
    WindowManager.LayoutParams mBtnLayoutParams;
    // 创建浮动窗口设置布局参数的对象
    WindowManager mWindowManager;
    ActivityManager mActivityManager;

    LoadView mFloatView;
    FpsView mFloatFpsView;
    TextView mFloatDragView;
    Button mStartBtn;
    Button mStopBtn;

    boolean mHaveCurrentPath = true;
    // 开始触控的坐标，移动时的坐标（相对于屏幕左上角的坐标）
    private int mTouchStartX, mTouchStartY;
    private int mTouchCurrentX, mTouchCurrentY;

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DISPLAY_CURRENT_NOW:
                    mIsDisplayCurrentNow = msg.getData().getBoolean(DISPLAY_KEY, false);
                    break;
                case MSG_DISPLAY_CURRENT_AVG:
                    mIsDisplayCurrentAvg = msg.getData().getBoolean(DISPLAY_KEY, false);
                    break;
                case MSG_DISPLAY_BATTERY_TEMP:
                    mIsDisplayBatteryTemp = msg.getData().getBoolean(DISPLAY_KEY, false);
                    break;
                case MSG_DISPLAY_MODE_CPU:
                    mIsDisplayCpuMode = msg.getData().getBoolean(DISPLAY_KEY, false);
                    break;
                case MSG_DISPLAY_MODE_LCD:
                    mIsDisplayLcdMode = msg.getData().getBoolean(DISPLAY_KEY, false);
                    break;
                case MSG_DISPLAY_STATISTICS_CURRENT_AVG:
                    mIsDisplayStatisticsCurrentAvg = msg.getData().getBoolean(DISPLAY_KEY, false);
                    if (mIsDisplayStatisticsCurrentAvg) {
                        createFloatBtnView();
                    } else {
                        if (mBtnLayout != null) {
                            mWindowManager.removeView(mBtnLayout);
                            mBtnLayout = null;
                        }
                    }
                    break;
                case MSG_DISPLAY_FPS:
                    mIsDisplayFps = msg.getData().getBoolean(DISPLAY_KEY, false);
                    if (mIsDisplayFps) {
                        createFloatFpsView();
                    } else {
                        if (mFloatFpsView != null) {
                            mFloatFpsView.removeCallbacks(mInvalidateRunnable);
                            mWindowManager.removeView(mFloatFpsView);
                            mFloatFpsView = null;
                        }
                    }
                    break;
                case MSG_DISPLAY_BRIGHTNESS:
                    mIsDisplayBrightness = msg.getData().getBoolean(DISPLAY_KEY, false);
                    break;
                case MSG_DISPLAY_LIGHT:
                    mIsDisplayLight = msg.getData().getBoolean(DISPLAY_KEY, false);
                    mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
                    if (mIsDisplayLight) {
                        mLightSensorListener = new LightSensorListener();
                        mSensorManager.registerListener(mLightSensorListener, SensorManager.SENSOR_LIGHT, SensorManager.SENSOR_DELAY_UI);
                    } else {
                        if (mLightSensorListener != null) {
                            mSensorManager.unregisterListener(mLightSensorListener);
                            mLightSensorListener = null;                            
                        }
                    }
                    break;                    
                case MSG_DISPLAY_TOP_ACTIVITY:
                    mIsDisplayTopActivity = msg.getData().getBoolean(DISPLAY_KEY, false);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    class LightSensorListener implements SensorListener {
        @Override
        public void onSensorChanged(int sensor, float[] values) {
            mLightValue = (int)values[0];
        }

        @Override
        public void onAccuracyChanged(int sensor, int accuracy) {
        }
    }

    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        if (!mHaveCurrentPath) {
            return null;
        } else {
            return mMessenger.getBinder();
        }
    }

    private final class CurrentTracker {
        boolean mHaveCurrentPath;
        boolean mHaveBatteryTempPath;
        String mLoadText = "";
        int mLoadWidth;
        private CurrentAvg mCurrentAvg = new UpdateCurrentAvg(Constants.AVG_DURATION);
        ArrayList<String> mTextList = new ArrayList<String>();
        private final Paint mPaint;
        Context mContext;

        CurrentTracker(Paint paint, Context context) {
            mPaint = paint;
            mContext = context;

            mHaveCurrentPath = true;
            mHaveBatteryTempPath = true;
        }

        public void init() {
            if (!Config.haveCurrentNowPath()) {
                mHaveCurrentPath = false;
                CurrentNowService.this.mHaveCurrentPath = false;
                return;
            }

            if (FileUtil.readSystemFile(Constants.BATTERY_TEMP).isEmpty()) {
                mHaveBatteryTempPath = false;
            }
        }

        public void updateData() {
            if (!mHaveCurrentPath) {
                return;
            }

            String current = FileUtil.readSystemFile(Constants.CURRENT_NOW);
            Integer cur = Integer.parseInt(current) / Constants.CURRENT_MULTI;
            if (cur == 0) cur = Integer.parseInt(current); // may be mA
            mCurrentAvg.add(cur);
            if (cur > 0) {
                mLoadText = mContext.getString(R.string.discharging, cur);
            } else {
                mLoadText = mContext.getString(R.string.charging, -cur);
            }

            mTextList.clear();
            if (mIsDisplayCurrentAvg) {
                mTextList.add(mContext.getString(R.string.avg, mCurrentAvg.getAvgCurrent()));
            }

            if (mIsDisplayBatteryTemp && mHaveBatteryTempPath) {
                String temp = FileUtil.readSystemFile(Constants.BATTERY_TEMP);
                mTextList.add(mContext.getString(R.string.battery_temp,
                        Double.valueOf(temp) / 10));
            }

            // int freq1 =
            // Integer.valueOf(FileUtil.readSystemFile(Constants.SCALING_CUR_FREQ));
            // result += getApplication().getString(R.string.scaling_cur_freq,
            // freq1) + "\n";
            // int freq2 =
            // Integer.valueOf(FileUtil.readSystemFile(Constants.SCALING_MAX_FREQ));
            // result += getApplication().getString(R.string.scaling_max_freq,
            // freq2) + "\n";

            if (mIsDisplayFps && mFloatFpsView != null) {
                mTextList.add(mContext.getString(R.string.fps, mFloatFpsView.getRealFps()));
            }
            if (mIsDisplayBrightness) {
                String temp = FileUtil.readSystemFile(Constants.BRIGHTNESS_PATH);
                if (temp.isEmpty()) {
                    try {
                        int curBrightnessValue=android.provider.Settings.System.getInt(
                        getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS);
                        mTextList.add(mContext.getString(R.string.brightness, curBrightnessValue));
                    } catch (SettingNotFoundException e) {
                        e.printStackTrace();
                    }
                } else {
                    mTextList.add(mContext.getString(R.string.brightness, Integer.valueOf(temp)));
                }
            }

            if (mIsDisplayLight) {
                mTextList.add(mContext.getString(R.string.light, mLightValue));
            }

            if (mIsDisplayCpuMode) {
                String cpuMode = Config.getCpuModeValues(FileUtil
                        .getSystemProperty(Constants.POWERMODE_PROPERTY));
                mTextList.add(mContext.getString(R.string.cpu_mode,
                        cpuMode));
            }

            if (mIsDisplayLcdMode) {
                String lcdMode = Config.getLcdModeValues(FileUtil
                        .getSystemProperty(Constants.LCD_MODE_PROPERTY));
                mTextList.add(mContext.getString(R.string.lcd_mode,
                        lcdMode));
            }

            if (mIsDisplayTopActivity) {
                String activePackages;
                if (Build.VERSION.SDK_INT > 20) {
                    activePackages = getActivePackages();
                } else {
                    activePackages = getActivePackagesCompat();
                }
                if (activePackages != null) {
                    mTextList.add(mContext.getString(R.string.top_activity,
                            activePackages));
                }
            }
            // Log.d(TAG, "mTextList:" + mTextList);
            onLoadChanged();
        }

        public void onLoadChanged() {
            mLoadWidth = (int) mPaint.measureText(mLoadText);
        }

        public int onMeasureText(String name) {
            return (int) mPaint.measureText(name);
        }
    }

    String getActivePackagesCompat() {
        final List<ActivityManager.RunningTaskInfo> taskInfo = mActivityManager.getRunningTasks(1);
        final ComponentName componentName = taskInfo.get(0).topActivity;
        return componentName.getClassName();
    }

    String getActivePackages() {
        final int PROCESS_STATE_TOP = 2;
        RunningAppProcessInfo currentProcessInfo = null;
        Field field = null;
        try {
            field = RunningAppProcessInfo.class.getDeclaredField("processState");
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<RunningAppProcessInfo> appList = mActivityManager.getRunningAppProcesses();
        for (RunningAppProcessInfo app : appList) {
            if (app.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    app.importanceReasonCode == 0) {
                Integer state = null;
                try {
                    state = field.getInt(app);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (state != null && state == PROCESS_STATE_TOP) {
                    currentProcessInfo = app;
                    break;
                }
            }
        }
        if (currentProcessInfo != null) {
            return currentProcessInfo.processName;
        } else {
            return "null";
        }
        
    }

    private class LoadView extends View {
        static final int MSG_UPDATE_DATA = 0x01;
        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MSG_UPDATE_DATA) {
                    asyncLoadData();
                }
            }
        };

        private void asyncLoadData() {
            AsyncTask<Void, Void, Void> loadDataTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    mStats.updateData();
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    updateDisplay();
                    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_DATA, 1000);
                }
            };
            loadDataTask.execute();
        }

        private final CurrentTracker mStats;

        private Paint mLoadPaint;
        private Paint mShadowPaint;
        private Paint mShadow2Paint;
        private float mAscent;
        private int mFH;

        protected int mPaddingRight = 0;
        protected int mPaddingTop = 0;
        protected int mPaddingLeft = 0;
        protected int mPaddingBottom = 0;

        private int mNeededWidth;
        private int mNeededHeight;

        LoadView(Context c) {
            super(c);

            setPadding(4, 4, 4, 4);
            // setBackgroundResource(com.android.internal.R.drawable.load_average_background);

            // Need to scale text size by density... but we won't do it
            // linearly, because with higher dps it is nice to squeeze the
            // text a bit to fit more of it. And with lower dps, trying to
            // go much smaller will result in unreadable text.
            int textSize = 10;
            float density = c.getResources().getDisplayMetrics().density;
            if (density < 1) {
                textSize = 9;
            } else {
                textSize = (int) (10 * density);
                if (textSize < 10) {
                    textSize = 10;
                }
            }
            mLoadPaint = new Paint();
            mLoadPaint.setAntiAlias(true);
            mLoadPaint.setTextSize(textSize);
            mLoadPaint.setARGB(255, 255, 255, 255);

            mShadowPaint = new Paint();
            mShadowPaint.setAntiAlias(true);
            mShadowPaint.setTextSize(textSize);
            // mShadowPaint.setFakeBoldText(true);
            mShadowPaint.setARGB(192, 0, 0, 0);
            mLoadPaint.setShadowLayer(4, 0, 0, 0xff000000);

            mShadow2Paint = new Paint();
            mShadow2Paint.setAntiAlias(true);
            mShadow2Paint.setTextSize(textSize);
            // mShadow2Paint.setFakeBoldText(true);
            mShadow2Paint.setARGB(192, 0, 0, 0);
            mLoadPaint.setShadowLayer(2, 0, 0, 0xff000000);

            mAscent = mLoadPaint.ascent();
            float descent = mLoadPaint.descent();
            mFH = (int) (descent - mAscent + 0.5f);

            mStats = new CurrentTracker(mLoadPaint, c);
            mStats.init();
            Config.storeHaveCurrentNowPath(mHaveCurrentPath);
            updateDisplay();
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            mHandler.sendEmptyMessage(MSG_UPDATE_DATA);
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            mHandler.removeMessages(MSG_UPDATE_DATA);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(resolveSize(mNeededWidth, widthMeasureSpec),
                    resolveSize(mNeededHeight, heightMeasureSpec));
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            final int RIGHT = getWidth() - 1;
            int y = mPaddingTop - (int) mAscent;
            if (mIsDisplayCurrentNow) {
                canvas.drawText(mStats.mLoadText, RIGHT - mPaddingRight - mStats.mLoadWidth - 1,
                        y - 1, mShadowPaint);
                canvas.drawText(mStats.mLoadText, RIGHT - mPaddingRight - mStats.mLoadWidth - 1,
                        y + 1, mShadowPaint);
                canvas.drawText(mStats.mLoadText, RIGHT - mPaddingRight - mStats.mLoadWidth + 1,
                        y - 1, mShadow2Paint);
                canvas.drawText(mStats.mLoadText, RIGHT - mPaddingRight - mStats.mLoadWidth + 1,
                        y + 1, mShadow2Paint);
                canvas.drawText(mStats.mLoadText, RIGHT - mPaddingRight - mStats.mLoadWidth,
                        y, mLoadPaint);
            }

            int N = mStats.mTextList.size();
            for (int i = 0; i < N; i++) {
                String name = mStats.mTextList.get(i);
                int nameWidth = mStats.onMeasureText(name);
                y += mFH;
                canvas.drawText(name, RIGHT - mPaddingRight - nameWidth - 1,
                        y - 1, mShadowPaint);
                canvas.drawText(name, RIGHT - mPaddingRight - nameWidth - 1,
                        y + 1, mShadowPaint);
                canvas.drawText(name, RIGHT - mPaddingRight - nameWidth + 1,
                        y - 1, mShadow2Paint);
                canvas.drawText(name, RIGHT - mPaddingRight - nameWidth + 1,
                        y + 1, mShadow2Paint);
                canvas.drawText(name, RIGHT - mPaddingRight - nameWidth, y, mLoadPaint);
            }
        }

        void updateDisplay() {
            final int NW = mStats.mTextList.size();

            int maxWidth = mStats.mLoadWidth;
            for (int i = 0; i < NW; i++) {
                String name = mStats.mTextList.get(i);
                int nameWidth = mStats.onMeasureText(name);
                if (nameWidth > maxWidth) {
                    maxWidth = nameWidth;
                }
            }

            int neededWidth = mPaddingLeft + mPaddingRight + maxWidth;
            int neededHeight = mPaddingTop + mPaddingBottom + (mFH * (1 + NW));
            if (neededWidth != mNeededWidth || neededHeight != mNeededHeight) {
                mNeededWidth = neededWidth;
                mNeededHeight = neededHeight;
                requestLayout();
            } else {
                invalidate();
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        createFloatTextView();
        // 提高process优先级
        Uri uri = Settings.System.getUriFor(ACTIVITY_SERVICE);
        System.out.print("uri:" + uri);
        Log.d(TAG, "XiaomiDeviceName:" + DeviceUtil.getXiaomiDeviceName());

        mIsDisplayCurrentNow = false;
        mIsDisplayCurrentAvg = false;
        mIsDisplayBatteryTemp = false;
        mIsDisplayCpuMode = false;
        mIsDisplayLcdMode = false;
        mIsDisplayFps = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mHaveCurrentPath) {
            return Service.START_STICKY;
        } else {
            return Service.START_NOT_STICKY;
        }
    }

    private void initButton() {
        if (mStopBtn != null) {
            mStopBtn.setVisibility(View.VISIBLE);
            mStartBtn.setVisibility(View.VISIBLE);
            mStopBtn.setEnabled(false);
            mStartBtn.setEnabled(true);
        }
    }

    private void createFloatTextView() {
        if (mFloatView != null) {
            return;
        }
        mFloatView = new LoadView(this);
        mTextLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        mTextLayoutParams.gravity = Gravity.END | Gravity.TOP;
        mWindowManager.addView(mFloatView, mTextLayoutParams);
    }

    private Runnable mInvalidateRunnable = new Runnable() {
        @Override
        public void run() {
            if (mFloatFpsView != null) {
                mFloatFpsView.invalidate();
                mFloatFpsView.post(this);
            }
        }
    };

    private void createFloatFpsView() {
        if (mFloatFpsView != null) {
            return;
        }
        mFloatFpsView = new FpsView(this);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.END | Gravity.TOP;
        mWindowManager.addView(mFloatFpsView, params);
        mFloatFpsView.post(mInvalidateRunnable);
    }

    private void createFloatBtnView() {
        if (mBtnLayout != null) {
            return;
        }
        mBtnLayoutParams = new WindowManager.LayoutParams();
        mBtnLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        mBtnLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mBtnLayoutParams.format = PixelFormat.TRANSLUCENT;
        mBtnLayoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        mBtnLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mBtnLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mBtnLayoutParams.x = 0;
        mBtnLayoutParams.y = 0;

        mBtnLayout = new RelativeLayout(this);
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mFloatDragView = new TextView(this);
        mFloatDragView.setId(TEXT_VIEW_ID);
        mFloatDragView.setTextColor(Color.parseColor("#ff0000"));
        mFloatDragView.setTextSize(16);
        mFloatDragView.setText(getResources().getString(R.string.drag));
        mBtnLayout.addView(mFloatDragView, rlp);

        rlp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        rlp.addRule(RelativeLayout.BELOW, TEXT_VIEW_ID);
        mStartBtn = new Button(this);
        mStartBtn.setId(START_BTN_ID);
        mStartBtn.setText(getString(R.string.start));
        mStartBtn.setOnClickListener(this);
        mBtnLayout.addView(mStartBtn, rlp);

        rlp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        rlp.addRule(RelativeLayout.BELOW, START_BTN_ID);
        mStopBtn = new Button(this);
        mStopBtn.setId(STOP_BTN_ID);
        mStopBtn.setText(getString(R.string.stop));
        mStopBtn.setOnClickListener(this);
        mBtnLayout.addView(mStopBtn, rlp);

        mWindowManager.addView(mBtnLayout, mBtnLayoutParams);

        mBtnLayout.setOnTouchListener(new DragListener());
        initButton();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatView != null) {
            mWindowManager.removeView(mFloatView);
            mFloatView = null;
        }

        if (mBtnLayout != null) {
            mWindowManager.removeView(mBtnLayout);
            mBtnLayout = null;
        }

        if (mLightSensor != null) {
            mSensorManager.unregisterListener(mLightSensorListener);
            mLightSensorListener = null;               
        }
        mIsDisplayCurrentNow = false;
        mIsDisplayCurrentAvg = false;
        mIsDisplayBatteryTemp = false;
        mIsDisplayCpuMode = false;
        mIsDisplayLcdMode = false;
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case START_BTN_ID:
                mStartBtn.setEnabled(false);
                mStopBtn.setEnabled(true);
                this.startService(new Intent(this, CurrentAvgService.class));
                break;
            case STOP_BTN_ID: {
                mStartBtn.setEnabled(true);
                mStopBtn.setEnabled(false);
                this.stopService(new Intent(this, CurrentAvgService.class));
                break;
            }
        }
    }

    private class DragListener implements OnTouchListener {
        @Override
        public boolean onTouch(View arg0, MotionEvent event) {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mTouchStartX = (int) event.getRawX();
                    mTouchStartY = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    mTouchCurrentX = (int) event.getRawX();
                    mTouchCurrentY = (int) event.getRawY();
                    mBtnLayoutParams.x += mTouchCurrentX - mTouchStartX;
                    mBtnLayoutParams.y += mTouchCurrentY - mTouchStartY;
                    mWindowManager.updateViewLayout(mBtnLayout, mBtnLayoutParams);

                    mTouchStartX = mTouchCurrentX;
                    mTouchStartY = mTouchCurrentY;
                    break;
                case MotionEvent.ACTION_UP:
                    break;
            }
            return false;
        }
    }
}
