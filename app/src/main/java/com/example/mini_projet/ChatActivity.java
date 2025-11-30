package com.example.mini_projet;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mini_projet.adapter.MessageAdapter;
import com.example.mini_projet.models.Chat;
import com.example.mini_projet.models.Message;
import com.example.mini_projet.models.User;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private ImageView btnBack;
    private TextView chatTitle;
    private EditText textSend;
    private ImageButton btnSend, btnShareLocation;
    private RecyclerView recyclerChat;

    private FirebaseUser fuser;
    private FirebaseFirestore db;
    private MessageAdapter messageAdapter;
    private List<Message> mChat;
    private FusedLocationProviderClient fusedLocationClient;

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
        btnShareLocation = findViewById(R.id.btnShareLocation);
        recyclerChat = findViewById(R.id.recyclerChat);

        recyclerChat.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerChat.setLayoutManager(linearLayoutManager);

        fuser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

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
                sendMessage(fuser.getUid(), msg, "text", null, null);
                textSend.setText("");
            } else {
                Toast.makeText(ChatActivity.this, "You can't send empty message", Toast.LENGTH_SHORT).show();
            }
        });

        // Check user role to toggle location button visibility
        db.collection("users").document(fuser.getUid()).get().addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user != null && "mechanic".equalsIgnoreCase(user.getRole())) {
                btnShareLocation.setVisibility(View.GONE);
            } else {
                btnShareLocation.setVisibility(View.VISIBLE);
                btnShareLocation.setOnClickListener(v -> shareLocation());
            }
        });
    }

    private void shareLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        sendMessage(fuser.getUid(), "Location shared", "location", latitude, longitude);
                    } else {
                        Toast.makeText(this, "Unable to get location. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                shareLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendMessage(String sender, String message, String type, Double latitude, Double longitude) {
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
                                addMessageToSubcollection(sender, message, timestamp, type, latitude, longitude);
                                readMessages(chatId); // Start listening
                            });
                });
            });

        } else {
            // Update existing chat
            db.collection("chats").document(chatId)
                    .update("lastMessage", message, "lastMessageTime", timestamp);
            addMessageToSubcollection(sender, message, timestamp, type, latitude, longitude);
        }
    }

    private void addMessageToSubcollection(String sender, String message, long timestamp, String type, Double latitude, Double longitude) {
        Message msg = new Message(sender, message, timestamp, type, latitude, longitude);
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
                            if (message != null) {
                                mChat.add(message);
                            }
                        }
                        messageAdapter = new MessageAdapter(ChatActivity.this, mChat, fuser.getUid());
                        recyclerChat.setAdapter(messageAdapter);
                    }
                });
    }
}
