package com.example.myapplication.util;

import android.content.Context;
import java.text.Normalizer;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import java.util.ArrayList;
import java.util.List;

public class DiacriticInsensitiveAdapter<T> extends ArrayAdapter<T> {

    private List<T> items;
    private List<T> itemsAll;
    private List<T> suggestions;

    public DiacriticInsensitiveAdapter(Context context, int resource, List<T> objects) {
        super(context, resource, objects);
        this.items = new ArrayList<>(objects);
        this.itemsAll = new ArrayList<>(objects);
        this.suggestions = new ArrayList<>();
    }

    @Override
    public Filter getFilter() {
        return nameFilter;
    }

    private Filter nameFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint != null) {
                suggestions.clear();
                String normalizedConstraint = removeDiacritics(constraint.toString().toLowerCase().trim());
                for (T item : itemsAll) {
                    String itemText = removeDiacritics(item.toString().toLowerCase());
                    if (itemText.contains(normalizedConstraint)) {
                        suggestions.add(item);
                    }
                }
                results.values = suggestions;
                results.count = suggestions.size();
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            clear();
            if (results != null && results.count > 0) {
                addAll((List<T>) results.values);
                notifyDataSetChanged();
            } else {
//                addAll(itemsAll);
                notifyDataSetInvalidated();
            }
        }
    };

    private String removeDiacritics(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
