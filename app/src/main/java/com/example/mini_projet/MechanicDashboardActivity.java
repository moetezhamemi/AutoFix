package com.example.mini_projet;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;
import de.hdodenhof.circleimageview.CircleImageView;
import com.example.mini_projet.models.User;

public class MechanicDashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CardView cardAddGarage, cardManageGarages, cardDemands;
    private ImageView logoutButton, chatIcon;
    private CircleImageView profileIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mechanic_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        cardAddGarage = findViewById(R.id.cardAddGarage);
        cardManageGarages = findViewById(R.id.cardManageGarages);
        cardDemands = findViewById(R.id.cardDemands);
        logoutButton = findViewById(R.id.logoutButton);
        profileIcon = findViewById(R.id.profile_icon);
        chatIcon = findViewById(R.id.chat_icon);

        // Add Garage Card Click
        cardAddGarage.setOnClickListener(v -> {
            startActivity(new Intent(MechanicDashboardActivity.this, AddGarageActivity.class));
        });

        // Manage Garages Card Click
        cardManageGarages.setOnClickListener(v -> {
            startActivity(new Intent(MechanicDashboardActivity.this, MechanicManageGaragesActivity.class));
        });

        // Demands Card Click
        cardDemands.setOnClickListener(v -> {
            startActivity(new Intent(MechanicDashboardActivity.this, MechanicDemandsActivity.class));
        });

        // Logout Button
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MechanicDashboardActivity.this, MainActivity2.class));
            finish();
        });

        profileIcon.setOnClickListener(v -> {
            startActivity(new Intent(MechanicDashboardActivity.this, profile.class));
        });

        // Chat Icon Click
        chatIcon.setOnClickListener(v -> {
            startActivity(new Intent(MechanicDashboardActivity.this, MechanicChatGaragesActivity.class));
        });

        loadUserProfile();
    }

    private void loadUserProfile() {
        if (mAuth.getCurrentUser() == null) {
            return;
        }
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                            Picasso.get()
                                    .load(user.getPhotoUrl())
                                    .placeholder(R.drawable.ic_profile_placeholder)
                                    .error(R.drawable.ic_profile_placeholder)
                                    .into(profileIcon);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Keep default image on error
                });
    }
}
