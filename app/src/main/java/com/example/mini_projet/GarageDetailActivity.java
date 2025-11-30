package com.example.mini_projet;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mini_projet.models.Garage;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

public class GarageDetailActivity extends AppCompatActivity {

    private ImageView garagePhoto, btnMessage, btnCall;
    private TextView garageName, ratingText, reviewCount, garageDescription;
    private Button btnReview, btnReportProblem;
    private FirebaseFirestore db;
    private String garageId;
    private Garage garage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_garage_detail);

        db = FirebaseFirestore.getInstance();
        garageId = getIntent().getStringExtra("garageId");

        if (garageId == null) {
            Toast.makeText(this, "Error loading garage", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        loadGarageDetails();
        setupListeners();
    }

    private void initializeViews() {
        garagePhoto = findViewById(R.id.garagePhoto);
        garageName = findViewById(R.id.garageName);
        ratingText = findViewById(R.id.ratingText);
        reviewCount = findViewById(R.id.reviewCount);
        garageDescription = findViewById(R.id.garageDescription);
        btnMessage = findViewById(R.id.btnMessage);
        btnCall = findViewById(R.id.btnCall);
        btnReview = findViewById(R.id.btnReview);
        btnReportProblem = findViewById(R.id.btnReportProblem);
    }

    private void loadGarageDetails() {
        db.collection("garages").document(garageId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        garage = documentSnapshot.toObject(Garage.class);
                        if (garage != null) {
                            displayGarageInfo();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading garage details", Toast.LENGTH_SHORT).show();
                });
    }

    private void displayGarageInfo() {
        garageName.setText(garage.getName());
        garageDescription.setText(garage.getDescription());
        ratingText.setText(String.format("%.1f", garage.getRating()));
        
        if (garage.getReviewCount() > 0) {
            reviewCount.setText("[" + garage.getReviewCount() + "+ Review]");
        } else {
            reviewCount.setText("[No reviews yet]");
        }

        if (garage.getPhotoUrl() != null && !garage.getPhotoUrl().isEmpty()) {
            Picasso.get().load(garage.getPhotoUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(garagePhoto);
        }
    }

    private void setupListeners() {
        btnCall.setOnClickListener(v -> {
            if (garage != null && garage.getPhone() != null) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + garage.getPhone()));
                startActivity(intent);
            }
        });

        btnMessage.setOnClickListener(v -> {
            Toast.makeText(this, "Message feature coming soon", Toast.LENGTH_SHORT).show();
        });

        btnReview.setOnClickListener(v -> {
            Toast.makeText(this, "Review feature coming soon", Toast.LENGTH_SHORT).show();
        });

        btnReportProblem.setOnClickListener(v -> {
            Toast.makeText(this, "Report feature coming soon", Toast.LENGTH_SHORT).show();
        });
    }
}
