package com.example.myapplication.data;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class MowingPlacesRepository {

    private static final String TAG = "MowingPlacesRepository";
    private static final String JSON_FILE_NAME = "mowing_places.json";

    public List<MowingPlace> loadMowingPlaces(Context context) {
        try {
            InputStream is = context.getAssets().open(JSON_FILE_NAME);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonString = new String(buffer, "UTF-8");

            Gson gson = new Gson();
            Type listType = new TypeToken<List<MowingPlace>>(){}.getType();
            return gson.fromJson(jsonString, listType);
        } catch (IOException e) {
            Log.e(TAG, "Error reading JSON file", e);
            return Collections.emptyList();
        }
    }

    // TODO: Implement saveMowingPlaces(...) if you want to write back to JSON

    public void saveMowingPlaces(Context context, List<MowingPlace> mowingPlaces) {
        Gson gson = new Gson();
        String jsonString = gson.toJson(mowingPlaces);

        try {
            context.getAssets().open(JSON_FILE_NAME);
        } catch (IOException e) {
            Log.e(TAG, "Error writing JSON file", e);
        }


    }
}
