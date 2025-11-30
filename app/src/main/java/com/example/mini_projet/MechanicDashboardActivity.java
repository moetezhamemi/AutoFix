package com.example.mini_projet;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MechanicDashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CardView cardAddGarage, cardManageGarages, cardDemands;
    private ImageView logoutButton;

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
            startActivity(new Intent(MechanicDashboardActivity.this,MechanicDemandsActivity.class));
        });

        // Logout Button
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MechanicDashboardActivity.this, MainActivity2.class));
            finish();
        });
    }
}
