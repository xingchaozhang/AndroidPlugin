package com.enjoy.leo_plugin;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.MultiTapKeyListener;
import android.util.Log;
import android.view.View;

import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 打印类加载器
                printClassLoader();

//                PathClassLoader dexClassLoader = new PathClassLoader("/sdcard/test.dex",
//                        null);
//                try {
//                    Class<?> clazz = dexClassLoader.loadClass("com.enjoy.plugin.Test");
//                    Method print = clazz.getMethod("print");
//                    print.invoke(null);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }

                try {
                    Class<?> clazz = Class.forName("com.enjoy.plugin.Test");
                    Method print = clazz.getMethod("print");
                    print.invoke(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // 找到一个容易替换Intent的地方
//                startActivity(new Intent(MainActivity.this,ProxyActivity.class));
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.enjoy.plugin",
                        "com.enjoy.plugin.MainActivity"));
                startActivity(intent);
            }
        });
    }

    private void printClassLoader() {
        ClassLoader classLoader = getClassLoader();
        while (classLoader != null) {
            Log.e("leo", "classLoader: " + classLoader);
            classLoader = classLoader.getParent();
        }

        // pathClassLoader  和  BootClassLoader  分别加载什么类
        Log.e("leo", "Activity的classLoader: " + Activity.class.getClassLoader());
        // 1  Path   2 boot
        Log.e("leo", "Activity的classLoader: " + AppCompatActivity.class.getClassLoader());

    }
}