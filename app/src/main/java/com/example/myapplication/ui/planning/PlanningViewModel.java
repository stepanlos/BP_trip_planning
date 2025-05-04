package com.example.myapplication.ui.planning;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * ViewModel class for managing the data and logic of the PlanningFragment.
 * This class handles loading and displaying planning-related data.
 */
public class PlanningViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    /**
     * Constructor for PlanningViewModel.
     * Initializes the MutableLiveData object with a default message.
     */
    public PlanningViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is planning fragment");
    }

    /**
     * Returns the LiveData object containing the text message.
     * This method is used by the PlanningFragment to observe changes in the data.
     *
     * @return A LiveData object containing a string message.
     */
    public LiveData<String> getText() {
        return mText;
    }
}