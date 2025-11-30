package com.example.mini_projet;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mini_projet.adapter.MechanicGarageAdapter;
import com.example.mini_projet.models.Garage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MechanicManageGaragesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MechanicGarageAdapter adapter;
    private List<Garage> garageList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TextView emptyView;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mechanic_manage_garages);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        garageList = new ArrayList<>();

        recyclerView = findViewById(R.id.recyclerViewGarages);
        emptyView = findViewById(R.id.emptyView);
        btnBack = findViewById(R.id.btnBack);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MechanicGarageAdapter(this, garageList);
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        fetchMyGarages();
    }

    private void fetchMyGarages() {
        String mechanicId = mAuth.getCurrentUser().getUid();

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

    @Override
    protected void onResume() {
        super.onResume();
        fetchMyGarages();
    }
}
