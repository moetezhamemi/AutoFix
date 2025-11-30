package com.example.mini_projet;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mini_projet.adapter.MessageAdapter;
import com.example.mini_projet.models.Chat;
import com.example.mini_projet.models.Message;
import com.example.mini_projet.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView chatTitle;
    private EditText textSend;
    private ImageButton btnSend;
    private RecyclerView recyclerChat;

    private FirebaseUser fuser;
    private FirebaseFirestore db;
    private MessageAdapter messageAdapter;
    private List<Message> mChat;

    private String otherUserId;
    private String chatId;
    private String garageId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        btnBack = findViewById(R.id.btnBack);
        chatTitle = findViewById(R.id.chatTitle);
        textSend = findViewById(R.id.textSend);
        btnSend = findViewById(R.id.btnSend);
        recyclerChat = findViewById(R.id.recyclerChat);

        recyclerChat.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerChat.setLayoutManager(linearLayoutManager);

        fuser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        otherUserId = getIntent().getStringExtra("otherUserId");
        chatId = getIntent().getStringExtra("chatId");
        garageId = getIntent().getStringExtra("garageId");

        // Set title based on garage or user
        if (garageId != null) {
            db.collection("garages").document(garageId).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String garageName = doc.getString("name");
                    chatTitle.setText(garageName != null ? garageName : "Garage");
                }
            });
        } else {
            db.collection("users").document(otherUserId).get().addOnSuccessListener(doc -> {
                User user = doc.toObject(User.class);
                if (user != null && user.getName() != null) {
                    chatTitle.setText(user.getName());
                } else {
                    chatTitle.setText("User");
                }
            });
        }

        if (chatId != null) {
            readMessages(chatId);
        }

        btnBack.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> {
            String msg = textSend.getText().toString();
            if (!msg.equals("")) {
                sendMessage(fuser.getUid(), msg);
                textSend.setText("");
            } else {
                Toast.makeText(ChatActivity.this, "You can't send empty message", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage(String sender, String message) {
        long timestamp = System.currentTimeMillis();

        if (chatId == null) {
            // Create new chat
            chatId = db.collection("chats").document().getId();
            List<String> participants = new ArrayList<>();
            participants.add(fuser.getUid());
            participants.add(otherUserId);

            Map<String, String> names = new HashMap<>();
            // Fetch current user name
            db.collection("users").document(fuser.getUid()).get().addOnSuccessListener(doc -> {
                User me = doc.toObject(User.class);
                String myName = (me != null && me.getName() != null) ? me.getName() : "User";
                names.put(fuser.getUid(), myName);

                // Fetch other user name
                db.collection("users").document(otherUserId).get().addOnSuccessListener(doc2 -> {
                    User other = doc2.toObject(User.class);
                    String otherName = (other != null && other.getName() != null) ? other.getName() : "User";
                    names.put(otherUserId, otherName);

                    Chat chat = new Chat(chatId, participants, message, timestamp, names, garageId);
                    db.collection("chats").document(chatId).set(chat)
                            .addOnSuccessListener(aVoid -> {
                                addMessageToSubcollection(sender, message, timestamp);
                                readMessages(chatId); // Start listening
                            });
                });
            });

        } else {
            // Update existing chat
            db.collection("chats").document(chatId)
                    .update("lastMessage", message, "lastMessageTime", timestamp);
            addMessageToSubcollection(sender, message, timestamp);
        }
    }

    private void addMessageToSubcollection(String sender, String message, long timestamp) {
        Message msg = new Message(sender, message, timestamp);
        db.collection("chats").document(chatId).collection("messages").add(msg);
    }

    private void readMessages(String chatId) {
        mChat = new ArrayList<>();
        db.collection("chats").document(chatId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }

                    mChat.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Message message = doc.toObject(Message.class);
                            mChat.add(message);
                        }
                        messageAdapter = new MessageAdapter(ChatActivity.this, mChat);
                        recyclerChat.setAdapter(messageAdapter);
                    }
                });
    }
}
