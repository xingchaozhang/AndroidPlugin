package com.enjoy.plugin;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 暂时注释，没涉及到资源
//        setContentView(R.layout.activity_main);
        Log.e("leo", "onCreate: 启动插件的Activity");

        View view = LayoutInflater.from(mContext).inflate(R.layout.activity_main, null);
        setContentView(view);
    }
}
