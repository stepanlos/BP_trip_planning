package com.example.myapplication;

import android.content.Context;
import android.util.Log;
import android.os.Environment;

import androidx.test.core.app.ApplicationProvider;
import com.example.myapplication.data.MowingPlace;
import com.example.myapplication.data.MowingPlacesRepository;
import com.example.myapplication.util.TSPPlanner;

import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class TSPBenchmark {

    private static final int ITERATIONS = 1000;
    private static final int MAX_INTERMEDIATES = 15;

    // hranice ČR
    private static final double MIN_LAT = 48.55;
    private static final double MAX_LAT = 51.05;
    private static final double MIN_LON = 12.09;
    private static final double MAX_LON = 18.87;


    @Test
    public void benchmarkTSPPlanner() throws IOException {
        // Use ApplicationProvider for local context in androidTest
        Context context = ApplicationProvider.getApplicationContext();
        MowingPlacesRepository repo = new MowingPlacesRepository();
        List<MowingPlace> allPlaces = repo.loadMowingPlaces(context);

        // Use public Downloads directory
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs();
        }
        File outputFile = new File(downloadsDir, "tsp_benchmark_results.txt");
        FileWriter writer = new FileWriter(outputFile, false);

        Random rnd = new Random();

        // For each number of intermediate stops 1..MAX_INTERMEDIATES
        for (int n = 1; n <= MAX_INTERMEDIATES; n++) {
            double totalPercent = 0.0;
            double maxPercent = Double.NEGATIVE_INFINITY;

            for (int iter = 0; iter < ITERATIONS; iter++) {
                // Sample n distinct random places
                List<MowingPlace> sample = new ArrayList<>(allPlaces);
                Collections.shuffle(sample, rnd);
                List<MowingPlace> intermediates = sample.subList(0, n);

                // Dummy start and end nodes
                MowingPlace start = new MowingPlace();
                start.setId("start");
                start.setLatitude(randomLatitude(rnd));
                start.setLongitude(randomLongitude(rnd));
                start.setDistancesToOthers(Collections.emptyList());

                MowingPlace end = new MowingPlace();
                end.setId("end");
                end.setLatitude(randomLatitude(rnd));
                end.setLongitude(randomLongitude(rnd));
                end.setDistancesToOthers(Collections.emptyList());

                // Build node list: start + intermediates + end
                List<MowingPlace> nodes = new ArrayList<>();
                nodes.add(start);
                nodes.addAll(intermediates);
                nodes.add(end);

                // Run TSPPlanner
                List<MowingPlace> route = TSPPlanner.generateRoute(nodes);
                double routeLength = computeRouteLength(route);

                // Build full distance matrix
                double[][] dist = buildDistanceMatrix(nodes);
                double optimum = computeOptimalPathLength(dist);

                // Compute percentage degradation
                double percent = (routeLength - optimum) / optimum * 100.0;
                totalPercent += percent;
                if (percent > maxPercent) {
                    maxPercent = percent;
                }
            }
            double avgPercent = totalPercent / ITERATIONS;
            // Write results: one line per n
            writer.write(String.format("n=%d: avg=%.2f%%, max=%.2f%%%n", n, avgPercent, maxPercent));
            Log.d("TSPBenchmark", String.format("n=%d: avg=%.2f%%, max=%.2f%%%n", n, avgPercent, maxPercent));
        }
        writer.flush();
        writer.close();
    }

    private double computeRouteLength(List<MowingPlace> route) {
        double length = 0.0;
        for (int i = 0; i < route.size() - 1; i++) {
            length += getDistance(route.get(i), route.get(i + 1));
        }
        return length;
    }

    private double getDistance(MowingPlace a, MowingPlace b) {
        if (a.getDistancesToOthers() != null) {
            for (MowingPlace.DistanceEntry e : a.getDistancesToOthers()) {
                if (e.getId().equals(b.getId())) {
                    return e.getDistance();
                }
            }
        }
        return haversineDistance(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
    }

    private double[][] buildDistanceMatrix(List<MowingPlace> nodes) {
        int size = nodes.size();
        double[][] dist = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i == j) {
                    dist[i][j] = 0.0;
                } else {
                    dist[i][j] = getDistance(nodes.get(i), nodes.get(j));
                }
            }
        }
        return dist;
    }

    private double computeOptimalPathLength(double[][] dist) {
        int total = dist.length;
        int nInter = total - 2;
        if (nInter == 0) {
            return dist[0][1];
        }
        int N = 1 << nInter;
        double[][] dp = new double[N][nInter];
        for (double[] row : dp) {
            Arrays.fill(row, Double.POSITIVE_INFINITY);
        }
        // Base cases: from start to each first intermediate
        for (int i = 0; i < nInter; i++) {
            dp[1 << i][i] = dist[0][i + 1];
        }
        // DP over subsets
        for (int mask = 0; mask < N; mask++) {
            for (int u = 0; u < nInter; u++) {
                if ((mask & (1 << u)) == 0) continue;
                double prevCost = dp[mask][u];
                if (Double.isInfinite(prevCost)) continue;
                for (int v = 0; v < nInter; v++) {
                    if ((mask & (1 << v)) != 0) continue;
                    int nextMask = mask | (1 << v);
                    double cost = prevCost + dist[u + 1][v + 1];
                    if (cost < dp[nextMask][v]) {
                        dp[nextMask][v] = cost;
                    }
                }
            }
        }
        // Close path to end
        double best = Double.POSITIVE_INFINITY;
        int fullMask = N - 1;
        for (int i = 0; i < nInter; i++) {
            double cost = dp[fullMask][i] + dist[i + 1][nInter + 1];
            if (cost < best) {
                best = cost;
            }
        }
        return best;
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0;
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }




    private double randomLatitude(Random rnd) {
        return MIN_LAT + rnd.nextDouble() * (MAX_LAT - MIN_LAT);
    }

    private double randomLongitude(Random rnd) {
        return MIN_LON + rnd.nextDouble() * (MAX_LON - MIN_LON);
    }
}
