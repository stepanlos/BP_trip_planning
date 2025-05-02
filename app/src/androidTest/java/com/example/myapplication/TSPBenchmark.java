package com.example.myapplication;

import android.content.Context;
import android.os.Environment;

import androidx.test.core.app.ApplicationProvider;

import com.example.myapplication.data.MowingPlace;
import com.example.myapplication.util.TSPPlanner;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class TSPBenchmark {
    // Toggle for choosing optimal path computation method
    // true => use Held-Karp dynamic programming; false => use brute-force permutation
    private static boolean USE_HELD_KARP = true;
    private static final int RUNS_PER_N = 10000;
    private static final int MAX_N = 10;

    // Bounding box for Czech Republic (approximate lat/lon ranges)
    private static final double MIN_LAT = 48.5;
    private static final double MAX_LAT = 51.2;
    private static final double MIN_LON = 12.0;
    private static final double MAX_LON = 18.9;



    /**
     * Runs the TSP benchmark tests for n  intermediate nodes.
     * @throws IOException if the dataset file cannot be read or the result file cannot be written.
     */
    @Test
    public void runBenchmark() throws IOException {
        Context context = ApplicationProvider.getApplicationContext();
        // Load the dataset of MowingPlace nodes from mowing_places.json
        InputStream is = context.getAssets().open("mowing_places.json");
        Reader reader = new BufferedReader(new InputStreamReader(is));
        Type listType = new TypeToken<List<MowingPlace>>(){}.getType();
        List<MowingPlace> allPlaces = new Gson().fromJson(reader, listType);
        reader.close();

        Random rand = new Random();
        // Prepare output file in directory
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File outFile = new File(downloadDir, "tsp_benchmark_results.txt");
        PrintWriter out = new PrintWriter(new FileWriter(outFile));

        // Loop over n from 1 to 10 intermediate nodes
        for (int n = 1; n <= MAX_N; n++) {
            double totalPercent = 0.0;
            double maxPercent = Double.NEGATIVE_INFINITY;
            double minPercent = Double.POSITIVE_INFINITY;
            // Repeat the test RUNS_PER_N times for statistical reliability
            for (int t = 0; t < RUNS_PER_N; t++) {
                // Randomly generate start and end points within aprox Czech Republic bounds
                double startLat = MIN_LAT + rand.nextDouble() * (MAX_LAT - MIN_LAT);
                double startLon = MIN_LON + rand.nextDouble() * (MAX_LON - MIN_LON);
                double endLat = MIN_LAT + rand.nextDouble() * (MAX_LAT - MIN_LAT);
                double endLon = MIN_LON + rand.nextDouble() * (MAX_LON - MIN_LON);
                MowingPlace start = new MowingPlace();
                start.setId("start");
                start.setName("Start");
                start.setLatitude(startLat);
                start.setLongitude(startLon);
                start.setDistancesToOthers(null);  // no predefined distances
                MowingPlace end = new MowingPlace();
                end.setId("end");
                end.setName("End");
                end.setLatitude(endLat);
                end.setLongitude(endLon);
                end.setDistancesToOthers(null);

                // Randomly sample n distinct intermediate nodes from the dataset
                List<MowingPlace> intermediateNodes = new ArrayList<>();
                HashSet<Integer> usedIndices = new HashSet<>();
                while (intermediateNodes.size() < n) {
                    int idx = rand.nextInt(allPlaces.size());
                    if (!usedIndices.contains(idx)) {
                        usedIndices.add(idx);
                        // We can reuse the MowingPlace object from allPlaces since we do not modify it
                        intermediateNodes.add(allPlaces.get(idx));
                    }
                }

                // Construct the list of nodes: start + intermediates + end
                List<MowingPlace> nodes = new ArrayList<>();
                nodes.add(start);
                nodes.addAll(intermediateNodes);
                nodes.add(end);



                // Build the distance matrix replicating TSPPlanner’s logic (including DistanceEntry usage and Haversine fallback)
                int interCount = intermediateNodes.size();  // this equals n
                int totalNodes = interCount + 2;
                // Map from node ID to matrix index
                Map<String, Integer> indexById = new HashMap<>();
                // Assign indices 0..(n-1) to intermediate nodes
                for (int i = 0; i < interCount; i++) {
                    indexById.put(intermediateNodes.get(i).getId(), i);
                }
                // Assign index n to start, n+1 to end
                int startIndex = interCount;
                int endIndex = interCount + 1;
                indexById.put(start.getId(), startIndex);
                indexById.put(end.getId(), endIndex);
                // Prepare an index->node lookup for distance calculations
                List<MowingPlace> nodeByIndex = new ArrayList<>(Collections.nCopies(totalNodes, null));
                for (int i = 0; i < interCount; i++) {
                    nodeByIndex.set(i, intermediateNodes.get(i));
                }
                nodeByIndex.set(startIndex, start);
                nodeByIndex.set(endIndex, end);

                // Initialize distance matrix
                double INF = Double.POSITIVE_INFINITY;
                double[][] dist = new double[totalNodes][totalNodes];
                for (int i = 0; i < totalNodes; i++) {
                    for (int j = 0; j < totalNodes; j++) {
                        if (i == j) {
                            dist[i][j] = 0.0;
                        } else {
                            dist[i][j] = INF;
                        }
                    }
                }
                // Fill in known distances from DistanceEntry lists
                for (int i = 0; i < totalNodes; i++) {
                    MowingPlace p = nodeByIndex.get(i);
                    List<MowingPlace.DistanceEntry> distancesList = p.getDistancesToOthers();
                    if (distancesList != null) {
                        for (MowingPlace.DistanceEntry entry : distancesList) {
                            Integer j = indexById.get(entry.getId());
                            if (j != null) {
                                dist[i][j] = entry.getDistance();
                            }
                        }
                    }
                }
                // Enforce symmetry and use Haversine for missing distances
                for (int i = 0; i < totalNodes; i++) {
                    for (int j = i + 1; j < totalNodes; j++) {
                        if (dist[i][j] == INF && dist[j][i] == INF) {
                            // Both directions missing: use Haversine formula
                            double d = haversineDistance(nodeByIndex.get(i), nodeByIndex.get(j));
                            dist[i][j] = d;
                            dist[j][i] = d;
                        } else if (dist[i][j] == INF) {
                            // One direction known, make symmetric
                            dist[i][j] = dist[j][i];
                        } else if (dist[j][i] == INF) {
                            dist[j][i] = dist[i][j];
                        } else {
                            // Both directions have values (possibly different); use the smaller to ensure symmetry
                            double d = (dist[i][j] < dist[j][i] ? dist[i][j] : dist[j][i]);
                            dist[i][j] = d;
                            dist[j][i] = d;
                        }
                    }
                }

                int total = nodes.size();
                // 2f) Metric closure via Floyd–Warshall
                for (int k = 0; k < total; k++) {
                    for (int i = 0; i < total; i++) {
                        for (int j = 0; j < total; j++) {
                            double via = dist[i][k] + dist[k][j];
                            if (via < dist[i][j]) {
                                dist[i][j] = via;
                            }
                        }
                    }
                }

                // Use TSPPlanner to generate an approximate route through these nodes
                List<MowingPlace> route = TSPPlanner.generateRoute(nodes);


                // Compute the optimal path length between start and end (visiting all intermediates)
                double optimalLength;
                if (USE_HELD_KARP) {
                    optimalLength = heldKarpOptimalDistance(dist, interCount, startIndex, endIndex);
                } else {
                    optimalLength = bruteForceOptimalDistance(dist, interCount, startIndex, endIndex);
                }

                // Calculate the length of the TSPPlanner-generated route
                double tspRouteLength = 0.0;
                for (int k = 0; k < route.size() - 1; k++) {
                    MowingPlace a = route.get(k);
                    MowingPlace b = route.get(k + 1);
                    int ai = indexById.get(a.getId());
                    int bi = indexById.get(b.getId());
                    tspRouteLength += dist[ai][bi];
                }

                // Calculate the percentage degradation of TSPPlanner’s solution vs optimal
                double percentDiff = ((tspRouteLength - optimalLength) / optimalLength) * 100.0;
                totalPercent += percentDiff;
                if (percentDiff > maxPercent) {
                    maxPercent = percentDiff;
                }
                if (percentDiff < minPercent) {
                    minPercent = percentDiff;
                }


                //if first is not start and last is not end
                if (!route.get(0).getId().equals(start.getId()) || !route.get(route.size() - 1).getId().equals(end.getId())) {
                    String pathids = "";
                    for (MowingPlace place : route) {
                        pathids += place.getId() + " ";
                    }
                    out.println("Path IDs: " + pathids);
                }
                //if number of places is not equal to n+2
                if (route.size() != n + 2) {
                    String pathids = "";
                    for (MowingPlace place : route) {
                        pathids += place.getId() + " ";
                    }
                    out.println(n + " Path IDs: " + pathids);
                }


            } // end of runs for this n

            // Compute average percentage difference for this n
            double avgPercent = totalPercent / RUNS_PER_N;
            // Write results line for this n
            out.printf(Locale.US, "n=%d: avg=%.2f%%, max=%.2f%%, min=%.2f%%%n", n, avgPercent, maxPercent, minPercent);
        } // end of loop for n=1..10

        out.close();
    }

    /**
     * Compute the great-circle distance between two points (Haversine formula).
     */
    private static double haversineDistance(MowingPlace p1, MowingPlace p2) {
        double lat1 = p1.getLatitude();
        double lon1 = p1.getLongitude();
        double lat2 = p2.getLatitude();
        double lon2 = p2.getLongitude();
        double R = 6371000.0;  // Earth’s radius in meters
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);
        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                Math.cos(phi1) * Math.cos(phi2) *
                        Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Compute the optimal path length using brute-force permutation (exhaustive search).
     * This considers all permutations of the intermediate nodes.
     */
    private static double bruteForceOptimalDistance(double[][] dist, int interCount, int startIndex, int endIndex) {
        if (interCount == 0) {
            return dist[startIndex][endIndex];  // (for completeness; not used in n>=1 benchmarks)
        }
        // List of intermediate node indices (0 to interCount-1)
        List<Integer> nodes = new ArrayList<>();
        for (int i = 0; i < interCount; i++) {
            nodes.add(i);
        }
        // Compute shortest route among all permutations
        return permuteAndFindBest(nodes, 0, startIndex, endIndex, dist, Double.POSITIVE_INFINITY);
    }

    // Helper method to generate permutations recursively and track the best distance found
    private static double permuteAndFindBest(List<Integer> arr, int l, int startIndex, int endIndex, double[][] dist, double currentBest) {
        int n = arr.size();
        if (l == n) {
            // We have a complete permutation in arr
            double totalDist = 0.0;
            // Distance from start to first intermediate
            totalDist += dist[startIndex][arr.get(0)];
            // Distances for intermediate sequence
            for (int i = 0; i < n - 1; i++) {
                totalDist += dist[arr.get(i)][arr.get(i + 1)];
                // Prune: if partial sum already exceeds current best, no need to continue
                if (totalDist >= currentBest) {
                    return currentBest;
                }
            }
            // Distance from last intermediate to end
            totalDist += dist[arr.get(n - 1)][endIndex];
            if (totalDist < currentBest) {
                currentBest = totalDist;
            }
        } else {
            for (int i = l; i < n; i++) {
                Collections.swap(arr, l, i);
                // Recurse and update best distance
                currentBest = permuteAndFindBest(arr, l + 1, startIndex, endIndex, dist, currentBest);
                Collections.swap(arr, l, i);  // backtrack
                // Early exit: if best possible is 0, cannot get lower
                if (currentBest == 0.0) {
                    return 0.0;
                }
            }
        }
        return currentBest;
    }

    /**
     * Compute the optimal path length using the Held-Karp dynamic programming algorithm.
     * This efficiently finds the shortest path from the fixed start to fixed end through all intermediates.
     */
    private static double heldKarpOptimalDistance(double[][] dist, int interCount, int startIndex, int endIndex) {
        int n = interCount;
        if (n == 0) {
            // No intermediates: direct distance from start to end
            return dist[startIndex][endIndex];  // (for completeness; not used in n>=1 benchmarks)
        }
        int totalStates = 1 << n;
        // dp[mask][j] = shortest distance from start to reach intermediate j with visited set = mask (mask includes j)
        double[][] dp = new double[totalStates][n];
        // Initialize dp with infinity
        for (int mask = 0; mask < totalStates; mask++) {
            Arrays.fill(dp[mask], Double.POSITIVE_INFINITY);
        }
        // Base case: start -> j for each single intermediate j
        for (int j = 0; j < n; j++) {
            int mask = 1 << j;
            dp[mask][j] = dist[startIndex][j];
        }
        // Fill DP table for masks of increasing size
        for (int mask = 1; mask < totalStates; mask++) {
            for (int j = 0; j < n; j++) {
                if ((mask & (1 << j)) == 0) continue;  // j not in this subset
                double prevDist = dp[mask][j];
                if (prevDist == Double.POSITIVE_INFINITY) continue;
                // Try to extend path from j to a new intermediate k not yet in mask
                int remaining = ((1 << n) - 1) ^ mask;  // bitmask of nodes not in 'mask'
                for (int k = 0; k < n; k++) {
                    if ((remaining & (1 << k)) == 0) continue;
                    int newMask = mask | (1 << k);
                    double newDist = prevDist + dist[j][k];
                    if (newDist < dp[newMask][k]) {
                        dp[newMask][k] = newDist;
                    }
                }
            }
        }
        // All intermediates visited (mask = fullMask). Now add distance to end and find minimum.
        int fullMask = (1 << n) - 1;
        double best = Double.POSITIVE_INFINITY;
        for (int j = 0; j < n; j++) {
            double routeDist = dp[fullMask][j] + dist[j][endIndex];
            if (routeDist < best) {
                best = routeDist;
            }
        }
        return best;
    }
}
