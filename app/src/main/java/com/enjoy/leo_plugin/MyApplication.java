package com.enjoy.leo_plugin;

import android.app.Application;
import android.content.res.Resources;

public class MyApplication extends Application {

//    private Resources mResources;

    @Override
    public void onCreate() {
        super.onCreate();

        LoadUtil.loadClass(this);

//        mResources = LoadUtil.loadResource(this);

        HookUtil.hookAMS();
        HookUtil.hookHandler();
    }

//    @Override
//    public Resources getResources() {
//        return mResources == null ? super.getResources() : mResources;
//    }
}
