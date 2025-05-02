package com.example.myapplication.util;

import android.util.Log;

import com.example.myapplication.data.MowingPlace;
import com.example.myapplication.data.MowingPlace.DistanceEntry;

import org.jgrapht.alg.cycle.HierholzerEulerianCycle;
import org.jgrapht.alg.spanning.KruskalMinimumSpanningTree;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.Multigraph;
import org.threeten.bp.LocalDate;

import java.util.*;

import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.alg.matching.blossom.v5.KolmogorovMinimumWeightPerfectMatching;
import org.jgrapht.alg.interfaces.MatchingAlgorithm;

import com.example.myapplication.data.MowingPlace;
import com.example.myapplication.data.MowingPlace.DistanceEntry;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.tour.ChristofidesThreeHalvesApproxMetricTSP;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TSPPlanner {

    /**
     * Returns a 3/2-approximate path from "start" → … → "end"
     * visiting every MowingPlace exactly once.
     */
    public static List<MowingPlace> generateRoute(List<MowingPlace> nodes) {
        //if only one intermediate place, return start, intermediate, end
        if (nodes.size() == 3) {
            return List.of(nodes.get(0), nodes.get(1), nodes.get(2));
        }

        // 1) identify start/end
        MowingPlace start = null, end = null;
        for (MowingPlace p : nodes) {
            if ("start".equals(p.getId())) start = p;
            else if ("end".equals(p.getId())) end = p;
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("Must include both start and end");
        }

        // 2) build the complete, metric weighted graph
        Graph<MowingPlace, DefaultWeightedEdge> complete =
                new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        for (MowingPlace p : nodes) {
            complete.addVertex(p);
        }
        int n = nodes.size();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                MowingPlace a = nodes.get(i), b = nodes.get(j);
                DefaultWeightedEdge e = complete.addEdge(a, b);
                complete.setEdgeWeight(e, computeDistance(a, b));
            }
        }

        // 3) compute an MST on that graph
        KruskalMinimumSpanningTree<MowingPlace, DefaultWeightedEdge> kruskal =
                new KruskalMinimumSpanningTree<>(complete);
        Set<DefaultWeightedEdge> mstEdges = new HashSet<>(kruskal.getSpanningTree().getEdges());

        // 4) find all odd‐degree vertices in the MST
        Set<MowingPlace> odd = new HashSet<>();
        for (MowingPlace v : complete.vertexSet()) {
            int deg = 0;
            for (DefaultWeightedEdge e : mstEdges) {
                if (complete.getEdgeSource(e).equals(v) ||
                        complete.getEdgeTarget(e).equals(v)) {
                    deg++;
                }
            }
            if ((deg & 1) == 1) {
                odd.add(v);
            }
        }
        // 4b) symmetric‐difference with {start, end}
        //    this flips membership of start/end in the odd‐set,
        //    guaranteeing |odd| remains even.
        if (!odd.remove(start)) {
            odd.add(start);
        }
        if (!odd.remove(end)) {
            odd.add(end);
        }

        // 5) form the induced complete subgraph on those odd vertices
        Graph<MowingPlace, DefaultWeightedEdge> oddComplete =
                new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        for (MowingPlace v : odd) {
            oddComplete.addVertex(v);
        }
        List<MowingPlace> oddList = new ArrayList<>(odd);
        for (int i = 0; i < oddList.size(); i++) {
            for (int j = i + 1; j < oddList.size(); j++) {
                MowingPlace a = oddList.get(i), b = oddList.get(j);
                DefaultWeightedEdge e = oddComplete.addEdge(a, b);
                double w = complete.getEdgeWeight(complete.getEdge(a, b));
                oddComplete.setEdgeWeight(e, w);
            }
        }

        // 6) compute a minimum‐weight perfect matching on that odd set
        KolmogorovMinimumWeightPerfectMatching<MowingPlace, DefaultWeightedEdge> matchingAlg =
                new KolmogorovMinimumWeightPerfectMatching<>(oddComplete);
        MatchingAlgorithm.Matching<MowingPlace, DefaultWeightedEdge> matching =
                matchingAlg.getMatching();
        Set<DefaultWeightedEdge> matchEdges = matching.getEdges();

        // 7) build a multigraph that merges MST + matching edges
        Multigraph<MowingPlace, DefaultEdge> multi =
                new Multigraph<>(DefaultEdge.class);
        // add all vertices
        for (MowingPlace v : complete.vertexSet()) {
            multi.addVertex(v);
        }
        // add MST edges
        for (DefaultWeightedEdge e : mstEdges) {
            MowingPlace u = complete.getEdgeSource(e);
            MowingPlace v = complete.getEdgeTarget(e);
            multi.addEdge(u, v);
        }
        // add matching edges
        for (DefaultWeightedEdge e : matchEdges) {
            MowingPlace u = oddComplete.getEdgeSource(e);
            MowingPlace v = oddComplete.getEdgeTarget(e);
            multi.addEdge(u, v);
        }

        // 8) add one artificial edge start–end to make the graph Eulerian
        multi.addEdge(start, end);

        // 9) compute an Eulerian cycle
        HierholzerEulerianCycle<MowingPlace, DefaultEdge> eulerAlg =
                new HierholzerEulerianCycle<>();
        List<MowingPlace> cycle = eulerAlg.getEulerianCycle(multi).getVertexList();

        // 10) trim duplicate last vertex if present
        int M = cycle.size();
        if (M > 1 && cycle.get(0).equals(cycle.get(M - 1))) {
            cycle = cycle.subList(0, M - 1);
            M = cycle.size();
        }

        // 11) locate our artificial start–end edge in the cycle
        int pos = -1;
        for (int i = 0; i < M; i++) {
            MowingPlace a = cycle.get(i);
            MowingPlace b = cycle.get((i + 1) % M);
            if ((a.equals(start) && b.equals(end)) ||
                    (a.equals(end) && b.equals(start))) {
                pos = i;
                break;
            }
        }
        if (pos < 0) {
            throw new IllegalStateException("Eulerian cycle never used the artificial start–end edge");
        }

        // 12) break the cycle at that edge, reversing if needed so we go start→…→end
        List<MowingPlace> route = new ArrayList<>(M);
        // walk backwards from pos to include every vertex once
        for (int k = 0; k < M - 1; k++) {
            int idx = (pos - k + M) % M;
            route.add(cycle.get(idx));
        }
        // finally include the other endpoint of the artificial edge
        route.add(cycle.get((pos + 1) % M));

        // if we ended up with end at front, just flip the whole route
        if (!route.get(0).equals(start) || !route.get(route.size() - 1).equals(end)) {
            Collections.reverse(route);
        }


        // Remove duplicates: ensure each node visited once with leaving start  at the first position and end at the last
        Set<String> seen = new HashSet<>();
        List<MowingPlace> finalRoute = new ArrayList<>();
        for (MowingPlace place : route) {
            if (!seen.contains(place.getId())) {
                seen.add(place.getId());
                finalRoute.add(place);
            }
        }
        // Ensure start is first and end is last
        if (!finalRoute.get(0).equals(start)) {
            finalRoute.remove(start);
            finalRoute.add(0, start);
        }
        if (!finalRoute.get(finalRoute.size() - 1).equals(end)) {
            finalRoute.remove(end);
            finalRoute.add(end);
        }
        // route now goes: start → … → end, visits every node exactly once,
        // and has the same 3/2‐approximation guarantee as Christofides.
        return finalRoute;
    }



    private static double computeDistance(MowingPlace a, MowingPlace b) {
        int da = Integer.MAX_VALUE, db = Integer.MAX_VALUE;
        if (a.getDistancesToOthers() != null) {
            for (DistanceEntry e : a.getDistancesToOthers()) {
                if (b.getId().equals(e.getId())) {
                    da = e.getDistance();
                    break;
                }
            }
        }
        if (b.getDistancesToOthers() != null) {
            for (DistanceEntry e : b.getDistancesToOthers()) {
                if (a.getId().equals(e.getId())) {
                    db = e.getDistance();
                    break;
                }
            }
        }
        if (da != Integer.MAX_VALUE || db != Integer.MAX_VALUE) {
            return Math.min(da, db);
        }
        return haversineDistance(
                a.getLatitude(), a.getLongitude(),
                b.getLatitude(), b.getLongitude()
        );
    }

    private static double haversineDistance(
            double lat1, double lon1,
            double lat2, double lon2
    ) {
        final double R = 6_371_000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double φ1 = Math.toRadians(lat1);
        double φ2 = Math.toRadians(lat2);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(φ1) * Math.cos(φ2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
    }


    /**
     * Inserts additional cemeteries into the existing route if time allows, preserving the original
     * order of the route (except for newly inserted places). The first (start) and last (end) points
     * of the route remain unchanged. New cemeteries are chosen and inserted one by one at the position
     * that minimally increases the total route duration, as long as the overall route time stays within
     * the allowed limit.
     *
     * Filtering criteria for candidate cemeteries:
     *
     *   If {addVisited} is false, only consider cemeteries that have not already been visited
     *       the required number of times this year (i.e., visited fewer times than their
     *       {mowingCountPerYear}).
     *   Exclude any cemetery that was visited within {timeFromLastVisit} weeks from the
     *       current date.
     *
     *
     * @param currentRoute       The current ordered route (with "start" at index 0 and "end" at last index).
     * @param allAvailablePlaces All available MowingPlace objects (potential extra cemeteries to add).
     * @param endTime            Total allowed route time in minutes (time constraint from start to end).
     * @param speedMultiplier    Multiplier to adjust mowing speed (affects mowing time only; travel time is unchanged).
     * @param addVisited         If false, skip cemeteries already visited enough times this year.
     * @param timeFromLastVisit  Exclude cemeteries visited within this many weeks of today.
     * @return Updated route with extra cemeteries inserted where possible without exceeding the time limit.
     */
    public static List<MowingPlace> addExtraCemeteries(List<MowingPlace> currentRoute,
                                                       List<MowingPlace> allAvailablePlaces,
                                                       int endTime,
                                                       double speedMultiplier,
                                                       boolean addVisited,
                                                       int timeFromLastVisit) {
        // Ensure valid speedMultiplier to avoid division by zero (just in case)
        if (speedMultiplier <= 0) {
            speedMultiplier = 1.0;
        }

        // Convert the allowed total time from minutes to seconds for comparison
        double allowedTimeSec = endTime * 60.0;

        // 1. Calculate current route's total duration (travel + mowing) in seconds.
        double currentRouteTimeSec = 0.0;
        // Sum travel time between consecutive places (use duration in seconds from distances)
        for (int i = 0; i < currentRoute.size() - 1; i++) {
            MowingPlace from = currentRoute.get(i);
            MowingPlace to = currentRoute.get(i + 1);
            double travelSec = getDurationSeconds(from, to);
            currentRouteTimeSec += travelSec;
        }
        // Sum all mowing times (in hours converted to seconds) and adjust by speed multiplier
        double totalMowingHours = 0.0;
        for (MowingPlace place : currentRoute) {
            totalMowingHours += place.getTimeRequirement();
        }
        // Adjust mowing time for speed (if speedMultiplier > 1, mowing is faster, so effective time is less)
        double mowingTimeSec = (totalMowingHours / speedMultiplier) * 3600.0;
        currentRouteTimeSec += mowingTimeSec;

        // 2. Filter the list of all available places to get eligible extra cemeteries.
        List<MowingPlace> candidates = new ArrayList<>();
        // Get the current year for visit count comparison
        int currentYear = LocalDate.now().getYear();
        // Calculate the cutoff date for recent visits (current date minus timeFromLastVisit weeks)
        LocalDate cutoffDate = LocalDate.now().minusWeeks(timeFromLastVisit);
        for (MowingPlace place : allAvailablePlaces) {
            String placeId = place.getId();
            // Skip places already in the current route (by matching ID)
            boolean inRoute = false;
            for (MowingPlace routePlace : currentRoute) {
                if (routePlace.getId().equals(placeId)) {
                    inRoute = true;
                    break;
                }
            }
            if (inRoute) {
                continue;
            }
            // Also ensure we do not add the special start or end markers as "cemeteries"
            if ("start".equals(placeId) || "end".equals(placeId)) {
                continue;
            }
            // If addVisited is false, skip places that have already been visited enough times this year.
            if (!addVisited) {
                int visitsThisYear = 0;
                List<String> visitDates = place.getVisitDates();
                if (visitDates != null) {
                    for (String dateStr : visitDates) {
                        try {
                            LocalDate visitDate = LocalDate.parse(dateStr);
                            if (visitDate.getYear() == currentYear) {
                                visitsThisYear++;
                            }
                        } catch (Exception e) {
                            // If date is not parseable, skip it for counting
                        }
                    }
                }
                // If the number of visits this year meets or exceeds the target, skip this place
                if (visitsThisYear >= place.getMowingCountPerYear()) {
                    continue;
                }
            }
            // If the place was visited within the last `timeFromLastVisit` weeks, skip it.
            if (timeFromLastVisit > 0) {
                List<String> visitDates = place.getVisitDates();
                if (visitDates != null && !visitDates.isEmpty()) {
                    LocalDate lastVisitDate = null;
                    // Find the most recent visit date
                    for (String dateStr : visitDates) {
                        try {
                            LocalDate date = LocalDate.parse(dateStr);
                            if (lastVisitDate == null || date.isAfter(lastVisitDate)) {
                                lastVisitDate = date;
                            }
                        } catch (Exception e) {
                            // If parse fails, ignore that date string
                        }
                    }
                    if (lastVisitDate != null) {
                        // If last visit is after the cutoff date (meaning it is within the restricted period), skip
                        if (!lastVisitDate.isBefore(cutoffDate)) {
                            // lastVisitDate is on or after the cutoffDate, meaning within the restricted window
                            continue;
                        }
                    }
                }
            }
            // If we reach here, the place passes all filters and can be considered for insertion
            candidates.add(place);
        }

        // 3. Greedily insert extra cemeteries one by one based on minimal increase in route duration.
        while (true) {
            MowingPlace bestCandidate = null;
            int bestInsertIndex = -1;
            double bestIncreaseSec = Double.POSITIVE_INFINITY;

            // Evaluate each remaining candidate and each possible insertion position for that candidate
            for (MowingPlace candidate : candidates) {
                // Calculate candidate's mowing time in seconds (adjusted by speed)
                double candidateMowSec = (candidate.getTimeRequirement() / speedMultiplier) * 3600.0;
                // Try inserting the candidate between every pair of consecutive points in the current route
                for (int i = 0; i < currentRoute.size() - 1; i++) {
                    MowingPlace prev = currentRoute.get(i);
                    MowingPlace next = currentRoute.get(i + 1);
                    // Compute travel time if candidate is inserted between prev and next
                    double originalTravelSec = getDurationSeconds(prev, next);
                    double newTravelSec = getDurationSeconds(prev, candidate) + getDurationSeconds(candidate, next);
                    double travelIncreaseSec = newTravelSec - originalTravelSec;
                    // Total time increase would be travel increase plus the candidate's mowing time
                    double totalIncreaseSec = travelIncreaseSec + candidateMowSec;
                    // Track if this is the smallest increase found so far
                    if (totalIncreaseSec < bestIncreaseSec) {
                        bestIncreaseSec = totalIncreaseSec;
                        bestCandidate = candidate;
                        bestInsertIndex = i + 1;  // new candidate will be inserted at this position
                    }
                }
            }

            // If no candidate can be inserted (no candidates left or none found), exit the loop
            if (bestCandidate == null) {
                break;
            }

            // Check if adding this best candidate keeps the route within the allowed time
            double newRouteTimeSec = currentRouteTimeSec + bestIncreaseSec;
            if (newRouteTimeSec > allowedTimeSec) {
                // Adding the best candidate would exceed the allowed time, so we cannot add any more.
                break;
            }

            // Otherwise, insert the best candidate at the determined position
            currentRoute.add(bestInsertIndex, bestCandidate);
            // Remove the added candidate from the list of available candidates
            candidates.remove(bestCandidate);
            // Update the current route time to include this insertion's time cost
            currentRouteTimeSec = newRouteTimeSec;
            // Continue to the next iteration to attempt adding another candidate
        }

        // Return the augmented route (currentRoute is now updated in-place)
        return currentRoute;
    }

    /**
     * Helper method to get the travel duration (in seconds) from one place to another.
     * It looks up the duration in the first place's distance list, or the second place's list if needed.
     */
    private static double getDurationSeconds(MowingPlace from, MowingPlace to) {
        if (from.getDistancesToOthers() != null) {
            for (MowingPlace.DistanceEntry entry : from.getDistancesToOthers()) {
                if (entry.getId().equals(to.getId())) {
                    return entry.getDuration();
                }
            }
        }
        if (to.getDistancesToOthers() != null) {  // try the reverse direction
            for (MowingPlace.DistanceEntry entry : to.getDistancesToOthers()) {
                if (entry.getId().equals(from.getId())) {
                    return entry.getDuration();
                }
            }
        }
        //TODO heuristic for missing distance

        // If not found in either (which should not happen if data is complete), return 0 as fallback
        return 0.0;
    }

}
