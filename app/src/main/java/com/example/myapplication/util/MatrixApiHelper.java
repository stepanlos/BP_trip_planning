package com.example.myapplication.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.myapplication.data.MowingPlace;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MatrixApiHelper {

    private static final String TAG = "MatrixApiHelper";
    private static final String API_MATRIX_PLANNING = "https://api.mapy.cz/v1/routing/matrix-m";
    private static final String API_KEY = "tTgH_bTEkT-C_FAUZ_PF98K1pdGdpLT-OXOxtR9pMFU";
    private static final int MAX_BATCH_SIZE = 100;
    private static final int MAX_CALLS = 10;

    public interface MatrixApiCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    /**
     * Updates distances for a new place against all other places.
     * This method will call the API in batches of MAX_BATCH_SIZE.
     * It will also limit the number of API calls to MAX_CALLS.
     *
     * @param context the context of the calling activity
     * @param newPlace the new place to update distances for
     * @param allPlaces the list of all places to compare against
     * @param callback the callback to handle success or failure
     */
    public static void updateDistances(Context context, MowingPlace newPlace, List<MowingPlace> allPlaces, MatrixApiCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            try {
                List<MowingPlace> destinationPlaces = new ArrayList<>();
                for (MowingPlace place : allPlaces) {
                    if (!place.getId().equals(newPlace.getId())) {
                        destinationPlaces.add(place);
                    }
                }

                int totalDestinations = destinationPlaces.size();
                int callsMade = 0;
                List<MowingPlace.DistanceEntry> newPlaceDistances = new ArrayList<>();

                for (int start = 0; start < totalDestinations; start += MAX_BATCH_SIZE) {
                    if (callsMade >= MAX_CALLS) {
                        mainHandler.post(() -> callback.onFailure("Maximum number of API calls reached."));
                        return;
                    }
                    int end = Math.min(start + MAX_BATCH_SIZE, totalDestinations);
                    List<MowingPlace> batch = destinationPlaces.subList(start, end);
                    List<String> ends = new ArrayList<>();
                    for (MowingPlace dest : batch) {
                        ends.add(dest.getLongitude() + "," + dest.getLatitude());
                    }

                    String startParam = URLEncoder.encode(newPlace.getLongitude() + "," + newPlace.getLatitude(), "UTF-8");
                    StringBuilder endsParamBuilder = new StringBuilder();
                    for (String s : ends) {
                        if (endsParamBuilder.length() > 0) {
                            endsParamBuilder.append(";");
                        }
                        endsParamBuilder.append(URLEncoder.encode(s, "UTF-8"));
                    }
                    String endsParam = endsParamBuilder.toString();

                    String urlString = API_MATRIX_PLANNING + "?apikey=" + URLEncoder.encode(API_KEY, "UTF-8")
                            + "&routeType=" + URLEncoder.encode("car_fast", "UTF-8")
                            + "&lang=" + URLEncoder.encode("cs", "UTF-8")
                            + "&starts=" + startParam
                            + "&ends=" + endsParam;
                    Log.d(TAG, "URL: " + urlString);

                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }
                        in.close();

                        Gson gson = new Gson();
                        JsonObject jsonResponse = gson.fromJson(response.toString(), JsonObject.class);
                        JsonArray matrixArray = jsonResponse.getAsJsonArray("matrix");
                        if (matrixArray != null && !matrixArray.isEmpty()) {
                            JsonArray results = matrixArray.get(0).getAsJsonArray();
                            for (int i = 0; i < results.size(); i++) {
                                JsonObject resObj = results.get(i).getAsJsonObject();
                                int distance = -1;
                                if (resObj.has("length") && resObj.get("length").getAsInt() >= 0) {
                                    distance = resObj.get("length").getAsInt();
                                }
                                int duration = -1;
                                if (resObj.has("duration") && resObj.get("duration").getAsInt() >= 0) {
                                    duration = resObj.get("duration").getAsInt();
                                    Log.d(TAG, "Duration: " + duration + " seconds");
                                }

                                // update the distances for the new place
                                MowingPlace.DistanceEntry entry = new MowingPlace.DistanceEntry();
                                entry.setId(batch.get(i).getId());
                                entry.setDistance(distance);
                                entry.setDuration(duration);
                                newPlaceDistances.add(entry);

                                // update the distances for the batch
                                if (batch.get(i).getDistancesToOthers() == null) {
                                    batch.get(i).setDistancesToOthers(new ArrayList<>());
                                }
                                MowingPlace.DistanceEntry reverseEntry = new MowingPlace.DistanceEntry();
                                reverseEntry.setId(newPlace.getId());
                                reverseEntry.setDistance(distance);
                                reverseEntry.setDuration(duration);
                                batch.get(i).getDistancesToOthers().add(reverseEntry);
                            }
                        } else {
                            mainHandler.post(() -> callback.onFailure("Žádné výsledky v odpovědi API."));
                            return;
                        }
                    } else {
                        mainHandler.post(() -> callback.onFailure("Chyba při volání API: " + responseCode));
                        return;
                    }
                    callsMade++;
                    Thread.sleep(100);
                }
                newPlace.setDistancesToOthers(newPlaceDistances);
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                Log.e(TAG, "Chyba při volání API: ", e);
                mainHandler.post(() -> callback.onFailure(e.getMessage()));
            }
        }).start();
    }
}
