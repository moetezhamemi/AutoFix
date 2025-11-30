package com.example.mini_projet;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mini_projet.adapter.MechanicDemandStatusAdapter;
import com.example.mini_projet.models.Garage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MechanicDemandsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TextView emptyView;
    private ImageView btnBack;
    private MechanicDemandStatusAdapter adapter;
    private List<Garage> garageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mechanic_demands);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        garageList = new ArrayList<>();

        recyclerView = findViewById(R.id.recyclerViewDemands);
        emptyView = findViewById(R.id.emptyView);
        btnBack = findViewById(R.id.btnBack);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MechanicDemandStatusAdapter(this, garageList);
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        fetchMyDemands();
    }

    private void fetchMyDemands() {
        if (mAuth.getCurrentUser() == null) return;
        String mechanicId = mAuth.getCurrentUser().getUid();

        db.collection("garages")
                .whereEqualTo("mechanicId", mechanicId)
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
                        emptyView.setText("No demands found");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching demands: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
