package com.example.mini_projet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mini_projet.adapter.MechanicListAdapter;
import com.example.mini_projet.models.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ManageGaragesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MechanicListAdapter adapter;
    private List<User> mechanicList;
    private FirebaseFirestore db;
    private TextView emptyView;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_garages);

        db = FirebaseFirestore.getInstance();
        mechanicList = new ArrayList<>();

        recyclerView = findViewById(R.id.recyclerViewGarages);
        emptyView = findViewById(R.id.emptyView);
        btnBack = findViewById(R.id.btnBack);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MechanicListAdapter(this, mechanicList);
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        fetchApprovedMechanics();
    }

    private void fetchApprovedMechanics() {
        db.collection("users")
                .whereEqualTo("role", "mechanic")
                .whereEqualTo("enabled", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    mechanicList.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            User mechanic = document.toObject(User.class);
                            if (mechanic != null) {
                                mechanic.setId(document.getId());
                                mechanicList.add(mechanic);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        emptyView.setVisibility(View.GONE);
                        emptyView.setText("No approved mechanics yet");
                    } else {
                        emptyView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching mechanics: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
