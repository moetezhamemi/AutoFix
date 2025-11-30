package com.example.mini_projet;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mini_projet.adapter.GarageRequestAdapter;
import com.example.mini_projet.models.Garage;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ManageDemandsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private GarageRequestAdapter adapter;
    private List<Garage> garageList;
    private FirebaseFirestore db;
    private TextView emptyView;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_demands);

        db = FirebaseFirestore.getInstance();
        garageList = new ArrayList<>();

        recyclerView = findViewById(R.id.recyclerViewDemands);
        emptyView = findViewById(R.id.emptyView);
        btnBack = findViewById(R.id.btnBack);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GarageRequestAdapter(this, garageList);
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        fetchPendingGarages();
    }

    private void fetchPendingGarages() {
        db.collection("garages")
                .whereEqualTo("enabled", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    garageList.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            Garage garage = document.toObject(Garage.class);
                            if (garage != null && !"REJECTED".equals(garage.getStatus())) {
                                garage.setId(document.getId());
                                garageList.add(garage);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        emptyView.setVisibility(View.GONE);
                    } else {
                        emptyView.setVisibility(View.VISIBLE);
                        emptyView.setText("No pending garage requests");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching garage requests: " + e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchPendingGarages();
    }
}
