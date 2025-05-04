package com.example.myapplication.util;

import android.content.Context;
import java.text.Normalizer;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom ArrayAdapter that performs diacritic-insensitive filtering on a list of items.
 * This adapter is used to display suggestions in a ListView or Spinner.
 *
 * @param <T> The type of the items in the list.
 */
public class DiacriticInsensitiveAdapter<T> extends ArrayAdapter<T> {

    private final List<T> itemsAll;
    private final List<T> suggestions;

    /**
     * Constructor for DiacriticInsensitiveAdapter.
     *
     * @param context  The context in which the adapter is used.
     * @param resource The resource ID for a layout file containing a TextView to use when instantiating views.
     * @param objects  The list of items to be displayed.
     */
    public DiacriticInsensitiveAdapter(Context context, int resource, List<T> objects) {
        super(context, resource, objects);
        this.itemsAll = new ArrayList<>(objects);
        this.suggestions = new ArrayList<>();
    }

    /**
     * Returns the custom filter for diacritic-insensitive filtering.
     *
     * @return A Filter object that performs the filtering logic.
     */
    @Override
    public Filter getFilter() {
        return nameFilter;
    }

    /**
     * Custom filter that performs diacritic-insensitive filtering on the list of items.
     */
    private final Filter nameFilter = new Filter() {
        /**
         * Performs the filtering logic.
         *
         * @param constraint The constraint used for filtering.
         * @return A FilterResults object containing the filtered results.
         */
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

        /**
         * Publishes the filtered results to the adapter.
         *
         * @param constraint The constraint used for filtering.
         * @param results    The FilterResults object containing the filtered results.
         */
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

    /**
     * Removes diacritics from the input string.
     *
     * @param input The input string from which diacritics will be removed.
     * @return The input string without diacritics.
     */
    private String removeDiacritics(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
