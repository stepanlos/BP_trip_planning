package com.example.myapplication.data;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Repository for saving and loading route plans.
 */
public class RoutePlanRepository {

    private static final String TAG = "RoutePlanRepository";
    private static final String JSON_FILE_NAME = "route_plans.json";

    /**
     * Loads route plans from a JSON file.
     * If the file is not found in internal storage, it loads from assets.
     *
     * @param context The application context used to access files and assets.
     * @return A list of RoutePlan objects, or an empty list if an error occurs.
     */
    public List<RoutePlan> loadRoutePlans(Context context) {
        try {
            File file = new File(context.getFilesDir(), JSON_FILE_NAME);
            InputStream is;
            if (file.exists() && file.length() > 0) {
                // Load from internal storage if the file exists and is not empty
                is = new FileInputStream(file);
                Log.d(TAG, "Loading route plans from internal storage");
            } else {
                // Otherwise, load from assets
                is = context.getAssets().open(JSON_FILE_NAME);
                Log.d(TAG, "Loading route plans from assets");
            }
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonString = new String(buffer, StandardCharsets.UTF_8);
            Gson gson = new Gson();
            Type listType = new TypeToken<List<RoutePlan>>() {}.getType();
            return gson.fromJson(jsonString, listType);
        } catch (Exception e) {
            Log.e(TAG, "Error loading route plans", e);
            return Collections.emptyList();
        }
    }

    /**
     * Saves route plans to a JSON file in internal storage.
     *
     * @param context The application context used to access files.
     * @param plans   The list of RoutePlan objects to save.
     * @return true if the save operation was successful, false otherwise.
     */
    public boolean saveRoutePlans(Context context, List<RoutePlan> plans) {
        try {
            Gson gson = new Gson();
            String jsonString = gson.toJson(plans);
            File file = new File(context.getFilesDir(), JSON_FILE_NAME);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(jsonString.getBytes(StandardCharsets.UTF_8));
            fos.close();
            Log.d(TAG, "Route plans saved.");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving route plans", e);
            return false;
        }
    }
}
