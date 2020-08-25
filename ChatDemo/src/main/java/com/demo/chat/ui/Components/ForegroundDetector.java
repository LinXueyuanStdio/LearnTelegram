package com.demo.chat.ui.Components;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;

import com.demo.chat.messager.BuildVars;
import com.demo.chat.messager.FileLog;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 * 监听应用的生命周期
 */
public class ForegroundDetector implements Application.ActivityLifecycleCallbacks {

    public interface Listener {
        void onBecameForeground();
        void onBecameBackground();
    }

    private int refs;
    private boolean wasInBackground = true;
    private long enterBackgroundTime = 0;
    private CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private static ForegroundDetector Instance = null;

    public static ForegroundDetector getInstance() {
        return Instance;
    }

    public ForegroundDetector(Application application) {
        Instance = this;
        application.registerActivityLifecycleCallbacks(this);
    }

    public boolean isForeground() {
        return refs > 0;
    }

    public boolean isBackground() {
        return refs == 0;
    }

    /**
     * 观察者模式
     * 这个函数没有使用，这是挖了个坑
     * @param listener
     */
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (++refs == 1) {
            if (SystemClock.elapsedRealtime() - enterBackgroundTime < 200) {
                wasInBackground = false;
            }
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("switch to foreground");
            }
            for (Listener listener : listeners) {
                try {
                    listener.onBecameForeground();
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        }
    }

    public boolean isWasInBackground(boolean reset) {
        if (reset && Build.VERSION.SDK_INT >= 21 && (SystemClock.elapsedRealtime() - enterBackgroundTime < 200)) {
            wasInBackground = false;
        }
        return wasInBackground;
    }

    public void resetBackgroundVar() {
        wasInBackground = false;
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (--refs == 0) {
            enterBackgroundTime = SystemClock.elapsedRealtime();
            wasInBackground = true;
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("switch to background");
            }
            for (Listener listener : listeners) {
                try {
                    listener.onBecameBackground();
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }
}

