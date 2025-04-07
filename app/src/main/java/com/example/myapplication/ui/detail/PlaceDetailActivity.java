package com.example.myapplication.ui.detail;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.data.MowingPlace;
import com.example.myapplication.data.MowingPlacesRepository;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PlaceDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PLACE_ID = "extra_place_id";

    private EditText etPlaceName;
    private EditText etTimeRequirement;
    private EditText etMowingCount;
    private EditText etWorkCost;
    private EditText etVisitDates;
    private EditText etDescription;
    private EditText etLatitude;
    private EditText etLongitude;
    private EditText etCaretaker;
    private EditText etCentre;
    private EditText etArea;
    private SwitchMaterial swLocked;
    private Button btnDelete; // New delete button

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
        etPlaceName = findViewById(R.id.etPlaceName);
        etTimeRequirement = findViewById(R.id.etTimeRequirement);
        etMowingCount = findViewById(R.id.etMowingCount);
        etWorkCost = findViewById(R.id.etWorkCost);
        etVisitDates = findViewById(R.id.etVisitDates);
        etDescription = findViewById(R.id.etDescription);
        etLatitude = findViewById(R.id.etLatitude);
        etLongitude = findViewById(R.id.etLongitude);
        etCaretaker = findViewById(R.id.etCaretaker);
        etCentre = findViewById(R.id.etCentre);
        etArea = findViewById(R.id.etArea);
        swLocked = findViewById(R.id.swLocked);
        btnDelete = findViewById(R.id.btnDelete); // Initialize the delete button

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

        // Set up the delete button with a confirmation dialog
        btnDelete.setOnClickListener(v -> new AlertDialog.Builder(PlaceDetailActivity.this)
                .setTitle("Potvrzení")
                .setMessage("Opravdu chcete odstranit toto místo?")
                .setPositiveButton("Ano", (dialog, which) -> deleteCurrentPlace())
                .setNegativeButton("Ne", null)
                .show());
    }

    // Populate the UI with the details of the current place
    private void populateFields() {
        etPlaceName.setText(currentPlace.getName());
        etTimeRequirement.setText(String.valueOf(currentPlace.getTimeRequirement()));
        etMowingCount.setText(String.valueOf(currentPlace.getMowingCountPerYear()));
        etWorkCost.setText(String.valueOf(currentPlace.getWorkCost()));
        etVisitDates.setText(TextUtils.join(", ", currentPlace.getVisitDates()));
        etDescription.setText(currentPlace.getDescription());
        etLatitude.setText(String.valueOf(currentPlace.getLatitude()));
        etLongitude.setText(String.valueOf(currentPlace.getLongitude()));
        etCaretaker.setText(currentPlace.getCaretaker());
        etCentre.setText(currentPlace.getCentre());
        etArea.setText(String.valueOf(currentPlace.getArea()));
        swLocked.setChecked(currentPlace.getLocked() == 1);
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

    // Save changes to the current place and update repository
    private void saveChanges() {
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
        currentPlace.setCaretaker(etCaretaker.getText().toString().trim());
        currentPlace.setCentre(etCentre.getText().toString().trim());
        try {
            currentPlace.setArea(Integer.parseInt(etArea.getText().toString().trim()));
        } catch (NumberFormatException e) {
            currentPlace.setArea(0);
        }
        currentPlace.setLocked(swLocked.isChecked() ? 1 : 0);

        boolean success = repository.saveMowingPlaces(this, allPlaces);
        if (success) {
            Toast.makeText(this, "Změny uloženy", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK, new Intent().putExtra("updatedPlaceId", currentPlace.getId()));
            finish();
        } else {
            Toast.makeText(this, "Chyba při ukládání změn", Toast.LENGTH_SHORT).show();
        }
    }

    // Delete the current place and remove its references from all other places
    private void deleteCurrentPlace() {
        // Remove the current place from the list
        allPlaces.remove(currentPlace);

        // Iterate through all remaining places and remove any distance entry referencing the deleted place
        for (MowingPlace place : allPlaces) {
            if (place.getDistancesToOthers() != null) {
                // Using removeIf (available on API level 24+); alternatively use an iterator
                place.getDistancesToOthers().removeIf(distanceEntry ->
                        distanceEntry.getId().equals(currentPlace.getId())
                );
            }
        }

        // Save updated list to JSON via repository
        boolean success = repository.saveMowingPlaces(this, allPlaces);
        if (success) {
            Toast.makeText(this, "Místo bylo odstraněno", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK, new Intent().putExtra("deletedPlaceId", currentPlace.getId()));
            finish();
        } else {
            Toast.makeText(this, "Chyba při odstraňování místa", Toast.LENGTH_SHORT).show();
        }
    }
}
