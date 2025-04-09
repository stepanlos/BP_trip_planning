package com.example.myapplication.ui.planning;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.data.MowingPlace;
import com.example.myapplication.data.MowingPlacesRepository;
import com.example.myapplication.databinding.FragmentPlanningBinding;
import com.example.myapplication.ui.planning.LocationPickerActivity;
import com.example.myapplication.util.TSPPlanner;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlanningFragment extends Fragment {

    private FragmentPlanningBinding binding;
    private MapView planningMapView;

    // UI elements for location and time inputs
    private EditText etStartLocation, etEndLocation;
    private TextView tvStartTime, tvEndTime;
    private SeekBar sbSpeedMultiplier;
    private CheckBox cbAddExtra;
    private Button btnGenerateRoute, btnOpenMapycZ, btnAddWaypoint;

    // Container for dynamic waypoint entries
    private ViewGroup llWaypoints;

    // Repository for available cemeteries and mapping from name to MowingPlace
    private MowingPlacesRepository placesRepository;
    private List<MowingPlace> availablePlaces;
    private Map<String, MowingPlace> nameToPlace;

    // Default time constraints (in minutes from midnight)
    private int startTimeInMinutes = 360; // default 06:00
    private int endTimeInMinutes = 1140;  // default 19:00

    // Route plan variables
    private List<MowingPlace> finalRoute;
    private double totalMowingTime; // computed total time (driving + mowing)
    private String mapyCzRouteUrl = "";

    private static final int REQUEST_CODE_START = 101;
    private static final int REQUEST_CODE_END = 102;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPlanningBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize UI components
        etStartLocation = binding.etStartLocation;
        etEndLocation = binding.etEndLocation;
        tvStartTime = binding.tvStartTime;
        tvEndTime = binding.tvEndTime;
        sbSpeedMultiplier = binding.seekBarSpeedMultiplier;
        cbAddExtra = binding.cbAddExtra;
        btnGenerateRoute = binding.btnGenerateRoute;
        btnOpenMapycZ = binding.btnOpenMapycZ;
        btnAddWaypoint = binding.btnAddWaypoint;
        planningMapView = binding.planningMapView;
        llWaypoints = binding.llWaypoints;

        // Hide "Open in Mapy.cz" button until a route is generated
        btnOpenMapycZ.setVisibility(View.GONE);

        // Initialize map view
        planningMapView.setMultiTouchControls(true);
        planningMapView.getController().setZoom(7.0);
        planningMapView.getController().setCenter(new GeoPoint(49.8175, 15.4730));

        // Initialize repository and load available places (cemeteries)
        placesRepository = new MowingPlacesRepository();
        availablePlaces = placesRepository.loadMowingPlaces(getContext());
        // Build a mapping from cemetery name to MowingPlace for auto complete suggestions (assume unique names)
        nameToPlace = new ArrayMap<>();
        List<String> placeNames = new ArrayList<>();
        for (MowingPlace mp : availablePlaces) {
            nameToPlace.put(mp.getName(), mp);
            placeNames.add(mp.getName());
        }

        // Set default times and update TextViews
        tvStartTime.setText(formatTime(startTimeInMinutes));
        tvEndTime.setText(formatTime(endTimeInMinutes));

        // Set up time picker listeners for start and end time
        tvStartTime.setOnClickListener(v -> {
            showTimePicker(startTimeInMinutes, (hour, minute) -> {
                startTimeInMinutes = hour * 60 + minute;
                tvStartTime.setText(formatTime(startTimeInMinutes));
            });
        });
        tvEndTime.setOnClickListener(v -> {
            showTimePicker(endTimeInMinutes, (hour, minute) -> {
                endTimeInMinutes = hour * 60 + minute;
                tvEndTime.setText(formatTime(endTimeInMinutes));
            });
        });

        // Set up SeekBar for speed multiplier (multiplier = 0.5 + progress*0.5, max=5)
        sbSpeedMultiplier.setMax(5);
        sbSpeedMultiplier.setProgress(1);
        double speedMultiplier = 0.5 + (sbSpeedMultiplier.getProgress() * 0.5);
        binding.tvSpeedMultiplierValue.setText("Multiplier: " + speedMultiplier + " x");
        sbSpeedMultiplier.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double multiplier = 0.5 + (progress * 0.5);
                binding.tvSpeedMultiplierValue.setText("Multiplier: " + multiplier + "x");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        // Set up location picker for start and end location fields
        etStartLocation.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), LocationPickerActivity.class);
            startActivityForResult(intent, REQUEST_CODE_START);
        });
        etEndLocation.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), LocationPickerActivity.class);
            startActivityForResult(intent, REQUEST_CODE_END);
        });

        // Set up Add Waypoint button to add a new searchable dropdown for mandatory waypoints
        btnAddWaypoint.setOnClickListener(v -> addWaypointEntry());

        // Set up button listeners for generating route and opening Mapy.cz
        btnGenerateRoute.setOnClickListener(v -> generateRoute());
        // Corrected listener for openMapyCz()
        btnOpenMapycZ.setOnClickListener(v -> openMapyCz());

        planningMapView.setOnTouchListener((v, event) -> {
            // Request parent (NestedScrollView) not to intercept touch events
            v.getParent().requestDisallowInterceptTouchEvent(true);
            // Return false so that map can handle events as usual
            return false;
        });

        return root;
    }

    // Handle results from LocationPickerActivity
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == getActivity().RESULT_OK && data != null) {
            double lat = data.getDoubleExtra(LocationPickerActivity.EXTRA_SELECTED_LAT, 0);
            double lon = data.getDoubleExtra(LocationPickerActivity.EXTRA_SELECTED_LON, 0);
            String coordinates = lat + "," + lon;
            if(requestCode == REQUEST_CODE_START) {
                etStartLocation.setText(coordinates);
            } else if(requestCode == REQUEST_CODE_END) {
                etEndLocation.setText(coordinates);
            }
        }
    }

    // Adds a new waypoint entry row to the dynamic container (llWaypoints)
    private void addWaypointEntry() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View waypointView = inflater.inflate(R.layout.waypoint_item, llWaypoints, false);
        AutoCompleteTextView actvWaypoint = waypointView.findViewById(R.id.actvWaypoint);
        ImageButton btnRemove = waypointView.findViewById(R.id.btnRemoveWaypoint);

        // Create adapter for suggestions (using available place names)
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, new ArrayList<>(nameToPlace.keySet()));
        actvWaypoint.setThreshold(1);
        actvWaypoint.setAdapter(adapter);

        // On item click, set tag with corresponding MowingPlace object
        actvWaypoint.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = (String) parent.getItemAtPosition(position);
            if (nameToPlace.containsKey(selectedName)) {
                actvWaypoint.setTag(nameToPlace.get(selectedName));
            }
        });

        // Remove button listener
        btnRemove.setOnClickListener(v -> llWaypoints.removeView(waypointView));

        // Add the new waypoint view to container
        llWaypoints.addView(waypointView);
    }

    // Generate route using TSPPlanner with mandatory waypoints selected by user
    private void generateRoute() {
        // Parse start and end location input (format "lat,lon")
        String startLocStr = etStartLocation.getText().toString().trim();
        String endLocStr = etEndLocation.getText().toString().trim();
        if (TextUtils.isEmpty(startLocStr) || TextUtils.isEmpty(endLocStr)) {
            Toast.makeText(getContext(), "Prosím, vyberte start a end lokaci", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] startParts = startLocStr.split(",");
        String[] endParts = endLocStr.split(",");
        if (startParts.length != 2 || endParts.length != 2) {
            Toast.makeText(getContext(), "Formát lokace musí být 'lat,lon'", Toast.LENGTH_SHORT).show();
            return;
        }
        double startLat = Double.parseDouble(startParts[0].trim());
        double startLon = Double.parseDouble(startParts[1].trim());
        double endLat = Double.parseDouble(endParts[0].trim());
        double endLon = Double.parseDouble(endParts[1].trim());

        // Get time constraints (in minutes)
        int startTime = startTimeInMinutes;
        int endTime = endTimeInMinutes;
        // Get speed multiplier from SeekBar (0.5 + progress*0.5)
        double speedMultiplier = 0.5 + (sbSpeedMultiplier.getProgress() * 0.5);

        // Create special MowingPlace nodes for start and end
        MowingPlace startPlace = new MowingPlace();
        startPlace.setId("start");
        startPlace.setName("Start");
        startPlace.setLatitude(startLat);
        startPlace.setLongitude(startLon);
        startPlace.setTimeRequirement(0);

        MowingPlace endPlace = new MowingPlace();
        endPlace.setId("end");
        endPlace.setName("End");
        endPlace.setLatitude(endLat);
        endPlace.setLongitude(endLon);
        endPlace.setTimeRequirement(0);

        // Retrieve mandatory waypoints from dynamic container
        List<MowingPlace> mandatoryWaypoints = new ArrayList<>();
        int count = llWaypoints.getChildCount();
        for (int i = 0; i < count; i++) {
            View waypointView = llWaypoints.getChildAt(i);
            AutoCompleteTextView actv = waypointView.findViewById(R.id.actvWaypoint);
            Object tag = actv.getTag();
            if (tag instanceof MowingPlace) {
                mandatoryWaypoints.add((MowingPlace) tag);
            }
        }

        // Build complete list: start, mandatory waypoints (in order), end
        List<MowingPlace> nodes = new ArrayList<>();
        nodes.add(startPlace);
        nodes.addAll(mandatoryWaypoints);
        nodes.add(endPlace);

        // Generate route using TSP algorithm (Christofides–Serdyukov)
        finalRoute = TSPPlanner.generateRoute(nodes);
        // Optionally add extra cemeteries if checkbox is checked
        if (cbAddExtra.isChecked()) {
            finalRoute = TSPPlanner.addExtraCemeteries(finalRoute, availablePlaces, endTime - startTime, speedMultiplier);
        }
        // Compute total mowing time (stub: sum of timeRequirement divided by speed multiplier)
        totalMowingTime = 0;
        for (MowingPlace mp : finalRoute) {
            totalMowingTime += mp.getTimeRequirement();
        }
        totalMowingTime /= speedMultiplier;

        // Generate Mapy.cz route URL
        mapyCzRouteUrl = generateMapyUrl(finalRoute);

        // Update map preview with polyline
        updateMapPreview(finalRoute);
        // Show the "Open in Mapy.cz" button
        btnOpenMapycZ.setVisibility(View.VISIBLE);
        Toast.makeText(getContext(), "Trasa vygenerována. Celkový čas sekání: " + totalMowingTime + " h", Toast.LENGTH_LONG).show();
    }

    // Draw a polyline on the map connecting the route stops
    private void updateMapPreview(List<MowingPlace> route) {
        planningMapView.getOverlays().clear();
        Polyline polyline = new Polyline();
        List<GeoPoint> geoPoints = new ArrayList<>();
        for (MowingPlace mp : route) {
            geoPoints.add(new GeoPoint(mp.getLatitude(), mp.getLongitude()));
        }
        polyline.setPoints(geoPoints);
        planningMapView.getOverlays().add(polyline);
        planningMapView.invalidate();
    }

    // Generate Mapy.cz URL according to required format:
    // Example: https://mapy.cz/fnc/v1/route?mapset=traffic&start=lon,lat&end=lon,lat&routeType=car_fast&waypoints=lon,lat;lon,lat&navigate=true
    private String generateMapyUrl(List<MowingPlace> route) {
        if (route.size() < 2) {
            return "";
        }
        // First and last nodes are start and end
        MowingPlace start = route.get(0);
        MowingPlace end = route.get(route.size() - 1);
        StringBuilder waypointsBuilder = new StringBuilder();
        for (int i = 1; i < route.size() - 1; i++) {
            MowingPlace mp = route.get(i);
            if (waypointsBuilder.length() > 0) {
                waypointsBuilder.append(";");
            }
            // Order: longitude,latitude
            waypointsBuilder.append(mp.getLongitude()).append(",").append(mp.getLatitude());
        }
        String url = "https://mapy.cz/fnc/v1/route?mapset=traffic";
        url += "&start=" + start.getLongitude() + "," + start.getLatitude();
        url += "&end=" + end.getLongitude() + "," + end.getLatitude();
        url += "&routeType=car_fast";
        if (waypointsBuilder.length() > 0) {
            url += "&waypoints=" + waypointsBuilder.toString();
        }
        return url;
    }

    // Format time in HH:mm format
    private String formatTime(int totalMinutes) {
        int hour = totalMinutes / 60;
        int minute = totalMinutes % 60;
        return String.format("%02d:%02d", hour, minute);
    }

    // Interface for time picker callback
    public interface TimePickerCallback {
        void onTimeSelected(int hour, int minute);
    }

    // Helper method for showing TimePicker dialog
    private void showTimePicker(int initialMinutes, TimePickerCallback callback) {
        int initialHour = initialMinutes / 60;
        int initialMinute = initialMinutes % 60;
        new android.app.TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            callback.onTimeSelected(hourOfDay, minute);
        }, initialHour, initialMinute, true).show();
    }

    // Method to open Mapy.cz route using the generated URL
    private void openMapyCz() {
        if (mapyCzRouteUrl == null || mapyCzRouteUrl.isEmpty()) {
            Toast.makeText(getContext(), "Trasa není vygenerována", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapyCzRouteUrl));
        startActivity(intent);
    }
}
