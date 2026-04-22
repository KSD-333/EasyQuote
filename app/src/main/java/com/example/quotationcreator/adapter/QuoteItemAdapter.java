package com.example.quotationcreator.adapter;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quotationcreator.R;
import com.example.quotationcreator.model.Item;
import com.example.quotationcreator.util.CurrencyUtils;
import com.example.quotationcreator.util.QuoteCalculator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QuoteItemAdapter extends RecyclerView.Adapter<QuoteItemAdapter.ItemViewHolder> {

    public interface OnItemsChangedListener {
        void onItemsChanged();
    }

    private static final String[] UNIT_OPTIONS = new String[]{
        "PCS", "NOS", "MTR", "BOX", "SET", "KG", "GM", "LTR", "BAG", "SQMT", "SQFT", "FEET", "INCH", "ROLL", "PKT", "DOZ"
    };

    private final List<Item> items = new ArrayList<>();
    private final OnItemsChangedListener onItemsChangedListener;

    public QuoteItemAdapter(List<Item> seedItems, OnItemsChangedListener onItemsChangedListener) {
        if (seedItems != null) {
            items.addAll(seedItems);
        }
        this.onItemsChangedListener = onItemsChangedListener;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quote_row, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        holder.bind(items.get(position), position + 1);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<Item> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
        notifyItemsChanged();
    }

    public void addItem(Item item) {
        addItemAt(items.size(), item);
    }

    public void addItemAt(int index, Item item) {
        if (index < 0) index = 0;
        if (index > items.size()) index = items.size();
        items.add(index, item);
        notifyItemInserted(index);
        notifyItemRangeChanged(index, items.size() - index);
        notifyItemsChanged();
    }

    public List<Item> getItemsCopy() {
        List<Item> copy = new ArrayList<>();
        for (Item item : items) {
            copy.add(item.copy());
        }
        return copy;
    }

    private void notifyItemsChanged() {
        if (onItemsChangedListener != null) {
            onItemsChangedListener.onItemsChanged();
        }
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvIndex;
        private final TextView tvAmount;
        private final com.google.android.material.textfield.MaterialAutoCompleteTextView etItemName;
        private final TextInputEditText etHsn;
        private final TextInputEditText etQty;
        private final TextInputEditText etPrice;
        private final AutoCompleteTextView actvUnit;
        private final ImageButton btnDelete;
        private final MaterialButton btnAddAbove;
        private final MaterialButton btnAddBelow;
        private final android.widget.ArrayAdapter<String> itemNameAdapter;

        private boolean binding;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIndex = itemView.findViewById(R.id.tvItemIndex);
            tvAmount = itemView.findViewById(R.id.tvItemAmount);
            etItemName = itemView.findViewById(R.id.etItemName);
            etHsn = itemView.findViewById(R.id.etHsnSac);
            etQty = itemView.findViewById(R.id.etQuantity);
            etPrice = itemView.findViewById(R.id.etUnitPrice);
            actvUnit = itemView.findViewById(R.id.actvUnit);
            btnDelete = itemView.findViewById(R.id.btnDeleteItem);
            btnAddAbove = itemView.findViewById(R.id.btnAddAbove);
            btnAddBelow = itemView.findViewById(R.id.btnAddBelow);
                itemNameAdapter = new android.widget.ArrayAdapter<>(
                    itemView.getContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    new java.util.ArrayList<>()
                );

            setupUnitDropdown();
            setupItemNameDropdown();
            attachWatchers();
            btnDelete.setOnClickListener(v -> removeCurrentItem());
            btnAddAbove.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) addItemAt(pos, new Item());
            });
            btnAddBelow.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) addItemAt(pos + 1, new Item());
            });
        }

        private void setupItemNameDropdown() {
            etItemName.setAdapter(itemNameAdapter);
            etItemName.setThreshold(1);
            refreshItemNameSuggestions();

            etItemName.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    return;
                }
                refreshItemNameSuggestions();
                if (itemNameAdapter.getCount() > 0) {
                    etItemName.post(etItemName::showDropDown);
                }
            });

            etItemName.setOnClickListener(v -> {
                refreshItemNameSuggestions();
                if (itemNameAdapter.getCount() > 0) {
                    etItemName.showDropDown();
                }
            });

            etItemName.setOnItemClickListener((parent, view, position, id) -> {
                String selection = itemNameAdapter.getItem(position);
                if (selection == null) return;
                org.json.JSONObject o = com.example.quotationcreator.util.ItemStorage.getItemByName(itemView.getContext(), selection);
                if (o == null) return;
                // populate related fields
                try {
                    String hsn = o.optString("hsn", "");
                    String unit = o.optString("unit", "");
                    double price = o.optDouble("unitPrice", 0d);
                    etHsn.setText(hsn);
                    actvUnit.setText(unit);
                    etPrice.setText(price == 0d ? "" : String.format(java.util.Locale.getDefault(), "%.2f", price));
                } catch (Exception ignored) {}
            });
        }

        private void refreshItemNameSuggestions() {
            java.util.List<String> names = com.example.quotationcreator.util.ItemStorage.getAllItemNames(itemView.getContext());
            itemNameAdapter.clear();
            itemNameAdapter.addAll(names);
            itemNameAdapter.notifyDataSetChanged();
        }

        private void setupUnitDropdown() {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                itemView.getContext(),
                android.R.layout.simple_dropdown_item_1line,
                UNIT_OPTIONS
            );
            actvUnit.setAdapter(adapter);
            actvUnit.setOnItemClickListener((parent, view, position, id) -> {
                Item item = currentItem();
                if (item != null) {
                    item.setUnit(parent.getItemAtPosition(position).toString());
                    notifyItemsChanged();
                }
            });
        }

        void bind(Item item, int displayIndex) {
            binding = true;

            tvIndex.setText(String.format(Locale.getDefault(), "Item %d", displayIndex));
            refreshItemNameSuggestions();
            setTextSafely(etItemName, item.getItemName());
            setTextSafely(etHsn, item.getHsnSacCode());
            setTextSafely(actvUnit, item.getUnit());
            setTextSafely(etQty, numberText(item.getQuantity(), true));
            setTextSafely(etPrice, numberText(item.getUnitPrice(), false));

            item.setDiscountPercent(0d);

            double effectiveQty = item.getQuantity();
            if (effectiveQty <= 0d && !TextUtils.isEmpty(item.getItemName()) && item.getUnitPrice() > 0d) {
                effectiveQty = 1d;
            }
            Item calculationItem = item.copy();
            calculationItem.setQuantity(effectiveQty);

            double lineAmount = QuoteCalculator.lineNetAmount(calculationItem);
            item.setLineAmount(lineAmount);
            tvAmount.setText(CurrencyUtils.format(lineAmount));

            binding = false;
        }

        private void removeCurrentItem() {
            int position = getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION) {
                return;
            }

            items.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, items.size() - position);
            notifyItemsChanged();
        }

        private void attachWatchers() {
            etItemName.addTextChangedListener(simpleWatcher(() -> {
                Item item = currentItem();
                if (item == null) {
                    return;
                }
                item.setItemName(textOf(etItemName));
                updateAmount(item);
            }));

            etHsn.addTextChangedListener(simpleWatcher(() -> {
                Item item = currentItem();
                if (item == null) {
                    return;
                }
                item.setHsnSacCode(textOf(etHsn));
                updateAmount(item);
            }));

            etQty.addTextChangedListener(simpleWatcher(() -> {
                Item item = currentItem();
                if (item == null) {
                    return;
                }
                item.setQuantity(parseNumber(etQty, 0d));
                updateAmount(item);
            }));

            etPrice.addTextChangedListener(simpleWatcher(() -> {
                Item item = currentItem();
                if (item == null) {
                    return;
                }
                item.setUnitPrice(parseNumber(etPrice, 0d));
                updateAmount(item);
            }));

            actvUnit.addTextChangedListener(simpleWatcher(() -> {
                Item item = currentItem();
                if (item != null) {
                    item.setUnit(actvUnit.getText().toString().trim());
                    notifyItemsChanged();
                }
            }));
        }

        private void updateAmount(Item item) {
            double effectiveQty = item.getQuantity();
            if (effectiveQty <= 0d && !TextUtils.isEmpty(item.getItemName()) && item.getUnitPrice() > 0d) {
                effectiveQty = 1d;
            }

            Item calculationItem = item.copy();
            calculationItem.setQuantity(effectiveQty);
            double lineAmount = QuoteCalculator.lineNetAmount(calculationItem);
            item.setLineAmount(lineAmount);
            tvAmount.setText(CurrencyUtils.format(lineAmount));
            notifyItemsChanged();
        }

        private Item currentItem() {
            int position = getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION || position >= items.size()) {
                return null;
            }
            return items.get(position);
        }

        private TextWatcher simpleWatcher(Runnable onChanged) {
            return new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (binding) {
                        return;
                    }
                    onChanged.run();
                }
            };
        }
    }

    private static String textOf(android.widget.TextView textView) {
        CharSequence cs = textView.getText();
        return cs == null ? "" : cs.toString().trim();
    }

    private static double parseNumber(android.widget.TextView textView, double fallback) {
        String value = textOf(textView);
        if (TextUtils.isEmpty(value)) {
            return fallback;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static String numberText(double value, boolean defaultOne) {
        if (defaultOne && value == 1d) {
            return "1";
        }
        if (value == 0d) {
            return "";
        }
        if (Math.floor(value) == value) {
            return String.format(Locale.getDefault(), "%.0f", value);
        }
        return String.format(Locale.getDefault(), "%.2f", value);
    }

    private static void setTextSafely(TextView textView, String value) {
        String current = textView.getText().toString().trim();
        String next = value == null ? "" : value;
        if (!current.equals(next)) {
            textView.setText(next);
        }
    }
}
