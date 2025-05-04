package com.example.myapplication.ui.history;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.data.RoutePlan;
import com.example.myapplication.data.RoutePlanRepository;

import java.util.List;

/**
 * ViewModel class for managing the data and logic of the HistoryFragment.
 * This class handles loading route plans from the repository.
 */
public class HistoryViewModel extends ViewModel {
    private MutableLiveData<List<RoutePlan>> routePlansLiveData;
    private RoutePlanRepository routePlanRepository;

    /**
     * Constructor for HistoryViewModel.
     * Initializes the MutableLiveData object and the repository.
     */
    public HistoryViewModel() {
        routePlansLiveData = new MutableLiveData<>();
        routePlanRepository = new RoutePlanRepository();
    }

    /**
     * Loads the route plans from the repository and returns them as LiveData.
     *
     * @param context The application context used to access files and assets.
     * @return A LiveData object containing a list of RoutePlan objects.
     */
    public LiveData<List<RoutePlan>> getRoutePlansLiveData(Context context) {
        List<RoutePlan> plans = routePlanRepository.loadRoutePlans(context);
        routePlansLiveData.setValue(plans);
        return routePlansLiveData;
    }
}
