package com.example.myapplication.ui.map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.data.MowingPlace;
import com.example.myapplication.databinding.FragmentMapBinding;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.List;

public class MapFragment extends Fragment {

    private FragmentMapBinding binding;
    private MapViewModel mapViewModel;
    private MapView mapView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentMapBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // ViewModel
        mapViewModel = new ViewModelProvider(requireActivity()).get(MapViewModel.class);

        // Odkaz na MapView z layoutu
        mapView = binding.osmMapView;
        // Aktivace multi-touch ovládání (zoom gesty, otáčení, atd.)
        mapView.setMultiTouchControls(true);

        // Přiblížení a pozice (cca střed ČR)
        mapView.getController().setZoom(7.0);
        mapView.getController().setCenter(new GeoPoint(49.8175, 15.4730));

        // Pozorujeme data z ViewModelu
        mapViewModel.getPlaces().observe(getViewLifecycleOwner(), this::updateMapMarkers);

        return root;
    }

    private void updateMapMarkers(List<MowingPlace> places) {
        // Smazat staré overlaye (pokud znovu načítáme)
        mapView.getOverlays().clear();

        for (MowingPlace place : places) {
            GeoPoint point = new GeoPoint(place.getLatitude(), place.getLongitude());
            Marker marker = new Marker(mapView);
            marker.setPosition(point);
            marker.setTitle(place.getName());

            // Volitelné nastavení ikonky markeru
            // marker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_marker));

            marker.setOnMarkerClickListener((m, mapView) -> {
                showPlaceDetail(place);
                return true; // vrátí true, pokud nechceme defaultní chování
            });

            mapView.getOverlays().add(marker);
        }

        // Refresh mapy
        mapView.invalidate();
    }

    private void showPlaceDetail(MowingPlace place) {
        // Pro ukázku jen Toast
        String message = "ID: " + place.getId()
                + "\nTime: " + place.getTimeRequirement()
                + "\nDescription: " + place.getDescription();
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();

        // Reálně by ses přepnul na DetailFragment nebo zobrazil Dialog:
        // PlaceDetailDialogFragment dialog = PlaceDetailDialogFragment.newInstance(place.getId());
        // dialog.show(getChildFragmentManager(), "detailDialog");
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume(); // důležité pro osmdroid
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause(); // důležité pro osmdroid
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
