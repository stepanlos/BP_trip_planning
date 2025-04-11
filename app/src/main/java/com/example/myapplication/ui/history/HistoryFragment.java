package com.example.myapplication.ui.history;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.RoutePlan;
import com.example.myapplication.data.RoutePlanRepository;
import com.example.myapplication.databinding.FragmentHistoryBinding;

import java.util.List;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;
    private RecyclerView recyclerView;
    private HistoryAdapter historyAdapter;
    private RoutePlanRepository routePlanRepository;
    private List<RoutePlan> routePlans;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Enable options menu for "delete all"
        setHasOptionsMenu(true);

        recyclerView = binding.recyclerHistory;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        routePlanRepository = new RoutePlanRepository();
        routePlans = routePlanRepository.loadRoutePlans(getContext());
        // Sort route plans by date/time descending (most recent first)
        routePlans.sort((r1, r2) -> r2.getDateTime().compareTo(r1.getDateTime()));

        Log.d("HistoryFragment", "Loaded route plans count: " + routePlans.size());

        // Attach the adapter to the RecyclerView
        historyAdapter = new HistoryAdapter(routePlans);
        recyclerView.setAdapter(historyAdapter);

        return root;
    }

    // Inflate the options menu (with delete all)
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.history_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    // Handle options menu selections
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete_all) {
            new AlertDialog.Builder(getContext())
                    .setTitle("Potvrdit")
                    .setMessage("Opravdu chcete smazat všechny trasy?")
                    .setPositiveButton("Ano", (dialog, which) -> {
                        routePlans.clear();
                        routePlanRepository.saveRoutePlans(getContext(), routePlans);
                        historyAdapter.notifyDataSetChanged();
                    })
                    .setNegativeButton("Ne", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Inner adapter class for RecyclerView
    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

        private final List<RoutePlan> plans;

        HistoryAdapter(List<RoutePlan> plans) {
            this.plans = plans;
        }

        @NonNull
        @Override
        public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.route_plan_item, parent, false);
            return new HistoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
            RoutePlan plan = plans.get(position);
            holder.tvCreationDate.setText("Vytvořeno: " + plan.getDateTime());
            // Display stops as a comma-separated list (excluding start/end)
            StringBuilder stops = new StringBuilder();
            if (plan.getRoutePlaces() != null) {
                for (String name : plan.getRoutePlaces()) {
                    if (stops.length() > 0) stops.append(", ");
                    stops.append(name);
                }
            }
            holder.tvStops.setText(stops.toString());

            // Delete button listener
            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(getContext())
                        .setTitle("Potvrdit")
                        .setMessage("Opravdu chcete smazat tuto trasu?")
                        .setPositiveButton("Ano", (dialog, which) -> {
                            int pos = holder.getAdapterPosition();
                            plans.remove(pos);
                            routePlanRepository.saveRoutePlans(getContext(), plans);
                            notifyItemRemoved(pos);
                        })
                        .setNegativeButton("Ne", null)
                        .show();
            });

            // Open in Mapy.cz button listener
            holder.btnOpenMapyCz.setOnClickListener(v -> {
                if (!TextUtils.isEmpty(plan.getMapyCzUrl())) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(plan.getMapyCzUrl()));
                    startActivity(intent);
                }
            });

            // Open in Google Maps button listener
            holder.btnOpenGoogleMaps.setOnClickListener(v -> {
                if (!TextUtils.isEmpty(plan.getGoogleMapsUrl())) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(plan.getGoogleMapsUrl()));
                    startActivity(intent);
                }
            });
//
//            // Mark stop as completed (dummy implementation)
//            holder.btnMarkStopCompleted.setOnClickListener(v -> {
//                Toast.makeText(getContext(), "Místo označeno jako dokončené", Toast.LENGTH_SHORT).show();
//            });
        }

        @Override
        public int getItemCount() {
            return plans.size();
        }

        class HistoryViewHolder extends RecyclerView.ViewHolder {
            TextView tvCreationDate, tvStops;
            Button btnOpenMapyCz, btnOpenGoogleMaps, btnDelete, btnMarkStopCompleted;

            HistoryViewHolder(@NonNull View itemView) {
                super(itemView);
                tvCreationDate = itemView.findViewById(R.id.tvCreationDate);
                tvStops = itemView.findViewById(R.id.tvStops);
                btnOpenMapyCz = itemView.findViewById(R.id.btnOpenMapyCzRoute);
                btnOpenGoogleMaps = itemView.findViewById(R.id.btnOpenGoogleMapsRoute);
                btnDelete = itemView.findViewById(R.id.btnDeleteRoutePlan);
//                btnMarkStopCompleted = itemView.findViewById(R.id.btnMarkStopCompleted);
            }
        }
    }
}
