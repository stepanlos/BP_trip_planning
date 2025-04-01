package com.example.myapplication.ui.detail;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.data.MowingPlace;
import com.example.myapplication.data.MowingPlacesRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlaceDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PLACE_ID = "extra_place_id";

    private TextView tvPlaceId;
    private EditText etPlaceName;
    private EditText etTimeRequirement;
    private EditText etMowingCount;
    private EditText etWorkCost;
    private EditText etVisitDates;
    private EditText etDescription;
    private EditText etLatitude;
    private EditText etLongitude;

    private MowingPlace currentPlace;
    private List<MowingPlace> allPlaces;
    private MowingPlacesRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_detail);

        // Enable the Up button in the ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize repository and load all places
        repository = new MowingPlacesRepository();
        allPlaces = repository.loadMowingPlaces(this);

        // Find views by ID
        //tvPlaceId = findViewById(R.id.tvPlaceId);
        etPlaceName = findViewById(R.id.etPlaceName);
        etTimeRequirement = findViewById(R.id.etTimeRequirement);
        etMowingCount = findViewById(R.id.etMowingCount);
        etWorkCost = findViewById(R.id.etWorkCost);
        etVisitDates = findViewById(R.id.etVisitDates);
        etDescription = findViewById(R.id.etDescription);
        etLatitude = findViewById(R.id.etLatitude);
        etLongitude = findViewById(R.id.etLongitude);

        // Get the place ID from the Intent extras
        String placeId = getIntent().getStringExtra(EXTRA_PLACE_ID);
        if (placeId == null) {
            Toast.makeText(this, "Nebylo zadáno žádné ID místa", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Find the place in the list
        for (MowingPlace place : allPlaces) {
            if (place.getId().equals(placeId)) {
                currentPlace = place;
                break;
            }
        }

        if (currentPlace == null) {
            Toast.makeText(this, "Místo nebylo nalezeno", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Populate the fields with current place details
        populateFields();
    }

    // Populate the UI with the details of the current place
    private void populateFields() {
        //tvPlaceId.setText(currentPlace.getId());
        etPlaceName.setText(currentPlace.getName());
        etTimeRequirement.setText(String.valueOf(currentPlace.getTimeRequirement()));
        etMowingCount.setText(String.valueOf(currentPlace.getMowingCountPerYear()));
        etWorkCost.setText(String.valueOf(currentPlace.getWorkCost()));
        // Convert list of visit dates to comma-separated string
        etVisitDates.setText(TextUtils.join(", ", currentPlace.getVisitDates()));
        etDescription.setText(currentPlace.getDescription());
        etLatitude.setText(String.valueOf(currentPlace.getLatitude()));
        etLongitude.setText(String.valueOf(currentPlace.getLongitude()));
    }

    // Inflate the menu with the Save action
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_place_detail, menu);
        return true;
    }

    // Handle ActionBar item clicks
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Handle the Save action
        if (id == R.id.action_save) {
            saveChanges();
            return true;
        }
        // Handle Up (back) button
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Collect data from fields, update the current place, save changes in repository
    private void saveChanges() {
        // Update currentPlace with new values from the EditTexts
        currentPlace.setName(etPlaceName.getText().toString().trim());
        try {
            currentPlace.setTimeRequirement(Double.parseDouble(etTimeRequirement.getText().toString().trim()));
        } catch (NumberFormatException e) {
            currentPlace.setTimeRequirement(0);
        }
        try {
            currentPlace.setMowingCountPerYear(Integer.parseInt(etMowingCount.getText().toString().trim()));
        } catch (NumberFormatException e) {
            currentPlace.setMowingCountPerYear(0);
        }
        try {
            currentPlace.setWorkCost(Integer.parseInt(etWorkCost.getText().toString().trim()));
        } catch (NumberFormatException e) {
            currentPlace.setWorkCost(0);
        }
        // Split visit dates by comma and trim spaces
        String datesString = etVisitDates.getText().toString().trim();
        if (!datesString.isEmpty()) {
            String[] datesArray = datesString.split(",");
            List<String> datesList = new ArrayList<>();
            for (String date : datesArray) {
                datesList.add(date.trim());
            }
            currentPlace.setVisitDates(datesList);
        } else {
            currentPlace.setVisitDates(new ArrayList<>());
        }
        currentPlace.setDescription(etDescription.getText().toString().trim());
        try {
            currentPlace.setLatitude(Double.parseDouble(etLatitude.getText().toString().trim()));
        } catch (NumberFormatException e) {
            currentPlace.setLatitude(0);
        }
        try {
            currentPlace.setLongitude(Double.parseDouble(etLongitude.getText().toString().trim()));
        } catch (NumberFormatException e) {
            currentPlace.setLongitude(0);
        }

        // Update the list in memory (allPlaces list already contains currentPlace by reference)
        // Save the updated list back to JSON using the repository
        boolean success = repository.saveMowingPlaces(this, allPlaces);
        if (success) {
                    String message = "cost: " + currentPlace.getWorkCost();


            Toast.makeText(this, message /*"Změny uloženy"*/, Toast.LENGTH_SHORT).show();
            // Optionally, return the updated data back to previous screen via setResult()
            setResult(RESULT_OK, new Intent().putExtra("updatedPlaceId", currentPlace.getId()));
            finish();
        } else {
            Toast.makeText(this, "Chyba při ukládání změn", Toast.LENGTH_SHORT).show();
        }

        setResult(RESULT_OK, new Intent().putExtra("updatedPlaceId", currentPlace.getId()));
        finish();
    }
}
