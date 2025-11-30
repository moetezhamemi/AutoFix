package com.example.mini_projet;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mini_projet.adapter.ChatListAdapter;
import com.example.mini_projet.models.Chat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MechanicGarageChatListActivity extends AppCompatActivity {

    private RecyclerView recyclerClients;
    private TextView noConversationsText, headerTitle;
    private ImageView btnBack;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ChatListAdapter adapter;
    private List<Chat> chatList;
    private String garageId;
    private String garageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mechanic_garage_chat_list);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        garageId = getIntent().getStringExtra("garageId");
        garageName = getIntent().getStringExtra("garageName");

        recyclerClients = findViewById(R.id.recyclerClients);
        noConversationsText = findViewById(R.id.noConversationsText);
        btnBack = findViewById(R.id.btnBack);
        headerTitle = findViewById(R.id.headerTitle);

        if (garageName != null) {
            headerTitle.setText(garageName + " - Chats");
        }

        recyclerClients.setLayoutManager(new LinearLayoutManager(this));
        chatList = new ArrayList<>();

        adapter = new ChatListAdapter(this, chatList, true); // true = mechanic view
        recyclerClients.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        loadChatsForGarage();
    }

    private void loadChatsForGarage() {
        db.collection("chats")
                .whereEqualTo("garageId", garageId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }

                    chatList.clear();
                    if (value != null && !value.isEmpty()) {
                        for (QueryDocumentSnapshot doc : value) {
                            Chat chat = doc.toObject(Chat.class);
                            chatList.add(chat);
                        }
                        adapter.notifyDataSetChanged();
                        noConversationsText.setVisibility(View.GONE);
                    } else {
                        noConversationsText.setVisibility(View.VISIBLE);
                    }
                });
    }
}
