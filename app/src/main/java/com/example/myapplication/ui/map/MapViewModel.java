package com.example.myapplication.ui.map;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.data.MowingPlace;
import com.example.myapplication.data.MowingPlacesRepository;

import java.util.List;

/**
 * ViewModel class for managing the data and logic of the MapFragment.
 * This class handles loading mowing places from the repository.
 */
public class MapViewModel extends AndroidViewModel {

    /**
     * LiveData object containing a list of MowingPlace objects.
     * This data is observed by the MapFragment to update the UI.
     */
    private final MutableLiveData<List<MowingPlace>> placesLiveData = new MutableLiveData<>();

    /**
     * Repository object used to load mowing places.
     * This repository handles data operations and provides access to the data source.
     */
    private final MowingPlacesRepository repository;

    /**
     * Constructor for MapViewModel.
     * Initializes the MutableLiveData object and the repository.
     *
     * @param application The application context used to access files and assets.
     */
    public MapViewModel(@NonNull Application application) {
        super(application);
        repository = new MowingPlacesRepository();
        loadData();
    }

    /**
     * Loads the mowing places from the repository and sets them to the LiveData object.
     * This method is called in the constructor to initialize the data.
     */
    void loadData() {
        List<MowingPlace> places = repository.loadMowingPlaces(getApplication());
        placesLiveData.setValue(places);
    }

    /**
     * Returns the LiveData object containing the list of MowingPlace objects.
     * This method is used by the MapFragment to observe changes in the data.
     *
     * @return A LiveData object containing a list of MowingPlace objects.
     */
    public LiveData<List<MowingPlace>> getPlaces() {
        return placesLiveData;
    }
}
