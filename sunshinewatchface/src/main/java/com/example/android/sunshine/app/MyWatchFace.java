/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class MyWatchFace extends CanvasWatchFaceService  {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;
    MyWatchFace mMyWatchFace;

    @Override
    public void onCreate() {
        //REMOVE THIS OR IT WILL FREEZE ON STARTUP
        //android.os.Debug.waitForDebugger();

        super.onCreate();
    }

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;

        Paint mBackgroundPaint;
        Paint mTextPaint;
        Paint mTextSecondaryPaint;
        Paint mLinePaint;
        Paint mHighTempPaint;
        Paint mLowTempPaint;

        boolean mAmbient;
        Time mTime;

        float mXOffset;
        float mYOffset;

        float mDateXOffset;
        float mDateYOffset;

        float mLineLength;
        float mLineY;

        int mLowTemp;
        int mHighTemp;
        int mWeatherId;

        float mTempYOffset;
        float mLowTempXOffset;
        float mWeatherXOffset;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;
        GoogleApiClient mGoogleApiClient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());
            Resources resources = MyWatchFace.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            mDateYOffset = resources.getDimension(R.dimen.date_y_offset);

            mTempYOffset = resources.getDimension(R.dimen.tempYOffset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            mTextPaint = new Paint();
            mTextPaint = createTextPaint(resources.getColor(R.color.digital_text));

            mTextSecondaryPaint = new Paint();
            mTextSecondaryPaint = createTextPaint(resources.getColor(R.color.text_secondary));

            mLinePaint = new Paint();
            mLinePaint.setColor(resources.getColor(R.color.line_color));


            mLowTempPaint = createTextPaint(resources.getColor(R.color.text_secondary));

            mHighTempPaint= createTextPaint(resources.getColor(R.color.digital_text));

            mTime = new Time();

            mLineLength = resources.getDimension(R.dimen.line_length);
            mLineY = resources.getDimension(R.dimen.line_y);

            mLowTemp = -1;
            mHighTemp = -1;
            mWeatherId = -1;

            startService(new Intent(getBaseContext(), WatchListenerService.class));

            LocalBroadcastManager.getInstance(getBaseContext()).registerReceiver(mReceiver,
                    new IntentFilter("weather_data"));
        }

        private BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Get extra data included in the Intent
                int low = intent.getIntExtra("LowTemp", 0);
                int high = intent.getIntExtra("HighTemp", 0);
                int weatherId = intent.getIntExtra("WeatherId", 0);
                updateWeather(low, high, weatherId);
            }
        };

        //teken from app/Utility.java
        public int getIconResourceForWeatherCondition(int weatherId) {
            // Based on weather code data found at:
            // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
            if (weatherId >= 200 && weatherId <= 232) {
                return R.drawable.ic_storm;
            } else if (weatherId >= 300 && weatherId <= 321) {
                return R.drawable.ic_light_rain;
            } else if (weatherId >= 500 && weatherId <= 504) {
                return R.drawable.ic_rain;
            } else if (weatherId == 511) {
                return R.drawable.ic_snow;
            } else if (weatherId >= 520 && weatherId <= 531) {
                return R.drawable.ic_rain;
            } else if (weatherId >= 600 && weatherId <= 622) {
                return R.drawable.ic_snow;
            } else if (weatherId >= 701 && weatherId <= 761) {
                return R.drawable.ic_fog;
            } else if (weatherId == 761 || weatherId == 781) {
                return R.drawable.ic_storm;
            } else if (weatherId == 800) {
                return R.drawable.ic_clear;
            } else if (weatherId == 801) {
                return R.drawable.ic_light_clouds;
            } else if (weatherId >= 802 && weatherId <= 804) {
                return R.drawable.ic_cloudy;
            }
            return -1;
        }

        private void updateWeather(int low, int high, int weather) {
            mLowTemp = low;
            mHighTemp = high;
            mWeatherId = weather;
            invalidate();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = MyWatchFace.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);

            mDateXOffset = resources.getDimension(isRound
                    ? R.dimen.date_x_offset_round : R.dimen.date_x_offset);

            mLowTempXOffset = resources.getDimension(isRound
                    ? R.dimen.low_temp_x_offset_round : R.dimen.low_temp_x_offset);

            mWeatherXOffset = resources.getDimension(isRound
                    ? R.dimen.weather_x_offset_round : R.dimen.weather_x_offset);

            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            float secondaryTextSize = resources.getDimension(isRound
                    ? R.dimen.secondary_text_size_round : R.dimen.secondary_text_size);

            float tempTextSize = resources.getDimension(isRound
                    ? R.dimen.weather_text_size_round : R.dimen.weather_text_size);

            mTextPaint.setTextSize(textSize);
            mTextSecondaryPaint.setTextSize(secondaryTextSize);

            mLowTempPaint.setTextSize(tempTextSize);
            mHighTempPaint.setTextSize(tempTextSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            float halfCanvasWidth = canvas.getWidth()/2;

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            mTime.setToNow();

            String text = String.format("%d:%02d", mTime.hour, mTime.minute);
            float timeX = (canvas.getWidth() / 2) - (mTextPaint.measureText(text)/2);
            canvas.drawText(text, timeX, mYOffset, mTextPaint);

            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd yyyy");
            String date = dateFormat.format(new Date());

            float dateX = halfCanvasWidth - (mTextSecondaryPaint.measureText(date)/2);
            canvas.drawText(date, dateX, mDateYOffset, mTextSecondaryPaint);

            canvas.drawLine((halfCanvasWidth - (mLineLength / 2)), mLineY, (halfCanvasWidth + mLineLength / 2), mLineY, mLinePaint);

            String highTemp = String.valueOf(mHighTemp) + '\u00B0';
            float highTempX = halfCanvasWidth - (mHighTempPaint.measureText(highTemp)/2);
            String lowTemp = String.valueOf(mLowTemp) + '\u00B0';

            canvas.drawText(highTemp,highTempX, mTempYOffset, mHighTempPaint);
            canvas.drawText(lowTemp, mLowTempXOffset, mTempYOffset, mLowTempPaint);

            if (mWeatherId > 0) {
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), getIconResourceForWeatherCondition(mWeatherId));
                canvas.drawBitmap(bitmap, mWeatherXOffset, mTempYOffset - (bitmap.getHeight()/2), null);
            }
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MyWatchFace.Engine> mWeakReference;

        public EngineHandler(MyWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MyWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }
}
