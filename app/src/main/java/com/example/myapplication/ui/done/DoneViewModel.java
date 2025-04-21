package com.example.myapplication.ui.done;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.data.MowingPlace;
import com.example.myapplication.data.MowingPlacesRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DoneViewModel extends ViewModel {

    private final MutableLiveData<List<VisitEntry>> visitEntriesLiveData;
    private final MowingPlacesRepository repo;
    private final SimpleDateFormat srcFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public DoneViewModel() {
        visitEntriesLiveData = new MutableLiveData<>();
        repo = new MowingPlacesRepository();
    }

    /** Načte a seřadí všechny VisitEntry z JSONu */
    public LiveData<List<VisitEntry>> getVisitEntries(Context context) {
        List<MowingPlace> places = repo.loadMowingPlaces(context);
        List<VisitEntry> entries = new ArrayList<>();
        for (MowingPlace p : places) {
            for (String date : p.getVisitDates()) {
                entries.add(new VisitEntry(p.getId(), p.getName(), date));
            }
        }
        // řadit podle data sestupně
        Collections.sort(entries, new Comparator<VisitEntry>() {
            @Override
            public int compare(VisitEntry e1, VisitEntry e2) {
                try {
                    Date d1 = srcFormat.parse(e1.getVisitDate());
                    Date d2 = srcFormat.parse(e2.getVisitDate());
                    return d2.compareTo(d1);
                } catch (Exception ex) {
                    return 0;
                }
            }
        });
        visitEntriesLiveData.setValue(entries);
        return visitEntriesLiveData;
    }

    /** Smaže jedno datum u daného místa a znovu načte data */
    public void removeVisit(Context context, VisitEntry entry) {
        List<MowingPlace> places = repo.loadMowingPlaces(context);
        for (MowingPlace p : places) {
            if (p.getId().equals(entry.getPlaceId())) {
                p.getVisitDates().remove(entry.getVisitDate());
                break;
            }
        }
        repo.saveMowingPlaces(context, places);
        // znovu načíst
        getVisitEntries(context);
    }
}
