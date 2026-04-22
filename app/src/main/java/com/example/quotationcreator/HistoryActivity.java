package com.example.quotationcreator;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quotationcreator.adapter.HistoryAdapter;
import com.example.quotationcreator.db.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private RecyclerView rvHistory;
    private HistoryAdapter adapter;
    private LinearLayout llEmpty;
    private List<DatabaseHelper.HistoryItem> historyList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dbHelper = new DatabaseHelper(this);
        
        Toolbar toolbar = findViewById(R.id.toolbarHistory);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvHistory = findViewById(R.id.rvHistory);
        llEmpty = findViewById(R.id.llEmpty);
        
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(historyList, new HistoryAdapter.OnHistoryActionListener() {
            @Override
            public void onView(DatabaseHelper.HistoryItem item) {
                // Return to MainActivity with the selected quote
                Intent intent = new Intent();
                intent.putExtra("LOADED_QUOTE_JSON", item.quoteJson);
                setResult(RESULT_OK, intent);
                finish();
            }

            @Override
            public void onDelete(DatabaseHelper.HistoryItem item) {
                new AlertDialog.Builder(HistoryActivity.this)
                        .setTitle("Delete Quote")
                        .setMessage("Are you sure you want to delete this bill from history?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            // save deleted quote into backup storage before removing
                            com.example.quotationcreator.util.BackupStorage.saveDeletedQuote(HistoryActivity.this, item.quoteJson);
                            dbHelper.deleteQuote(item.id);
                            loadData(null);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        rvHistory.setAdapter(adapter);

        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                loadData(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                loadData(newText);
                return true;
            }
        });

        loadData(null);
    }

    private void loadData(String query) {
        historyList = dbHelper.searchQuotes(query);
        adapter.updateList(historyList);
        
        if (historyList.isEmpty()) {
            llEmpty.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);
        } else {
            llEmpty.setVisibility(View.GONE);
            rvHistory.setVisibility(View.VISIBLE);
        }
    }
}
