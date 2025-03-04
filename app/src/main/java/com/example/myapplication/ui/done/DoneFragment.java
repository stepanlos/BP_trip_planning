package com.example.myapplication.ui.done;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapplication.databinding.FragmentDoneBinding;

public class DoneFragment extends Fragment {

private FragmentDoneBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        DoneViewModel notificationsViewModel =
                new ViewModelProvider(this).get(DoneViewModel.class);

    binding = FragmentDoneBinding.inflate(inflater, container, false);
    View root = binding.getRoot();

        final TextView textView = binding.textDone;
        notificationsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

@Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}