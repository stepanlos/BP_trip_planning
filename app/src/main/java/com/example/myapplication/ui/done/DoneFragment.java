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

public class DoneFragment extends Fragment {

    private FragmentDoneBinding binding;
    private DoneAdapter adapter;
    private DoneViewModel viewModel;

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

        // Pozor: musíme načíst data z kontextu
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
