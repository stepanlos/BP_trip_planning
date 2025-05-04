package com.example.myapplication.ui.done;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.databinding.FragmentDoneBinding;

import java.util.List;

/**
 * Fragment that displays a list of completed visits.
 * It uses a RecyclerView to show the list of VisitEntry objects.
 */
public class DoneFragment extends Fragment {

    private FragmentDoneBinding binding;
    private DoneAdapter adapter;
    private DoneViewModel viewModel;

    /**
     * Called when the fragment is created.
     * It initializes the ViewModel and sets up the RecyclerView.
     *
     * @param inflater           The LayoutInflater used to inflate the fragment's view.
     * @param container          The parent view that this fragment's UI should be attached to.
     * @param savedInstanceState A Bundle containing the saved state of the fragment.
     * @return The root view of the fragment.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDoneBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Recycler setup
        binding.recyclerDone.setLayoutManager(new LinearLayoutManager(getContext()));
        viewModel = new ViewModelProvider(this).get(DoneViewModel.class);
        adapter = new DoneAdapter(getContext(), viewModel, List.of());
        binding.recyclerDone.setAdapter(adapter);

        // Load data and update UI
        viewModel.getVisitEntries(getContext()).observe(getViewLifecycleOwner(), entries -> {
            if (entries.isEmpty()) {
                binding.recyclerDone.setVisibility(View.GONE);
                binding.tvEmptyDone.setVisibility(View.VISIBLE);
            } else {
                binding.tvEmptyDone.setVisibility(View.GONE);
                binding.recyclerDone.setVisibility(View.VISIBLE);
                adapter.setEntries(entries);
            }
        });

        return root;
    }

    /**
     * Called when the view is destroyed.
     * It sets the binding to null to avoid memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
