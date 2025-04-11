package com.example.myapplication.util;

import com.example.myapplication.data.MowingPlace;
import com.example.myapplication.data.MowingPlace.DistanceEntry;

import java.util.*;

/**
 * Planner for Traveling Salesman Problem (TSP) routes using Christofides–Serdyukov approximation.
 */
public class TSPPlanner {

    /**
     * Generates a near-optimal ordered route of MowingPlace nodes, starting at the node with ID "start"
     * and ending at the node with ID "end", using a 3/2-approximation TSP algorithm (Christofides).
     * All intermediate nodes are visited exactly once. Distances are based on the provided
     * distancesToOthers list, with missing values estimated via Haversine formula.
     *
     * @param nodes           List of MowingPlace nodes (including the start and end nodes).
     * @return Ordered list of MowingPlace objects representing the route from start to end.
     */
    public static List<MowingPlace> generateRoute(List<MowingPlace> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }

        // Identify start and end nodes by ID
        MowingPlace startNode = null;
        MowingPlace endNode = null;
        for (MowingPlace place : nodes) {
            if ("start".equals(place.getId())) {
                startNode = place;
            } else if ("end".equals(place.getId())) {
                endNode = place;
            }
        }
        if (startNode == null || endNode == null) {
            // If we cannot find the start or end node, return the input or empty route
            return nodes;
        }

        // Prepare list of intermediate nodes (exclude start and end from TSP cycle computation)
        List<MowingPlace> intermediates = new ArrayList<>();
        for (MowingPlace place : nodes) {
            if (place != startNode && place != endNode) {
                intermediates.add(place);
            }
        }
        int nInter = intermediates.size();

        // Map node IDs to matrix indices for convenience
        // We will assign indices 0..nInter-1 to intermediate nodes, index nInter to start, and nInter+1 to end.
        Map<String, Integer> indexById = new HashMap<>();
        Map<Integer, MowingPlace> nodeByIndex = new HashMap<>();
        for (int i = 0; i < nInter; i++) {
            MowingPlace place = intermediates.get(i);
            indexById.put(place.getId(), i);
            nodeByIndex.put(i, place);
        }
        int startIndex = nInter;
        int endIndex = nInter + 1;
        indexById.put(startNode.getId(), startIndex);
        indexById.put(endNode.getId(), endIndex);
        nodeByIndex.put(startIndex, startNode);
        nodeByIndex.put(endIndex, endNode);

        int totalNodes = nInter + 2;  // total nodes including start and end
        // Initialize distance matrix for all nodes (use double for distances)
        double[][] dist = new double[totalNodes][totalNodes];
        // Fill with some large default for missing values
        for (int i = 0; i < totalNodes; i++) {
            Arrays.fill(dist[i], Double.POSITIVE_INFINITY);
            dist[i][i] = 0.0;
        }

        // Populate distance matrix using distancesToOthers lists
        for (MowingPlace place : nodes) {
            Integer i = indexById.get(place.getId());
            if (i == null) continue;  // skip if not in our index map
            List<DistanceEntry> dList = place.getDistancesToOthers();
            if (dList != null) {
                for (DistanceEntry entry : dList) {
                    Integer j = indexById.get(entry.getId());
                    if (j != null) {
                        // Use provided distance (convert to double)
                        dist[i][j] = entry.getDistance();
                    }
                }
            }
        }

        // Ensure symmetry and compute missing distances via Haversine formula
        for (int i = 0; i < totalNodes; i++) {
            for (int j = i + 1; j < totalNodes; j++) {
                if (dist[i][j] == Double.POSITIVE_INFINITY && dist[j][i] == Double.POSITIVE_INFINITY) {
                    // Distance missing in both directions – calculate using Haversine
                    MowingPlace pi = nodeByIndex.get(i);
                    MowingPlace pj = nodeByIndex.get(j);
                    double havDist = haversineDistance(pi.getLatitude(), pi.getLongitude(),
                            pj.getLatitude(), pj.getLongitude());
                    dist[i][j] = havDist;
                    dist[j][i] = havDist;
                } else if (dist[i][j] == Double.POSITIVE_INFINITY) {
                    // Use the known opposite direction distance
                    dist[i][j] = dist[j][i];
                } else if (dist[j][i] == Double.POSITIVE_INFINITY) {
                    dist[j][i] = dist[i][j];
                } else {
                    // Both distances are present; enforce symmetry by taking the minimum
                    double d = Math.min(dist[i][j], dist[j][i]);
                    dist[i][j] = d;
                    dist[j][i] = d;
                }
            }
        }

        // We will run Christofides on the intermediate nodes (indices 0..nInter-1).
        int n = nInter;
        List<Integer> cycleOrder = new ArrayList<>();  // this will store the Hamiltonian cycle (tour) for intermediate nodes

        if (n > 0) {
            // 1. Compute MST on intermediate nodes using Prim's algorithm
            boolean[] inMST = new boolean[n];
            double[] minEdge = new double[n];
            int[] parent = new int[n];
            Arrays.fill(minEdge, Double.POSITIVE_INFINITY);
            Arrays.fill(parent, -1);
            minEdge[0] = 0.0;  // start MST from node 0 (arbitrary choice)

            for (int k = 0; k < n; k++) {
                int u = -1;
                for (int v = 0; v < n; v++) {
                    if (!inMST[v] && (u == -1 || minEdge[v] < minEdge[u])) {
                        u = v;
                    }
                }
                if (u == -1) break;  // should not happen if graph is connected
                inMST[u] = true;
                // Update neighbor distances
                for (int w = 0; w < n; w++) {
                    if (!inMST[w] && dist[u][w] < minEdge[w]) {
                        minEdge[w] = dist[u][w];
                        parent[w] = u;
                    }
                }
            }

            // Build adjacency list for MST
            List<Set<Integer>> mstAdj = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                mstAdj.add(new HashSet<>());
            }
            for (int v = 1; v < n; v++) {  // v=0 is root, parent of root stays -1
                int u = parent[v];
                if (u != -1) {
                    mstAdj.get(u).add(v);
                    mstAdj.get(v).add(u);
                }
            }

            // 2. Find all vertices with odd degree in MST
            List<Integer> oddVertices = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (mstAdj.get(i).size() % 2 != 0) {
                    oddVertices.add(i);
                }
            }

            // 3. Compute minimum weight perfect matching on the subgraph induced by odd-degree vertices
            // We will find pairs among oddVertices that minimize the sum of distances.
            List<int[]> matchingEdges = new ArrayList<>();
            int m = oddVertices.size();
            if (m > 0) {
                if (m <= 16) {
                    // Use DP bitmask algorithm for exact minimum-weight perfect matching (for small sets)
                    int mMaskSize = 1 << m;
                    double[] dpMatch = new double[mMaskSize];
                    int[] pairChoice = new int[mMaskSize];
                    Arrays.fill(dpMatch, Double.POSITIVE_INFINITY);
                    dpMatch[0] = 0.0;
                    // Iterate over all subsets of odd vertices
                    for (int mask = 0; mask < mMaskSize; mask++) {
                        if (dpMatch[mask] == Double.POSITIVE_INFINITY) continue;
                        // Find first unmatched vertex in this subset
                        int i;
                        for (i = 0; i < m; i++) {
                            if ((mask & (1 << i)) == 0) {
                                break;
                            }
                        }
                        if (i >= m) continue; // no unmatched vertices
                        int maskWithI = mask | (1 << i);
                        // Try pairing i with any other unmatched j
                        for (int j = i + 1; j < m; j++) {
                            if ((mask & (1 << j)) != 0) continue;
                            int newMask = maskWithI | (1 << j);
                            // vertices indices in original graph:
                            int v1 = oddVertices.get(i);
                            int v2 = oddVertices.get(j);
                            double edgeWeight = dist[v1][v2];
                            if (dpMatch[newMask] > dpMatch[mask] + edgeWeight) {
                                dpMatch[newMask] = dpMatch[mask] + edgeWeight;
                                // store this pairing choice for reconstruction
                                pairChoice[newMask] = (i << 16) | j;
                            }
                        }
                    }
                    // Reconstruct matching pairs from DP result
                    int fullMask = (1 << m) - 1;
                    int curMask = fullMask;
                    boolean[] matched = new boolean[m];
                    while (curMask != 0) {
                        int pair = pairChoice[curMask];
                        int i = pair >> 16;
                        int j = pair & 0xFFFF;
                        // Add the edge (oddVertices[i], oddVertices[j]) to matching
                        int v1 = oddVertices.get(i);
                        int v2 = oddVertices.get(j);
                        matchingEdges.add(new int[]{v1, v2});
                        // Remove i and j from the current mask
                        curMask &= ~(1 << i);
                        curMask &= ~(1 << j);
                    }
                } else {
                    // For larger sets of odd vertices, use a greedy approach (approximate matching)
                    Set<Integer> unmatched = new HashSet<>(oddVertices);
                    while (!unmatched.isEmpty()) {
                        Iterator<Integer> it = unmatched.iterator();
                        int v1 = it.next();
                        it.remove();
                        if (unmatched.isEmpty()) break;
                        // find the closest unmatched vertex to v1
                        int v2 = -1;
                        double minDist = Double.POSITIVE_INFINITY;
                        for (int u : unmatched) {
                            if (dist[v1][u] < minDist) {
                                minDist = dist[v1][u];
                                v2 = u;
                            }
                        }
                        // add pair (v1, v2)
                        unmatched.remove(v2);
                        if (v2 != -1) {
                            matchingEdges.add(new int[]{v1, v2});
                        }
                    }
                }
                // Add matching edges to MST adjacency to form Eulerian multigraph
                for (int[] edge : matchingEdges) {
                    int u = edge[0];
                    int v = edge[1];
                    mstAdj.get(u).add(v);
                    mstAdj.get(v).add(u);
                }
            }

            // 4. Eulerian circuit: find an Eulerian tour in the combined graph (MST + matching)
            List<Integer> eulerTour = new ArrayList<>();
            Stack<Integer> stack = new Stack<>();
            Stack<Integer> path = new Stack<>();
            stack.push(0);  // start from vertex 0
            // We will copy the adjacency list to modify while finding the tour
            List<Deque<Integer>> adjCopy = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                adjCopy.add(new ArrayDeque<>(mstAdj.get(i)));
            }
            while (!stack.isEmpty()) {
                int u = stack.peek();
                if (adjCopy.get(u).isEmpty()) {
                    // No more edges out of u, add to tour
                    path.push(u);
                    stack.pop();
                } else {
                    // Follow an unused edge
                    int v = adjCopy.get(u).poll();  // get a neighbor
                    // Remove the edge v->u as well
                    adjCopy.get(v).remove(u);
                    stack.push(v);
                }
            }
            // path stack now contains the Eulerian circuit in reverse order
            while (!path.isEmpty()) {
                eulerTour.add(path.pop());
            }

            // 5. Make it a Hamiltonian circuit by skipping repeated vertices (shortcutting)
            // We traverse the Euler tour and add each vertex once.
            Set<Integer> visited = new HashSet<>();
            for (int vertex : eulerTour) {
                if (!visited.contains(vertex)) {
                    cycleOrder.add(vertex);
                    visited.add(vertex);
                }
            }
            // The cycleOrder now contains each intermediate vertex exactly once, forming a tour (cycle).
        }

        // If there are no intermediate nodes, then the route is simply start -> end
        if (cycleOrder.isEmpty()) {
            return Arrays.asList(startNode, endNode);
        }

        // 6. Convert the Hamiltonian cycle to a path from start to end by finding the best edge to break.
        // We will evaluate each edge of the cycle to determine where to "cut" the cycle and insert start and end.
        double bestIncrease = Double.POSITIVE_INFINITY;
        int breakIndex = 0;
        boolean attachSwapped = false;
        int cycleSize = cycleOrder.size();
        // Calculate total cycle cost (for reference)
        double cycleCost = 0.0;
        for (int k = 0; k < cycleSize; k++) {
            int vi = cycleOrder.get(k);
            int vj = cycleOrder.get((k + 1) % cycleSize);
            cycleCost += dist[vi][vj];
        }
        // Try removing each edge (cycle[i] - cycle[i+1]) and connecting start/end to the break
        for (int i = 0; i < cycleSize; i++) {
            int u = cycleOrder.get(i);
            int v = cycleOrder.get((i + 1) % cycleSize);
            // Compute the cost increase if we remove edge (u,v) and attach start to u and end to v
            double cost1 = dist[startIndex][u] + dist[v][endIndex] - dist[u][v];
            // Compute the cost increase if we attach start to v and end to u instead
            double cost2 = dist[startIndex][v] + dist[u][endIndex] - dist[u][v];
            if (cost1 < bestIncrease) {
                bestIncrease = cost1;
                breakIndex = i;
                attachSwapped = false;
            }
            if (cost2 < bestIncrease) {
                bestIncrease = cost2;
                breakIndex = i;
                attachSwapped = true;
            }
        }

        // Retrieve the chosen edge to break
        int breakU = cycleOrder.get(breakIndex);
        int breakV = cycleOrder.get((breakIndex + 1) % cycleSize);
        // Prepare an adjacency list for the cycle to help retrieve the path order after breaking
        Map<Integer, List<Integer>> cycleAdj = new HashMap<>();
        for (int i = 0; i < cycleSize; i++) {
            int u = cycleOrder.get(i);
            int v = cycleOrder.get((i + 1) % cycleSize);
            cycleAdj.computeIfAbsent(u, x -> new ArrayList<>()).add(v);
            cycleAdj.computeIfAbsent(v, x -> new ArrayList<>()).add(u);
        }
        // Remove the chosen edge (breakU - breakV) from the cycle adjacency to "break" the cycle
        cycleAdj.get(breakU).remove((Integer) breakV);
        cycleAdj.get(breakV).remove((Integer) breakU);

        // Find the path between breakU and breakV in this broken cycle (it should be a linear path now)
        int pathStart = attachSwapped ? breakV : breakU;
        int pathEnd = attachSwapped ? breakU : breakV;
        List<Integer> intermediatePath = findPathInTree(cycleAdj, pathStart, pathEnd);

        // 7. Build the final route: start node -> (intermediate path nodes in order) -> end node
        List<MowingPlace> route = new ArrayList<>();
        route.add(startNode);
        for (int idx : intermediatePath) {
            // Add each intermediate place by index
            route.add(nodeByIndex.get(idx));
        }
        route.add(endNode);

        return route;
    }

    /**
     * Computes the great-circle distance between two points on Earth using the Haversine formula.
     * @param lat1 Latitude of first point in degrees.
     * @param lon1 Longitude of first point in degrees.
     * @param lat2 Latitude of second point in degrees.
     * @param lon2 Longitude of second point in degrees.
     * @return Distance between the two points in meters.
     */
    private static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        // Earth radius in meters
        final double R = 6371000.0;
        // Convert degrees to radians
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lon2 - lon1);
        // Haversine formula
        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2) +
                Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
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
    public static List<MowingPlace> addExtraCemeteries(List<MowingPlace> currentRoute, List<MowingPlace> allAvailablePlaces, int endTime, double speedMultiplier, boolean addVisited, int timeFromLastVisit) {
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
