package com.example.myapplication;

import android.app.Application;

import com.jakewharton.threetenabp.AndroidThreeTen;

import org.osmdroid.config.Configuration;
import org.osmdroid.library.BuildConfig;

/**
 * Custom Application class for initializing global settings.
 * This class is used to set the user agent for osmdroid and initialize the Android ThreeTen library.
 */
public class MyApplication extends Application {
    /**
     * Called when the application is starting, before any activity, service, or receiver objects have been created.
     * This method is used to perform one-time initialization of the application.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        // Set user agent for osmdroid
        Configuration.getInstance().setUserAgentValue(BuildConfig.LIBRARY_PACKAGE_NAME);
        AndroidThreeTen.init(this);

    }
}
