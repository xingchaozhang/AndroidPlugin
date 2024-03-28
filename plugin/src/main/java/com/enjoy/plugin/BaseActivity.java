package com.enjoy.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.ContextThemeWrapper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Field;

public class BaseActivity extends AppCompatActivity {

    protected Context mContext;

//    @Override
//    public Resources getResources() {
//        if (getApplication() != null && getApplication().getResources() != null) {
//            return getApplication().getResources();
//        }
//        return super.getResources();
//
//        Resources resources = LoadUtil.getResources(getApplication()/*这里传入Activity会导致栈溢出崩溃*/);
//        // 如果插件作为一个单独的app，返回 super.getResources()
//        return resources == null ? super.getResources() : resources;
//    }

    // 该方式可以完全解决插件影响宿主的问题。
    // decor_content_parent这个id有在androidx.appcompat.app.AppCompatDelegateImpl#createSubDecor使用，
    // 如果这个id在宿主和插件中不相同，就有可能会出现资源找不到的错误，因为无论是宿主还是插件，
    // 用到的AppCompatDelegateImpl是同一个，这个是有双亲委派机制以及PathClassLoader中的Element数组决定的。
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Resources resources = LoadUtil.getResources(getApplication());
        // 使用新的context就是为解决注释中的问题。
        mContext = new ContextThemeWrapper(getBaseContext(), 0);

        Class<? extends Context> clazz = mContext.getClass();
        try {
            Field mResourcesField = clazz.getDeclaredField("mResources");
            mResourcesField.setAccessible(true);
            mResourcesField.set(mContext, resources);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
