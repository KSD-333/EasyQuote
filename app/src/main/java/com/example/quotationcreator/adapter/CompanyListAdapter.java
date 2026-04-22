package com.example.quotationcreator.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quotationcreator.R;
import com.example.quotationcreator.model.CompanyProfile;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class CompanyListAdapter extends RecyclerView.Adapter<CompanyListAdapter.CompanyViewHolder> {

    public interface CompanyActionListener {
        void onEditCompany(@NonNull CompanyProfile profile);

        void onSetActive(@NonNull CompanyProfile profile);

        void onDeleteCompany(@NonNull CompanyProfile profile);
    }

    private final List<CompanyProfile> companies = new ArrayList<>();
    private String activeCompanyId;
    private final CompanyActionListener listener;

    public CompanyListAdapter(@NonNull CompanyActionListener listener) {
        this.listener = listener;
    }

    public void submitData(@NonNull List<CompanyProfile> profiles, String activeCompanyId) {
        companies.clear();
        companies.addAll(profiles);
        this.activeCompanyId = activeCompanyId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CompanyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_company_settings, parent, false);
        return new CompanyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CompanyViewHolder holder, int position) {
        holder.bind(companies.get(position));
    }

    @Override
    public int getItemCount() {
        return companies.size();
    }

    final class CompanyViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvCompanyName;
        private final TextView tvCompanyMeta;
        private final TextView tvActiveTag;
        private final MaterialButton btnSetActive;
        private final MaterialButton btnEdit;
        private final MaterialButton btnDelete;

        CompanyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCompanyName = itemView.findViewById(R.id.tvCompanyName);
            tvCompanyMeta = itemView.findViewById(R.id.tvCompanyMeta);
            tvActiveTag = itemView.findViewById(R.id.tvActiveTag);
            btnSetActive = itemView.findViewById(R.id.btnSetActiveCompany);
            btnEdit = itemView.findViewById(R.id.btnEditCompany);
            btnDelete = itemView.findViewById(R.id.btnDeleteCompany);
        }

        void bind(@NonNull CompanyProfile profile) {
            String name = profile.getCompanyName();
            tvCompanyName.setText(TextUtils.isEmpty(name) ? itemView.getContext().getString(R.string.company_default_name) : name);

            String phone = profile.getCompanyPhone();
            String email = profile.getCompanyEmail();
            String meta;
            if (!TextUtils.isEmpty(phone) && !TextUtils.isEmpty(email)) {
                meta = phone + "  •  " + email;
            } else if (!TextUtils.isEmpty(phone)) {
                meta = phone;
            } else if (!TextUtils.isEmpty(email)) {
                meta = email;
            } else {
                meta = itemView.getContext().getString(R.string.company_meta_fallback);
            }
            tvCompanyMeta.setText(meta);

            boolean isActive = TextUtils.equals(activeCompanyId, profile.getCompanyId());
            tvActiveTag.setText(isActive ? R.string.active_company : R.string.inactive_company);

            btnSetActive.setEnabled(!isActive);
            btnSetActive.setText(isActive
                    ? itemView.getContext().getString(R.string.active_company)
                    : itemView.getContext().getString(R.string.set_active));

            btnSetActive.setOnClickListener(v -> listener.onSetActive(profile));
            btnEdit.setOnClickListener(v -> listener.onEditCompany(profile));
            btnDelete.setOnClickListener(v -> listener.onDeleteCompany(profile));
        }
    }
}
