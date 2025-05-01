package com.example.myapplication.util;

import com.example.myapplication.data.MowingPlace;
import com.example.myapplication.data.MowingPlace.DistanceEntry;
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

    public static List<MowingPlace> generateRoute(List<MowingPlace> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }
        // Najdi start a end
        MowingPlace start = null, end = null;
        for (MowingPlace p : nodes) {
            if ("start".equals(p.getId())) start = p;
            else if ("end".equals(p.getId())) end = p;
        }
        if (start == null || end == null) {
            // Bez pevného startu/endu fallback na původní pořadí
            return new ArrayList<>(nodes);
        }

        // Dummy uzel
        MowingPlace dummy = new MowingPlace();
        dummy.setId("dummy");

        // 1) Vytvoř graf a přidej vrcholy
        Graph<MowingPlace, DefaultWeightedEdge> graph =
                new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        for (MowingPlace p : nodes) {
            graph.addVertex(p);
        }
        graph.addVertex(dummy);

        // 2) Přidej vážené hrany (kompletní graf)
        List<MowingPlace> all = new ArrayList<>(nodes);
        all.add(dummy);
        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                MowingPlace u = all.get(i), v = all.get(j);
                double w;
                // dummy↔start a dummy↔end mají váhu 0
                if ((u == dummy && (v == start || v == end)) ||
                        (v == dummy && (u == start || u == end))) {
                    w = 0.0;
                } else {
                    w = computeDistance(u, v);
                }
                DefaultWeightedEdge e = graph.addEdge(u, v);
                graph.setEdgeWeight(e, w);
            }
        }

        // 3) Spusť Christofidesovu 1.5-approximaci
        ChristofidesThreeHalvesApproxMetricTSP<MowingPlace, DefaultWeightedEdge> solver =
                new ChristofidesThreeHalvesApproxMetricTSP<>();
        GraphPath<MowingPlace, DefaultWeightedEdge> tour = solver.getTour(graph);

        // 4) Z cyklu odstraň dummy na začátku i konci → dostaneš path start→…→end
        List<MowingPlace> ordered = new ArrayList<>(tour.getVertexList());
        if (!ordered.isEmpty() && ordered.get(0).equals(dummy)) {
            ordered.remove(0);
        }
        if (!ordered.isEmpty() && ordered.get(ordered.size() - 1).equals(dummy)) {
            ordered.remove(ordered.size() - 1);
        }
        return ordered;
    }

    // Pomocná funkce pro metrickou váhu (zdroj: původní Haversine + distance list)
    private static double computeDistance(MowingPlace a, MowingPlace b) {
        if (a.getDistancesToOthers() != null) {
            for (DistanceEntry de : a.getDistancesToOthers()) {
                if (de.getId().equals(b.getId())) {
                    return de.getDistance();
                }
            }
        }
        if (b.getDistancesToOthers() != null) {
            for (DistanceEntry de : b.getDistancesToOthers()) {
                if (de.getId().equals(a.getId())) {
                    return de.getDistance();
                }
            }
        }
        return haversineDistance(a.getLatitude(), a.getLongitude(),
                b.getLatitude(), b.getLongitude());
    }

    // Původní Haversine
    private static double haversineDistance(double lat1, double lon1,
                                            double lat2, double lon2) {
        final double R = 6371000.0;
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLam = Math.toRadians(lon2 - lon1);
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(dLam / 2) * Math.sin(dLam / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }



    /**
     * Inserts additional cemeteries into the existing route if time allows, preserving the original
     * order of the route (except for newly inserted places). The first (start) and last (end) points
     * of the route remain unchanged. New cemeteries are chosen and inserted one by one at the position
     * that minimally increases the total route duration, as long as the overall route time stays within
     * the allowed limit.
     *
     * <p>Filtering criteria for candidate cemeteries:
     * <ul>
     *   <li>If {@code addVisited} is false, only consider cemeteries that have not already been visited
     *       the required number of times this year (i.e., visited fewer times than their
     *       {@code mowingCountPerYear}).</li>
     *   <li>Exclude any cemetery that was visited within {@code timeFromLastVisit} weeks from the
     *       current date.</li>
     * </ul>
     *
     * @param currentRoute      The current ordered route (with "start" at index 0 and "end" at last index).
     * @param allAvailablePlaces All available MowingPlace objects (potential extra cemeteries to add).
     * @param endTime           Total allowed route time in minutes (time constraint from start to end).
     * @param speedMultiplier   Multiplier to adjust mowing speed (affects mowing time only; travel time is unchanged).
     * @param addVisited        If false, skip cemeteries already visited enough times this year.
     * @param timeFromLastVisit Exclude cemeteries visited within this many weeks of today.
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
