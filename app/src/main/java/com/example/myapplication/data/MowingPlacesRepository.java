package com.example.myapplication.data;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Repository class for managing MowingPlace data.
 * This class handles loading and saving mowing places from/to JSON files.
 */
public class MowingPlacesRepository {


    private static final String TAG = "MowingPlacesRepository";
    private static final String JSON_FILE_NAME = "mowing_places.json";

    /**
     * Loads a list of MowingPlace objects from a JSON file.
     * The method first checks internal storage; if the file is not found, it loads from assets.
     *
     * @param context The application context used to access files and assets.
     * @return A list of MowingPlace objects, or an empty list if an error occurs.
     */
    public List<MowingPlace> loadMowingPlaces(Context context) {
        try {
            File file = new File(context.getFilesDir(), JSON_FILE_NAME);
            InputStream is;
            if (file.exists()) {
                // Load from internal storage
                is = new FileInputStream(file);
                Log.d(TAG, "Loading JSON from internal storage");
            } else {
                // Load from assets if file does not exist in internal storage
                is = context.getAssets().open(JSON_FILE_NAME);
                Log.d(TAG, "Loading JSON from assets");
            }
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonString = new String(buffer, StandardCharsets.UTF_8);

            Gson gson = new Gson();
            Type listType = new TypeToken<List<MowingPlace>>() {}.getType();

            return gson.fromJson(jsonString, listType);
        } catch (IOException e) {
            Log.e(TAG, "Error reading JSON file", e);
            return Collections.emptyList();
        }
    }

    /**
     * Saves a list of MowingPlace objects to a JSON file.
     * The method saves the file in internal storage.
     *
     * @param context The application context used to access files.
     * @param places  The list of MowingPlace objects to save.
     * @return true if the save operation was successful, false otherwise.
     */
    public boolean saveMowingPlaces(Context context, List<MowingPlace> places) {
        try {
            Gson gson = new Gson();
            String jsonString = gson.toJson(places);
            //log the JSON string
            Log.d(TAG, "Saving JSON: \n" + jsonString);
            File file = new File(context.getFilesDir(), JSON_FILE_NAME);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(jsonString.getBytes(StandardCharsets.UTF_8));
            fos.close();
            Log.d(TAG, "Mowing places saved to " + file.getAbsolutePath());
            loadMowingPlaces(context); // Reload the data after saving
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error saving JSON file", e);
            return false;
        }
    }

    /**
     * Generates the next ID for a new MowingPlace.
     * The method checks the existing IDs and returns the next available ID.
     *
     * @param context The application context used to access files.
     * @return The next available ID as an integer.
     */
    public int getNextId(Context context) {
        // Reload places (or use cached highestId if available)
        List<MowingPlace> places = loadMowingPlaces(context);
        int maxId = 0;
        for (MowingPlace place : places) {
            int idInt = 0;
            try {
                idInt = Integer.parseInt(place.getId());
            } catch (NumberFormatException e) {
                // Ignore non-integer IDs
            }
            if (idInt > maxId) {
                maxId = idInt;
            }
        }
        return maxId + 1;
    }

}
