package com.example.myapplication.ui.history;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.data.RoutePlan;
import com.example.myapplication.data.RoutePlanRepository;

import java.util.List;

public class HistoryViewModel extends ViewModel {
    private MutableLiveData<List<RoutePlan>> routePlansLiveData;
    private RoutePlanRepository routePlanRepository;

    public HistoryViewModel() {
        routePlansLiveData = new MutableLiveData<>();
        routePlanRepository = new RoutePlanRepository();
    }

    public LiveData<List<RoutePlan>> getRoutePlansLiveData(Context context) {
        List<RoutePlan> plans = routePlanRepository.loadRoutePlans(context);
        routePlansLiveData.setValue(plans);
        return routePlansLiveData;
    }
}
