package com.example.quotationcreator.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quotationcreator.R;
import com.example.quotationcreator.db.DatabaseHelper;
import com.example.quotationcreator.util.CurrencyUtils;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<DatabaseHelper.HistoryItem> items;
    private OnHistoryActionListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public interface OnHistoryActionListener {
        void onView(DatabaseHelper.HistoryItem item);
        void onDelete(DatabaseHelper.HistoryItem item);
    }

    public HistoryAdapter(List<DatabaseHelper.HistoryItem> items, OnHistoryActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DatabaseHelper.HistoryItem item = items.get(position);
        holder.tvName.setText(item.customerName);
        holder.tvPhone.setText(item.customerPhone);
        holder.tvQuoteNum.setText("Quote #" + item.quoteNumber);
        holder.tvDate.setText(dateFormat.format(new Date(item.dateMillis)));
        holder.tvAmount.setText(CurrencyUtils.format(item.totalAmount));

        holder.btnView.setOnClickListener(v -> listener.onView(item));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateList(List<DatabaseHelper.HistoryItem> newList) {
        this.items = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvQuoteNum, tvDate, tvAmount;
        MaterialButton btnView, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvHistoryCustomerName);
            tvPhone = itemView.findViewById(R.id.tvHistoryPhone);
            tvQuoteNum = itemView.findViewById(R.id.tvHistoryQuoteNum);
            tvDate = itemView.findViewById(R.id.tvHistoryDate);
            tvAmount = itemView.findViewById(R.id.tvHistoryAmount);
            btnView = itemView.findViewById(R.id.btnHistoryView);
            btnDelete = itemView.findViewById(R.id.btnHistoryDelete);
        }
    }
}
