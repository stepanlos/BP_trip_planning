package com.example.myapplication.ui.planning;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.myapplication.data.MowingPlace;
import com.example.myapplication.data.MowingPlacesRepository;
import com.example.myapplication.databinding.FragmentPlanningBinding;
import com.example.myapplication.util.TSPPlanner;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import java.util.ArrayList;
import java.util.List;

public class PlanningFragment extends Fragment {

    private FragmentPlanningBinding binding;
    private MapView planningMapView;

    // UI elements for inputs
    private EditText etStartLocation, etEndLocation;
    private TextView tvStartTime, tvEndTime;
    private SeekBar sbSpeedMultiplier;
    private CheckBox cbAddExtra;
    private Button btnGenerateRoute, btnOpenMapycZ;

    // Repository to load available cemeteries (MowingPlaces)
    private MowingPlacesRepository placesRepository;
    private List<MowingPlace> availablePlaces;

    // Default time constraints (in minutes from midnight)
    private int startTimeInMinutes = 360; // default 06:00
    private int endTimeInMinutes = 1140;  // default 19:00

    // Route plan variables
    private List<MowingPlace> finalRoute;
    private double totalMowingTime; // computed route time (driving + mowing)

    private String mapyCzRouteUrl = "";

    private static final int REQUEST_CODE_START = 101;
    private static final int REQUEST_CODE_END = 102;

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
        planningMapView = binding.planningMapView;

        // Hide "Open in Mapy.cz" button until a route is generated
        btnOpenMapycZ.setVisibility(View.GONE);

        // Initialize map view
        planningMapView.setMultiTouchControls(true);
        planningMapView.getController().setZoom(7.0);
        planningMapView.getController().setCenter(new GeoPoint(49.8175, 15.4730));

        // Initialize repository and load available places (cemeteries)
        placesRepository = new MowingPlacesRepository();
        availablePlaces = placesRepository.loadMowingPlaces(getContext());

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
                Log.d("PlanningFragment", "Speed multiplier changed to: " + multiplier);
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



        // Set up button listeners for generating route and opening Mapy.cz
        btnGenerateRoute.setOnClickListener(v -> generateRoute());
        btnOpenMapycZ.setOnClickListener(v -> openMapyCz());

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

    // Generate route using TSPPlanner (stub implementation)
    private void generateRoute() {
        // Parse start and end location input (format "lat,lon")
        String startLocStr = etStartLocation.getText().toString().trim();
        String endLocStr = etEndLocation.getText().toString().trim();
        if (TextUtils.isEmpty(startLocStr) || TextUtils.isEmpty(endLocStr)) {
            Toast.makeText(getContext(), "Please select start and end locations", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] startParts = startLocStr.split(",");
        String[] endParts = endLocStr.split(",");
        if (startParts.length != 2 || endParts.length != 2) {
            Toast.makeText(getContext(), "Location format must be 'lat,lon'", Toast.LENGTH_SHORT).show();
            return;
        }
        double startLat = Double.parseDouble(startParts[0].trim());
        double startLon = Double.parseDouble(startParts[1].trim());
        double endLat = Double.parseDouble(endParts[0].trim());
        double endLon = Double.parseDouble(endParts[1].trim());

        // Get time constraints from selected times (in minutes)
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

        // For simplicity, assume mandatory cemeteries are all available places
        List<MowingPlace> mandatoryPlaces = new ArrayList<>(availablePlaces);

        // Build complete list: start, mandatory, end
        List<MowingPlace> nodes = new ArrayList<>();
        nodes.add(startPlace);
        nodes.addAll(mandatoryPlaces);
        nodes.add(endPlace);

        // Generate route using TSP algorithm
        finalRoute = TSPPlanner.generateRoute(nodes, speedMultiplier);
        // Optionally add extra cemeteries if checkbox is checked
        if (cbAddExtra.isChecked()) {
            finalRoute = TSPPlanner.addExtraCemeteries(finalRoute, availablePlaces, endTime - startTime, speedMultiplier);
        }
        // Compute total route time (stub: sum of timeRequirement)
        totalMowingTime = 0;
        for (MowingPlace mp : finalRoute) {
            totalMowingTime += mp.getTimeRequirement();
        }
        totalMowingTime /= speedMultiplier;

        mapyCzRouteUrl = generateMapyUrl(finalRoute);

        // Update map preview with polyline
        updateMapPreview(finalRoute);
        // Show the "Open in Mapy.cz" button
        btnOpenMapycZ.setVisibility(View.VISIBLE);
        //total mowing time with one decimal in hours
        double mowingTimeInHours = Math.floor(totalMowingTime / 60.0 * 10) / 10;
        Toast.makeText(getContext(), "Route generated. Total mowing time: " + mowingTimeInHours + " h", Toast.LENGTH_LONG).show();
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

    // Generate Mapy.cz URL from the route according to required format and open it in browser
    private void openMapyCz() {
        if (finalRoute == null || finalRoute.isEmpty()) {
            Toast.makeText(getContext(), "No route generated", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = generateMapyUrl(finalRoute);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    // Build Mapy.cz URL with parameters: start, end, routeType, waypoints, navigate
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

    // Helper method to show TimePicker dialog
    private void showTimePicker(int initialMinutes, TimePickerCallback callback) {
        int initialHour = initialMinutes / 60;
        int initialMinute = initialMinutes % 60;
        new android.app.TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            callback.onTimeSelected(hourOfDay, minute);
        }, initialHour, initialMinute, true).show();
    }
}
