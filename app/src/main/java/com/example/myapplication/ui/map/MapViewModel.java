package com.example.myapplication.ui.map;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.data.MowingPlace;
import com.example.myapplication.data.MowingPlacesRepository;

import java.util.List;

public class MapViewModel extends AndroidViewModel {

    private final MutableLiveData<List<MowingPlace>> placesLiveData = new MutableLiveData<>();
    private final MowingPlacesRepository repository;

    public MapViewModel(@NonNull Application application) {
        super(application);
        repository = new MowingPlacesRepository();
        loadData();
    }

    void loadData() {
        List<MowingPlace> places = repository.loadMowingPlaces(getApplication());
        placesLiveData.setValue(places);
    }

    public LiveData<List<MowingPlace>> getPlaces() {
        return placesLiveData;
    }
}
