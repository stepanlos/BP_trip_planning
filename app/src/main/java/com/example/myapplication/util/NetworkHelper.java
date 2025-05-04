package com.example.myapplication.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Utility class for checking network connectivity.
 * This class provides methods to determine if the device is connected to the internet.
 */
public class NetworkHelper {
    /**
     * Checks if the device is connected to the internet.
     * This method uses the ConnectivityManager to check the network status.
     *
     * @param context The context of the calling activity or application.
     * @return true if the device is connected to the internet, false otherwise.
     */
    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        }
        return false;
    }
}
