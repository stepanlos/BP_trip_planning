package com.example.myapplication.ui.planning;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

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
    private PlanningViewModel planningViewModel;
    private MapView planningMapView;

    // UI elements for inputs
    private EditText etStartLocation, etStartTime, etEndLocation, etEndTime, etSpeedMultiplier;
    private CheckBox cbAddExtra;
    private Button btnGenerateRoute, btnOpenMapycZ;

    // Repository to load available cemeteries (MowingPlaces)
    private MowingPlacesRepository placesRepository;
    private List<MowingPlace> availablePlaces;

    // Route plan variables
    private List<MowingPlace> finalRoute;
    private double totalRouteTime; // computed route time (driving + mowing)

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPlanningBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize UI components
        etStartLocation = binding.etStartLocation;
        etStartTime = binding.etStartTime;
        etEndLocation = binding.etEndLocation;
        etEndTime = binding.etEndTime;
        etSpeedMultiplier = binding.etSpeedMultiplier;
        cbAddExtra = binding.cbAddExtra;
        btnGenerateRoute = binding.btnGenerateRoute;
        btnOpenMapycZ = binding.btnOpenMapycZ;
        planningMapView = binding.planningMapView;

        // Initialize map view
        planningMapView.setMultiTouchControls(true);
        planningMapView.getController().setZoom(7.0);
        planningMapView.getController().setCenter(new GeoPoint(49.8175, 15.4730));

        // Initialize repository and load available places (cemeteries)
        placesRepository = new MowingPlacesRepository();
        availablePlaces = placesRepository.loadMowingPlaces(getContext());

        // Set up button listeners
        btnGenerateRoute.setOnClickListener(v -> generateRoute());
        btnOpenMapycZ.setOnClickListener(v -> openMapycZ());

        return root;
    }

    // Generate route using TSPPlanner (stub implementation)
    private void generateRoute() {
        // Parse start location input (expected format "lat,lon")
        String startLocStr = etStartLocation.getText().toString().trim();
        String endLocStr = etEndLocation.getText().toString().trim();
        if (TextUtils.isEmpty(startLocStr) || TextUtils.isEmpty(endLocStr)) {
            Toast.makeText(getContext(), "Please enter start and end locations", Toast.LENGTH_SHORT).show();
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

        // Parse time constraints and speed multiplier
        int startTime = Integer.parseInt(etStartTime.getText().toString().trim());
        int endTime = Integer.parseInt(etEndTime.getText().toString().trim());
        double speedMultiplier = Double.parseDouble(etSpeedMultiplier.getText().toString().trim());

        // Create special MowingPlace nodes for start and end
        MowingPlace startPlace = new MowingPlace();
        startPlace.setId("start");
        startPlace.setName("Start");
        startPlace.setLatitude(startLat);
        startPlace.setLongitude(startLon);
        // Set mowing time equal to startTime (or 0 if not applicable)
        startPlace.setTimeRequirement(startTime);

        MowingPlace endPlace = new MowingPlace();
        endPlace.setId("end");
        endPlace.setName("End");
        endPlace.setLatitude(endLat);
        endPlace.setLongitude(endLon);
        endPlace.setTimeRequirement(endTime);

        // For simplicity, assume that the mandatory cemeteries are already selected.
        // Here, we use all available places as mandatory (in real app, user selection is needed)
        List<MowingPlace> mandatoryPlaces = new ArrayList<>(availablePlaces);

        // Build the complete list: start, mandatory, end
        List<MowingPlace> nodes = new ArrayList<>();
        nodes.add(startPlace);
        nodes.addAll(mandatoryPlaces);
        nodes.add(endPlace);

        // Generate route using TSP algorithm (stub implementation)
        finalRoute = TSPPlanner.generateRoute(nodes, speedMultiplier);
        // Optionally, if "add extra" is checked, try to add extra cemeteries if time remains
        if (cbAddExtra.isChecked()) {
            finalRoute = TSPPlanner.addExtraCemeteries(finalRoute, availablePlaces, endTime, speedMultiplier);
        }
        // For demonstration, compute total route time (stub: sum of timeRequirement)
        totalRouteTime = 0;
        for (MowingPlace mp : finalRoute) {
            totalRouteTime += mp.getTimeRequirement();
        }

        // Update map preview with polyline
        updateMapPreview(finalRoute);
        Toast.makeText(getContext(), "Route generated. Total time: " + totalRouteTime + " minutes", Toast.LENGTH_LONG).show();
    }

    // Draw a polyline on the map connecting the route stops
    private void updateMapPreview(List<MowingPlace> route) {
        // Remove previous overlays
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

    // Generate Mapy.cz URL and open in browser
    private void openMapycZ() {
        if (finalRoute == null || finalRoute.isEmpty()) {
            Toast.makeText(getContext(), "No route generated", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = generateMapyUrl(finalRoute);
        // Open URL in browser
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    // Generate Mapy.cz URL from the route (format: ...&rc=lat1,lon1|lat2,lon2|...)
    private String generateMapyUrl(List<MowingPlace> route) {
        StringBuilder rcBuilder = new StringBuilder();
        for (MowingPlace mp : route) {
            if (rcBuilder.length() > 0) {
                rcBuilder.append("|");
            }
            rcBuilder.append(mp.getLatitude()).append(",").append(mp.getLongitude());
        }
        // Example URL: modify parameters as needed
        String baseUrl = "https://mapy.cz/zakladni?planovani-trasy=&rc=";
        return baseUrl + rcBuilder.toString();
    }
}
