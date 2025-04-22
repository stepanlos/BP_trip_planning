package com.example.myapplication.ui.done;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;


import com.example.myapplication.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DoneAdapter extends RecyclerView.Adapter<DoneAdapter.ViewHolder> {

    private final Context context;
    private final DoneViewModel viewModel;
    private List<VisitEntry> entries;
    private final SimpleDateFormat srcFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat dstFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    public DoneAdapter(Context context, DoneViewModel viewModel, List<VisitEntry> entries) {
        this.context = context;
        this.viewModel = viewModel;
        this.entries = entries;
    }

    public void setEntries(List<VisitEntry> entries) {
        this.entries = entries;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_done_visit, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        VisitEntry e = entries.get(pos);
        // date format
        String formattedDate;
        try {
            Date d = srcFormat.parse(e.getVisitDate());
            formattedDate = dstFormat.format(d);
        } catch (Exception ex) {
            formattedDate = e.getVisitDate();
        }

        // set text
        h.tvPlaceName.setText(e.getPlaceName());
        h.tvVisitDate.setText(formattedDate);

        // confirm delete
        h.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Potvrzení")
                    .setMessage("Opravdu chcete odstranit návštěvu?")
                    .setPositiveButton("Ano", (dialog, which) ->
                            viewModel.removeVisit(context, e)
                    )
                    .setNegativeButton("Ne", null)
                    .show();
        });
    }


    @Override public int getItemCount() { return entries.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlaceName, tvVisitDate;
        ImageButton btnDelete;
        ViewHolder(@NonNull View v) {
            super(v);
            tvPlaceName = v.findViewById(R.id.tvPlaceName);
            tvVisitDate = v.findViewById(R.id.tvVisitDate);
            btnDelete   = v.findViewById(R.id.btnDeleteVisit);
        }
    }

}
