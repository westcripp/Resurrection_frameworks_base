/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.policy;

import java.util.ArrayList;

import android.bluetooth.BluetoothAdapter.BluetoothStateChangeCallback;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff;
import android.os.BatteryManager;
import android.os.Handler;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.CharacterStyle;
import android.util.AttributeSet;
import android.util.ColorUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.systemui.R;

public class SbBatteryController extends LinearLayout {
    private static final String TAG = "StatusBar.BatteryController";

    private Context mContext;
    private ArrayList<ImageView> mIconViews = new ArrayList<ImageView>();
    private ArrayList<TextView> mLabelViews = new ArrayList<TextView>();

    private ArrayList<BatteryStateChangeCallback> mChangeCallbacks =
            new ArrayList<BatteryStateChangeCallback>();

    private int mLevel;
    private boolean mPlugged = false;
    private ColorUtils.ColorSettingInfo mColorInfo;

    public interface BatteryStateChangeCallback {
        public void onBatteryLevelChanged(int level, boolean pluggedIn);
    }

    private ImageView mBatteryIcon;
    private TextView mBatteryText;
    private TextView mBatteryCenterText;
    private ViewGroup mBatteryGroup;
    private TextView mBatteryTextOnly;
    private TextView mBatteryTextOnly_Low;
    private TextView mBatteryTextOnly_Plugged;

    private static int mBatteryStyle;

    public static final int STYLE_ICON_ONLY = 0;
    public static final int STYLE_TEXT_ONLY = 1;
    public static final int STYLE_ICON_TEXT = 2;
    public static final int STYLE_ICON_CENTERED_TEXT = 3;
    public static final int STYLE_ICON_CIRCLE = 4;
    public static final int STYLE_HIDE = 5;

    public SbBatteryController(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        mColorInfo = ColorUtils.getColorSettingInfo(context, Settings.System.STATUS_ICON_COLOR);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        init();
        mBatteryGroup = (ViewGroup) findViewById(R.id.battery_combo);
        mBatteryIcon = (ImageView) findViewById(R.id.battery);
        mBatteryText = (TextView) findViewById(R.id.battery_text);
        mBatteryCenterText = (TextView) findViewById(R.id.battery_text_center);
        mBatteryTextOnly = (TextView) findViewById(R.id.battery_text_only);
        mBatteryTextOnly_Low = (TextView) findViewById(R.id.battery_text_only_low);
        mBatteryTextOnly_Plugged = (TextView) findViewById(R.id.battery_text_only_plugged);
        addIconView(mBatteryIcon);

        SettingsObserver settingsObserver = new SettingsObserver(new Handler());
        settingsObserver.observe();
        updateSettings(); // to initialize values
    }

    private void init() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        mContext.registerReceiver(mBatteryBroadcastReceiver, filter);
    }

    public void addIconView(ImageView v) {
        mIconViews.add(v);
    }

    public void addLabelView(TextView v) {
        mLabelViews.add(v);
    }

    public void addStateChangedCallback(BatteryStateChangeCallback cb) {
        mChangeCallbacks.add(cb);
    }

    public void setColor(ColorUtils.ColorSettingInfo colorInfo) {
        mColorInfo = colorInfo;
        updateBatteryLevel();
    }


    private BroadcastReceiver mBatteryBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                mLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                mPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
                updateBatteryLevel();
            }
        }
    };

    private void updateBatteryLevel() {
        int N = mIconViews.size();
        for (int i=0; i<N; i++) {
            ImageView v = mIconViews.get(i);
            v.setImageLevel(mLevel);
            v.setContentDescription(mContext.getString(R.string.accessibility_battery_level,
                    mLevel));
        }
        N = mLabelViews.size();
        for (int i=0; i<N; i++) {
            TextView v = mLabelViews.get(i);
            v.setText(mContext.getString(R.string.status_bar_settings_battery_meter_format,
                    mLevel));
        }

        for (BatteryStateChangeCallback cb : mChangeCallbacks) {
            cb.onBatteryLevelChanged(mLevel, mPlugged);
        }
        updateBattery();
    }

    private void updateBattery() {
        ContentResolver cr = mContext.getContentResolver();
        mBatteryStyle = Settings.System.getInt(cr,
                Settings.System.STATUSBAR_BATTERY_ICON, 0);
        int mIcon = R.drawable.stat_sys_battery;

        if (mBatteryStyle == STYLE_ICON_CIRCLE) {
            mIcon = mPlugged ? R.drawable.stat_sys_battery_charge_circle
                    : R.drawable.stat_sys_battery_circle;
        } else {
            mIcon = mPlugged ? R.drawable.stat_sys_battery_charge
                    : R.drawable.stat_sys_battery;
        }
        int N = mIconViews.size();
        for (int i = 0; i < N; i++) {
            ImageView v = mIconViews.get(i);
            Drawable batteryBitmap = mContext.getResources().getDrawable(mIcon);
            if (mColorInfo.isLastColorNull) {
                batteryBitmap.clearColorFilter();                
            } else {
                batteryBitmap.setColorFilter(mColorInfo.lastColor, PorterDuff.Mode.SRC_IN);
            }
            v.setImageDrawable(batteryBitmap);
        }
        N = mLabelViews.size();
        for (int i = 0; i < N; i++) {
            TextView v = mLabelViews.get(i);
        }

        // do my stuff here
        if (mBatteryGroup != null) {
            mBatteryText.setText(Integer.toString(mLevel));
            mBatteryCenterText.setText(Integer.toString(mLevel));
            SpannableStringBuilder formatted = new SpannableStringBuilder(
                    Integer.toString(mLevel) + "%");
            CharacterStyle style = new RelativeSizeSpan(0.7f); // beautiful
                                                               // formatting
            if (mLevel < 10) { // level < 10, 2nd char is %
                formatted.setSpan(style, 1, 2,
                        Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            } else if (mLevel < 100) { // level 10-99, 3rd char is %
                formatted.setSpan(style, 2, 3,
                        Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            } else { // level 100, 4th char is %
                formatted.setSpan(style, 3, 4,
                        Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            }
            mBatteryTextOnly.setText(formatted);
            mBatteryTextOnly_Low.setText(formatted);
            mBatteryTextOnly_Plugged.setText(formatted);
            if (mBatteryStyle == STYLE_TEXT_ONLY) {
                if (mPlugged) {
                    mBatteryTextOnly.setVisibility(View.GONE);
                    mBatteryTextOnly_Plugged.setVisibility(View.VISIBLE);
                    mBatteryTextOnly_Low.setVisibility(View.GONE);
                } else if (mLevel < 16) {
                    mBatteryTextOnly.setVisibility(View.GONE);
                    mBatteryTextOnly_Plugged.setVisibility(View.GONE);
                    mBatteryTextOnly_Low.setVisibility(View.VISIBLE);
                } else {
                    mBatteryTextOnly.setVisibility(View.VISIBLE);
                    mBatteryTextOnly_Plugged.setVisibility(View.GONE);
                    mBatteryTextOnly_Low.setVisibility(View.GONE);
                }
            } else {
                mBatteryTextOnly.setVisibility(View.GONE);
                mBatteryTextOnly_Plugged.setVisibility(View.GONE);
                mBatteryTextOnly_Low.setVisibility(View.GONE);
            }
        }
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUSBAR_BATTERY_ICON), false,
                    this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    private void updateSettings() {
        // Slog.i(TAG, "updated settings values");
        ContentResolver cr = mContext.getContentResolver();
        mBatteryStyle = Settings.System.getInt(cr,
                Settings.System.STATUSBAR_BATTERY_ICON, 0);

        switch (mBatteryStyle) {
            case STYLE_ICON_ONLY:
                mBatteryCenterText.setVisibility(View.GONE);
                mBatteryText.setVisibility(View.GONE);
                mBatteryIcon.setVisibility(View.VISIBLE);
                setVisibility(View.VISIBLE);
                break;
            case STYLE_TEXT_ONLY:
                mBatteryText.setVisibility(View.GONE);
                mBatteryCenterText.setVisibility(View.GONE);
                mBatteryIcon.setVisibility(View.GONE);
                setVisibility(View.VISIBLE);
                break;
            case STYLE_ICON_TEXT:
                mBatteryText.setVisibility(View.VISIBLE);
                mBatteryCenterText.setVisibility(View.GONE);
                mBatteryIcon.setVisibility(View.VISIBLE);
                setVisibility(View.VISIBLE);
                break;
            case STYLE_ICON_CENTERED_TEXT:
                mBatteryText.setVisibility(View.GONE);
                mBatteryCenterText.setVisibility(View.VISIBLE);
                mBatteryIcon.setVisibility(View.VISIBLE);
                setVisibility(View.VISIBLE);
                break;
            case STYLE_HIDE:
                mBatteryText.setVisibility(View.GONE);
                mBatteryCenterText.setVisibility(View.GONE);
                mBatteryIcon.setVisibility(View.GONE);
                setVisibility(View.GONE);
                break;
            case STYLE_ICON_CIRCLE:
                mBatteryText.setVisibility(View.GONE);
                mBatteryCenterText.setVisibility(View.GONE);
                mBatteryIcon.setVisibility(View.VISIBLE);
                setVisibility(View.VISIBLE);
                break;
            default:
                mBatteryText.setVisibility(View.GONE);
                mBatteryCenterText.setVisibility(View.GONE);
                mBatteryIcon.setVisibility(View.VISIBLE);
                setVisibility(View.VISIBLE);
                break;
        }

        updateBattery();
    }
}
