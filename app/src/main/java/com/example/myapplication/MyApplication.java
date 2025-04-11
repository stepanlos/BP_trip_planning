package com.example.myapplication;

import android.app.Application;

import com.jakewharton.threetenabp.AndroidThreeTen;

import org.osmdroid.config.Configuration;
import org.osmdroid.library.BuildConfig;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Set user agent for osmdroid
        Configuration.getInstance().setUserAgentValue(BuildConfig.LIBRARY_PACKAGE_NAME);
        AndroidThreeTen.init(this);

    }
}
