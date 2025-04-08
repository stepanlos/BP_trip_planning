package com.example.myapplication.ui.planning;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class LocationPickerActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_LAT = "extra_selected_lat";
    public static final String EXTRA_SELECTED_LON = "extra_selected_lon";

    private MapView mapView;
    private Marker marker;
    private Button btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        // Enable Up navigation
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mapView = findViewById(R.id.locationMapView);
        btnConfirm = findViewById(R.id.btnConfirmLocation);

        mapView.setMultiTouchControls(true);
        // Set initial position – center of Czech Republic
        GeoPoint initialPoint = new GeoPoint(49.8175, 15.4730);
        mapView.getController().setZoom(7.0);
        mapView.getController().setCenter(initialPoint);

        // Create a marker that will be moved on map tap
        marker = new Marker(mapView);
        marker.setPosition(initialPoint);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(marker);

        // Add an overlay to update marker position on single tap
        mapView.getOverlays().add(new Marker(mapView) {
            @Override
            public boolean onSingleTapConfirmed(@NonNull android.view.MotionEvent event, MapView mapView) {
                // Convert touch point to GeoPoint and update marker
                GeoPoint tappedPoint = (GeoPoint) mapView.getProjection().fromPixels((int) event.getX(), (int) event.getY());
                marker.setPosition(tappedPoint);
                mapView.invalidate();
                return true;
            }
        });

        // Confirm button returns selected coordinates
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GeoPoint selectedPoint = marker.getPosition();
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_SELECTED_LAT, selectedPoint.getLatitude());
                resultIntent.putExtra(EXTRA_SELECTED_LON, selectedPoint.getLongitude());
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    // Handle Up button in ActionBar
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
}
