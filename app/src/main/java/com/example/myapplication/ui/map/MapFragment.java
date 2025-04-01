package com.example.myapplication.ui.map;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.data.MowingPlace;
import com.example.myapplication.databinding.FragmentMapBinding;
import com.example.myapplication.ui.detail.PlaceDetailActivity;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

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
        // Activation of multi touch control (zoom gestures, rotation, etc.)
        mapView.setMultiTouchControls(true);

        // Zoom and position (approx. center of Czech Republic)
        mapView.getController().setZoom(7.0);
        mapView.getController().setCenter(new GeoPoint(49.8175, 15.4730));

        // Observing the data from the ViewModel
        mapViewModel.getPlaces().observe(getViewLifecycleOwner(), this::updateMapMarkers);

        return root;
    }

    private void updateMapMarkers(List<MowingPlace> places) {
        // Delete old markers (if reloading)
        mapView.getOverlays().clear();

        for (MowingPlace place : places) {
            GeoPoint point = new GeoPoint(place.getLatitude(), place.getLongitude());
            Marker marker = new Marker(mapView);
            marker.setPosition(point);
            marker.setTitle(place.getName());

            marker.setOnMarkerClickListener((m, mapView) -> {
                showPlaceDetail(place);
                return true;
            });

            mapView.getOverlays().add(marker);
        }

        // Refresh map
        mapView.invalidate();
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
