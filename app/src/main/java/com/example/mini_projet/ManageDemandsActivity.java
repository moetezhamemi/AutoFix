package com.example.mini_projet;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mini_projet.adapter.MechanicDemandAdapter;
import com.example.mini_projet.models.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ManageDemandsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MechanicDemandAdapter adapter;
    private List<User> mechanicList;
    private FirebaseFirestore db;
    private TextView emptyView;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_demands);

        db = FirebaseFirestore.getInstance();
        mechanicList = new ArrayList<>();

        recyclerView = findViewById(R.id.recyclerViewDemands);
        emptyView = findViewById(R.id.emptyView);
        btnBack = findViewById(R.id.btnBack);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MechanicDemandAdapter(this, mechanicList);
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        fetchPendingDemands();
    }

    private void fetchPendingDemands() {
        db.collection("users")
                .whereEqualTo("role", "mechanic")
                .whereEqualTo("enabled", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    mechanicList.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            User user = document.toObject(User.class);
                            if (user != null) {
                                // Ensure ID is set from document ID if not in object
                                user.setId(document.getId());
                                mechanicList.add(user);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        emptyView.setVisibility(View.GONE);
                    } else {
                        emptyView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching demands: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
