package com.guoj.crashmanager;

import android.app.Application;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CrashManager.getInstance(this).init();
    }
}
