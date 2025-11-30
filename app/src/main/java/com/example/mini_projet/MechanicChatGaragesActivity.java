package com.example.mini_projet;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mini_projet.adapter.GarageWithMessagesAdapter;
import com.example.mini_projet.models.Garage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MechanicChatGaragesActivity extends AppCompatActivity {

    private RecyclerView recyclerGarages;
    private TextView noMessagesText;
    private ImageView btnBack;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private GarageWithMessagesAdapter adapter;
    private List<Garage> garageList;
    private Map<String, Integer> garageMessageCounts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mechanic_chat_garages);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerGarages = findViewById(R.id.recyclerGarages);
        noMessagesText = findViewById(R.id.noMessagesText);
        btnBack = findViewById(R.id.btnBack);

        recyclerGarages.setLayoutManager(new LinearLayoutManager(this));
        garageList = new ArrayList<>();
        garageMessageCounts = new HashMap<>();

        adapter = new GarageWithMessagesAdapter(this, garageList, garageMessageCounts);
        recyclerGarages.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        loadGaragesWithMessages();
    }

    private void loadGaragesWithMessages() {
        String mechanicId = auth.getCurrentUser().getUid();

        // First, get all garages owned by this mechanic
        db.collection("garages")
                .whereEqualTo("mechanicId", mechanicId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> garageIds = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Garage garage = document.toObject(Garage.class);
                        garageList.add(garage);
                        garageIds.add(garage.getId());
                    }

                    if (garageIds.isEmpty()) {
                        noMessagesText.setVisibility(View.VISIBLE);
                        return;
                    }

                    // Now check which garages have messages
                    db.collection("chats")
                            .get()
                            .addOnSuccessListener(chatSnapshots -> {
                                for (QueryDocumentSnapshot chatDoc : chatSnapshots) {
                                    String garageId = chatDoc.getString("garageId");
                                    if (garageId != null && garageIds.contains(garageId)) {
                                        int count = garageMessageCounts.getOrDefault(garageId, 0);
                                        garageMessageCounts.put(garageId, count + 1);
                                    }
                                }

                                // Filter garages to only show those with messages
                                List<Garage> garagesWithMessages = new ArrayList<>();
                                for (Garage garage : garageList) {
                                    if (garageMessageCounts.containsKey(garage.getId())) {
                                        garagesWithMessages.add(garage);
                                    }
                                }

                                garageList.clear();
                                garageList.addAll(garagesWithMessages);

                                if (garageList.isEmpty()) {
                                    noMessagesText.setVisibility(View.VISIBLE);
                                } else {
                                    adapter.notifyDataSetChanged();
                                }
                            });
                });
    }
}
