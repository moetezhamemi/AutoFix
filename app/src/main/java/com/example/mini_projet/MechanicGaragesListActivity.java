package com.example.mini_projet;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mini_projet.adapter.AdminGarageAdapter;
import com.example.mini_projet.models.Garage;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MechanicGaragesListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminGarageAdapter adapter;
    private List<Garage> garageList;
    private FirebaseFirestore db;
    private TextView emptyView, titleText;
    private ImageView btnBack;
    private String mechanicId, mechanicName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mechanic_garages_list);

        db = FirebaseFirestore.getInstance();
        garageList = new ArrayList<>();

        // Get mechanic info from intent
        mechanicId = getIntent().getStringExtra("mechanicId");
        mechanicName = getIntent().getStringExtra("mechanicName");

        recyclerView = findViewById(R.id.recyclerViewGarages);
        emptyView = findViewById(R.id.emptyView);
        btnBack = findViewById(R.id.btnBack);
        titleText = findViewById(R.id.titleText);

        titleText.setText(mechanicName + "'s Garages");

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminGarageAdapter(this, garageList);
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        fetchMechanicGarages();
    }

    private void fetchMechanicGarages() {
        db.collection("garages")
                .whereEqualTo("mechanicId", mechanicId)
                .whereEqualTo("enabled", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    garageList.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            Garage garage = document.toObject(Garage.class);
                            if (garage != null) {
                                garage.setId(document.getId());
                                garageList.add(garage);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        emptyView.setVisibility(View.GONE);
                    } else {
                        emptyView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching garages: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
