package com.example.myapplication.ui.map;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.data.MowingPlace;
import com.example.myapplication.databinding.FragmentMapBinding;
import com.example.myapplication.ui.detail.PlaceDetailActivity;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;



public class MapFragment extends Fragment {

    private FragmentMapBinding binding;
    private MapViewModel mapViewModel;
    private MapView mapView;

    private ActivityResultLauncher<Intent> detailActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                    // Reload the data from the repository via ViewModel
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    mapViewModel.loadData();
                }
            }
    );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentMapBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // ViewModel
        mapViewModel = new ViewModelProvider(requireActivity()).get(MapViewModel.class);

        // Link MapView from layout
        mapView = binding.osmMapView;
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(7.8);
        mapView.getController().setCenter(new GeoPoint(49.8175, 15.0));

        // Observe places and update markers
        mapViewModel.getPlaces().observe(getViewLifecycleOwner(), this::updateMapMarkers);

        // Set up FloatingActionButton for adding a new place
        binding.fabAddPlace.setOnClickListener(v -> {
            // Launch PlaceDetailActivity in "create mode"
            Intent intent = new Intent(getContext(), PlaceDetailActivity.class);
            intent.putExtra(PlaceDetailActivity.EXTRA_NEW_PLACE, true);
            detailActivityLauncher.launch(intent);
        });

        return root;
    }

    private void updateMapMarkers(List<MowingPlace> places) {
        mapView.getOverlays().clear();

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1; // 1=Jan, … ,12=Dec

        for (MowingPlace place : places) {
            GeoPoint point = new GeoPoint(place.getLatitude(), place.getLongitude());
            Marker marker = new Marker(mapView);
            marker.setPosition(point);
            marker.setTitle(place.getName());

            // pick correct icon based on visit dates vs. mowing schedule
            int iconRes = pickMarkerIcon(place, currentYear, currentMonth);
            marker.setIcon(ContextCompat.getDrawable(requireContext(), iconRes));

            marker.setOnMarkerClickListener((m, map) -> {
                showPlaceDetail(place);
                return true;
            });

            mapView.getOverlays().add(marker);
        }

        mapView.invalidate();
    }

    /**
     * Decide which marker color to use:
     * - GREEN if visitsThisYear >= mowingCountPerYear
     * - otherwise RED or YELLOW depending on how many visits are missing in each mowing window
     */
    private int pickMarkerIcon(MowingPlace place, int year, int month) {
        // count how many visits happened THIS YEAR
        int visitsThisYear = 0;
        for (String date : place.getVisitDates()) {
            // date format is "YYYY-MM-DD"
            if (date.startsWith(String.valueOf(year))) {
                visitsThisYear++;
            }
        }

        int mowCount = place.getMowingCountPerYear();
        // green if done or overdone
        if (visitsThisYear >= mowCount) {
            return R.drawable.marker_green;
        }

        // Helper: we define mow‑windows as:
        // Window1 = May–June (months 5–6)
        // Window2 = July–August (7–8)
        // Window3 = September–December (9–12)

        // If place needs only 1 mow per year → mow in Window2
        if (mowCount == 1) {
            //if before june green
            if (month < 7) {
                return R.drawable.marker_green;
            }
            // not yet reached the window → yellow
            if (month < 9) {
                return R.drawable.marker_yellow;
            }
            // mow time or passed → red
            else {
                return R.drawable.marker_red;
            }
        }

        // If place needs 2 mows → mow in Window1 and Window3
        if (mowCount == 2) {
            if (visitsThisYear == 0) {
                // first mow in Window1
                //if before may green
                if (month < 5) {
                    return R.drawable.marker_green;
                }
                if (month < 7) {
                    return R.drawable.marker_yellow;
                } else {
                    return R.drawable.marker_red;
                }
            } else { // visitsThisYear == 1
                // second mow in Window3
                //if before september green
                if (month < 7) {
                    return R.drawable.marker_green;
                }
                if (month < 10) {
                    return R.drawable.marker_yellow;
                } else {
                    return R.drawable.marker_red;
                }
            }
        }

        // If place needs 3 mows → mow in Window1, Window2, Window3
        if (mowCount == 3) {
            if (visitsThisYear == 0) {
                // first mow in Window1
                //if before may green
                if (month < 5) {
                    return R.drawable.marker_green;
                }
                if (month < 7) {
                    return R.drawable.marker_yellow;
                } else {
                    return R.drawable.marker_red;
                }
            } else if (visitsThisYear == 1) {
                // second mow in Window2
                //if before july green
                if (month < 7) {
                    return R.drawable.marker_green;
                }
                if (month < 9) {
                    return R.drawable.marker_yellow;
                } else {
                    return R.drawable.marker_red;
                }
            } else { // visitsThisYear == 2
                // third mow in Window3
                //if before september green
                if (month < 8) {
                    return R.drawable.marker_green;
                }
                if (month < 9) {
                    return R.drawable.marker_yellow;
                } else {
                    return R.drawable.marker_red;
                }
            }
        }

        // fallback to yellow if mowingCountPerYear is outside 1–3
        return R.drawable.marker_yellow;
    }

    private void showPlaceDetail(MowingPlace place) {
        Intent intent = new Intent(getContext(), PlaceDetailActivity.class);
        intent.putExtra(PlaceDetailActivity.EXTRA_PLACE_ID, place.getId());
        detailActivityLauncher.launch(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume(); // important for osmdroid
        mapViewModel.loadData(); // reload data when fragment is resumed
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause(); // important for osmdroid
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
