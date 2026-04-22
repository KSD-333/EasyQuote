package com.example.quotationcreator.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quotationcreator.R;
import com.example.quotationcreator.db.DatabaseHelper;

import java.util.List;

public class CustomerSearchAdapter extends RecyclerView.Adapter<CustomerSearchAdapter.ViewHolder> {

    private List<DatabaseHelper.CustomerSearchResult> customers;
    private OnCustomerSelectedListener listener;

    public interface OnCustomerSelectedListener {
        void onCustomerSelected(DatabaseHelper.CustomerSearchResult customer);
    }

    public CustomerSearchAdapter(List<DatabaseHelper.CustomerSearchResult> customers, OnCustomerSelectedListener listener) {
        this.customers = customers;
        this.listener = listener;
    }

    public void updateList(List<DatabaseHelper.CustomerSearchResult> newList) {
        this.customers = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_customer_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DatabaseHelper.CustomerSearchResult customer = customers.get(position);
        holder.tvName.setText(customer.name);
        holder.tvPhone.setText(customer.phone);
        
        if (customer.name != null && !customer.name.isEmpty()) {
            String initials = "";
            String[] parts = customer.name.split(" ");
            if (parts.length > 0) initials += parts[0].substring(0, 1).toUpperCase();
            if (parts.length > 1) initials += parts[1].substring(0, 1).toUpperCase();
            holder.tvInitials.setText(initials);
        } else {
            holder.tvInitials.setText("C");
        }

        holder.itemView.setOnClickListener(v -> listener.onCustomerSelected(customer));
    }

    @Override
    public int getItemCount() {
        return customers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvInitials;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCustomerName);
            tvPhone = itemView.findViewById(R.id.tvCustomerPhone);
            tvInitials = itemView.findViewById(R.id.tvInitials);
        }
    }
}
