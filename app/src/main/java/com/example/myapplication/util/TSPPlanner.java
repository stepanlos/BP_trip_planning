package com.example.myapplication.util;

import com.example.myapplication.data.MowingPlace;
import com.example.myapplication.data.MowingPlace.DistanceEntry;

import java.util.*;

/**
 * Planner for Traveling Salesman Problem (TSP) routes using Christofides–Serdyukov approximation.
 */
public class TSPPlanner {

    /**
     * Generates a near-optimal ordered route from start to end using a modified TSP algorithm.
     * A dummy node is added among the intermediate nodes that has zero distance to start and end,
     * and extremely high distance to all other intermediate nodes.
     * After computing the TSP cycle (using Christofides), the dummy node is removed and the cycle is "cut"
     * at its position, yielding a final route from start -> intermediate nodes -> end.
     *
     * @param nodes List of MowingPlace nodes, where the nodes with id "start" and "end" represent the start and end.
     * @return Ordered list of MowingPlace objects representing the final route.
     */
    public static List<MowingPlace> generateRoute(List<MowingPlace> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }

        // Identify start and end nodes and collect intermediate nodes.
        MowingPlace startNode = null;
        MowingPlace endNode = null;
        List<MowingPlace> intermediates = new ArrayList<>();
        for (MowingPlace place : nodes) {
            if ("start".equals(place.getId())) {
                startNode = place;
            } else if ("end".equals(place.getId())) {
                endNode = place;
            } else {
                intermediates.add(place);
            }
        }
        if (startNode == null || endNode == null) {
            // If start or end are missing, return the input list
            return nodes;
        }

        // Create a dummy node and add it to the intermediate nodes list.
        MowingPlace dummyNode = new MowingPlace();
        dummyNode.setId("dummy");
        // Other attributes of dummy can remain at their default values.
        intermediates.add(dummyNode);

        // Build mappings for intermediate nodes (indices 0 .. nInter-1)
        int nInter = intermediates.size(); // now includes dummy node
        Map<String, Integer> indexById = new HashMap<>();
        Map<Integer, MowingPlace> nodeByIndex = new HashMap<>();
        for (int i = 0; i < nInter; i++) {
            MowingPlace place = intermediates.get(i);
            indexById.put(place.getId(), i);
            nodeByIndex.put(i, place);
        }
        // Assign new indices for start and end nodes.
        int startIndex = nInter;
        int endIndex = nInter + 1;
        indexById.put(startNode.getId(), startIndex);
        indexById.put(endNode.getId(), endIndex);
        nodeByIndex.put(startIndex, startNode);
        nodeByIndex.put(endIndex, endNode);

        int totalNodes = nInter + 2; // intermediates (with dummy) + start + end
        double[][] dist = new double[totalNodes][totalNodes];

        // Initialize the distance matrix.
        for (int i = 0; i < totalNodes; i++) {
            Arrays.fill(dist[i], Double.POSITIVE_INFINITY);
            dist[i][i] = 0.0;
        }

        // Populate distance matrix using the distancesToOthers list for nodes provided in input.
        for (MowingPlace place : nodes) {
            Integer i = indexById.get(place.getId());
            if (i == null) continue;
            List<MowingPlace.DistanceEntry> dList = place.getDistancesToOthers();
            if (dList != null) {
                for (MowingPlace.DistanceEntry entry : dList) {
                    Integer j = indexById.get(entry.getId());
                    if (j != null) {
                        dist[i][j] = entry.getDistance();
                    }
                }
            }
        }

        // Explicitly set distances for the dummy node:
        int dummyIndex = indexById.get("dummy");
        // Set distance from dummy to start and end as 0.
        dist[dummyIndex][startIndex] = 0.0;
        dist[startIndex][dummyIndex] = 0.0;
        dist[dummyIndex][endIndex] = 0.0;
        dist[endIndex][dummyIndex] = 0.0;
        // For dummy node to all other intermediate nodes (except start and end), set distance to a very high value.
        for (int j = 0; j < totalNodes; j++) {
            if (j != dummyIndex && j != startIndex && j != endIndex) {
                dist[dummyIndex][j] = Double.POSITIVE_INFINITY;
                dist[j][dummyIndex] = Double.POSITIVE_INFINITY;
            }
        }

        // Ensure symmetry: if distance is missing, estimate via Haversine formula.
        for (int i = 0; i < totalNodes; i++) {
            for (int j = i + 1; j < totalNodes; j++) {
                if (dist[i][j] == Double.POSITIVE_INFINITY && dist[j][i] == Double.POSITIVE_INFINITY) {
                    MowingPlace pi = nodeByIndex.get(i);
                    MowingPlace pj = nodeByIndex.get(j);
                    double havDist = haversineDistance(pi.getLatitude(), pi.getLongitude(),
                            pj.getLatitude(), pj.getLongitude());
                    dist[i][j] = havDist;
                    dist[j][i] = havDist;
                } else if (dist[i][j] == Double.POSITIVE_INFINITY) {
                    dist[i][j] = dist[j][i];
                } else if (dist[j][i] == Double.POSITIVE_INFINITY) {
                    dist[j][i] = dist[i][j];
                } else {
                    double d = Math.min(dist[i][j], dist[j][i]);
                    dist[i][j] = d;
                    dist[j][i] = d;
                }
            }
        }

        // Run Christofides algorithm on the intermediate nodes (indices 0..nInter-1), i.e. including the dummy node.
        int n = nInter;
        List<Integer> cycleOrder = new ArrayList<>();

        if (n > 0) {
            // 1. Compute MST on intermediate nodes using Prim's algorithm.
            boolean[] inMST = new boolean[n];
            double[] minEdge = new double[n];
            int[] parent = new int[n];
            Arrays.fill(minEdge, Double.POSITIVE_INFINITY);
            Arrays.fill(parent, -1);
            minEdge[0] = 0.0;
            for (int k = 0; k < n; k++) {
                int u = -1;
                for (int v = 0; v < n; v++) {
                    if (!inMST[v] && (u == -1 || minEdge[v] < minEdge[u])) {
                        u = v;
                    }
                }
                if (u == -1) break; // graph disconnected? Should not happen.
                inMST[u] = true;
                for (int w = 0; w < n; w++) {
                    if (!inMST[w] && dist[u][w] < minEdge[w]) {
                        minEdge[w] = dist[u][w];
                        parent[w] = u;
                    }
                }
            }

            // Build MST adjacency list.
            List<Set<Integer>> mstAdj = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                mstAdj.add(new HashSet<>());
            }
            for (int v = 1; v < n; v++) { // v = 0 is the arbitrary root.
                int u = parent[v];
                if (u != -1) {
                    mstAdj.get(u).add(v);
                    mstAdj.get(v).add(u);
                }
            }

            // 2. Identify vertices with odd degree in the MST.
            List<Integer> oddVertices = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (mstAdj.get(i).size() % 2 != 0) {
                    oddVertices.add(i);
                }
            }

            // 3. Compute a minimum-weight perfect matching on the odd-degree vertices.
            List<int[]> matchingEdges = new ArrayList<>();
            int mOdd = oddVertices.size();
            if (mOdd > 0) {
                if (mOdd <= 16) {
                    int mMaskSize = 1 << mOdd;
                    double[] dpMatch = new double[mMaskSize];
                    int[] pairChoice = new int[mMaskSize];
                    Arrays.fill(dpMatch, Double.POSITIVE_INFINITY);
                    dpMatch[0] = 0.0;
                    for (int mask = 0; mask < mMaskSize; mask++) {
                        if (dpMatch[mask] == Double.POSITIVE_INFINITY)
                            continue;
                        int i;
                        for (i = 0; i < mOdd; i++) {
                            if ((mask & (1 << i)) == 0)
                                break;
                        }
                        if (i >= mOdd)
                            continue; // all matched.
                        int maskWithI = mask | (1 << i);
                        for (int j = i + 1; j < mOdd; j++) {
                            if ((mask & (1 << j)) != 0)
                                continue;
                            int newMask = maskWithI | (1 << j);
                            int v1 = oddVertices.get(i);
                            int v2 = oddVertices.get(j);
                            double edgeWeight = dist[v1][v2];
                            if (dpMatch[newMask] > dpMatch[mask] + edgeWeight) {
                                dpMatch[newMask] = dpMatch[mask] + edgeWeight;
                                pairChoice[newMask] = (i << 16) | j;
                            }
                        }
                    }
                    int fullMask = (1 << mOdd) - 1;
                    int curMask = fullMask;
                    while (curMask != 0) {
                        int pair = pairChoice[curMask];
                        int i = pair >> 16;
                        int j = pair & 0xFFFF;
                        int v1 = oddVertices.get(i);
                        int v2 = oddVertices.get(j);
                        matchingEdges.add(new int[]{v1, v2});
                        curMask &= ~(1 << i);
                        curMask &= ~(1 << j);
                    }
                } else {
                    // Use a greedy matching approach for larger sets.
                    Set<Integer> unmatched = new HashSet<>(oddVertices);
                    while (!unmatched.isEmpty()) {
                        Iterator<Integer> it = unmatched.iterator();
                        int v1 = it.next();
                        it.remove();
                        if (unmatched.isEmpty())
                            break;
                        int v2 = -1;
                        double minDist = Double.POSITIVE_INFINITY;
                        for (int u : unmatched) {
                            if (dist[v1][u] < minDist) {
                                minDist = dist[v1][u];
                                v2 = u;
                            }
                        }
                        unmatched.remove(v2);
                        if (v2 != -1) {
                            matchingEdges.add(new int[]{v1, v2});
                        }
                    }
                }
                // Add matching edges to the MST adjacency to form an Eulerian multigraph.
                for (int[] edge : matchingEdges) {
                    int u = edge[0];
                    int v = edge[1];
                    mstAdj.get(u).add(v);
                    mstAdj.get(v).add(u);
                }
            }

            // 4. Find an Eulerian tour in the multigraph (MST + matching).
            List<Integer> eulerTour = new ArrayList<>();
            Stack<Integer> stack = new Stack<>();
            Stack<Integer> path = new Stack<>();
            stack.push(0); // Start from an arbitrary intermediate vertex.
            // Create a modifiable copy of the adjacency list.
            List<Deque<Integer>> adjCopy = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                adjCopy.add(new ArrayDeque<>(mstAdj.get(i)));
            }
            while (!stack.isEmpty()) {
                int u = stack.peek();
                if (adjCopy.get(u).isEmpty()) {
                    path.push(u);
                    stack.pop();
                } else {
                    int v = adjCopy.get(u).poll();
                    adjCopy.get(v).remove(u);
                    stack.push(v);
                }
            }
            while (!path.isEmpty()) {
                eulerTour.add(path.pop());
            }

            // 5. Shortcut the Eulerian tour to form a Hamiltonian cycle (remove repeated vertices).
            Set<Integer> visited = new HashSet<>();
            for (int vertex : eulerTour) {
                if (!visited.contains(vertex)) {
                    cycleOrder.add(vertex);
                    visited.add(vertex);
                }
            }
        }

        // At this point, cycleOrder contains the TSP order of intermediate nodes (including the dummy).
        // Find the dummy node in the cycle and rotate the list so that dummy is the first element.
        int dummyPos = -1;
        for (int i = 0; i < cycleOrder.size(); i++) {
            MowingPlace place = nodeByIndex.get(cycleOrder.get(i));
            if ("dummy".equals(place.getId())) {
                dummyPos = i;
                break;
            }
        }
        if (dummyPos != -1) {
            List<Integer> rotated = new ArrayList<>();
            for (int i = 0; i < cycleOrder.size(); i++) {
                rotated.add(cycleOrder.get((dummyPos + i) % cycleOrder.size()));
            }
            cycleOrder = rotated;
            // Remove the dummy node (now at the beginning of the list).
            cycleOrder.remove(0);
        }

        // Build the final route: start -> (ordered intermediate nodes) -> end.
        List<MowingPlace> finalRoute = new ArrayList<>();
        finalRoute.add(startNode);
        for (Integer idx : cycleOrder) {
            finalRoute.add(nodeByIndex.get(idx));
        }
        finalRoute.add(endNode);

        return finalRoute;
    }

    // Helper: Haversine distance calculation (in meters).
    private static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Finds a path between two vertices in a tree (acyclic connected graph) using BFS.
     * This is used to retrieve the sequence of intermediate nodes after breaking the cycle.
     *
     * @param graphAdj Adjacency list of the tree graph.
     * @param start    Starting vertex.
     * @param target   Target vertex to reach.
     * @return List of vertices from start to target (inclusive) along the unique path in the tree.
     */
    private static List<Integer> findPathInTree(Map<Integer, List<Integer>> graphAdj, int start, int target) {
        // BFS to find parent pointers
        Map<Integer, Integer> parent = new HashMap<>();
        Deque<Integer> queue = new ArrayDeque<>();
        parent.put(start, null);
        queue.offer(start);
        while (!queue.isEmpty() && !parent.containsKey(target)) {
            int u = queue.poll();
            for (int w : graphAdj.getOrDefault(u, Collections.emptyList())) {
                if (!parent.containsKey(w)) {
                    parent.put(w, u);
                    queue.offer(w);
                    if (w == target) break;
                }
            }
        }
        // Reconstruct path from target to start using parent map
        List<Integer> path = new ArrayList<>();
        if (!parent.containsKey(target)) {
            return path;  // no path found (should not happen in a tree)
        }
        Integer cur = target;
        while (cur != null) {
            path.add(cur);
            cur = parent.get(cur);
        }
        Collections.reverse(path);
        return path;
    }



    /**
     * Optionally adds extra cemeteries to the route if leftover time remains.
     * This is a stub implementation – replace with logic based on priority and time constraints.
     *
     * @param currentRoute The current route generated by TSP.
     * @param allAvailablePlaces All available MowingPlace objects.
     * @param endTime The time constraint at the end location.
     * @param speedMultiplier Speed multiplier applied.
     * @return Updated route with extra cemeteries added if possible.
     */
    public static List<MowingPlace> addExtraCemeteries(List<MowingPlace> currentRoute, List<MowingPlace> allAvailablePlaces, int endTime, double speedMultiplier) {
        // For demonstration, if current route time is less than endTime, add one extra place (if available)
        double currentTime = 0;
        for (MowingPlace mp : currentRoute) {
            currentTime += mp.getTimeRequirement();
        }
        currentTime /= speedMultiplier; // Adjust for speed multiplier
        // If there is at least 30 minutes remaining, try to add one extra cemetery not already in route
        if (endTime - currentTime >= 30) {
            for (MowingPlace extra : allAvailablePlaces) {
                boolean alreadyInRoute = false;
                for (MowingPlace mp : currentRoute) {
                    if (mp.getId().equals(extra.getId())) {
                        alreadyInRoute = true;
                        break;
                    }
                }
                if (!alreadyInRoute) {
                    // Adjust the mowing time using the speed multiplier
                    extra.setTimeRequirement(extra.getTimeRequirement() / speedMultiplier);
                    currentRoute.add(currentRoute.size() - 1, extra); // Insert before the end node
                    break;
                }
            }
        }
        return currentRoute;
    }
}
