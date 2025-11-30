package com.example.mini_projet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

public class AdminDashboardActivity extends AppCompatActivity {

    private CardView cardManageDemands, cardManageGarages;
    private TextView pendingCount, pendingGaragesCount;
    private ImageView logoutButton;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        cardManageDemands = findViewById(R.id.cardManageDemands);
        cardManageGarages = findViewById(R.id.cardManageGarages);
        pendingCount = findViewById(R.id.pendingCount);
        pendingGaragesCount = findViewById(R.id.pendingGaragesCount);
        logoutButton = findViewById(R.id.logoutButton);

        // Load admin profile if available
        if (mAuth.getCurrentUser() != null) {
            // Ideally fetch admin details from Firestore
            // For now, just placeholder
        }

        // Setup click listeners
        cardManageDemands.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, ManageDemandsActivity.class));
        });

        cardManageGarages.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, ManageGaragesActivity.class));
        });

        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(AdminDashboardActivity.this, MainActivity2.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchPendingCount();
        fetchPendingGaragesCount();
    }

    private void fetchPendingCount() {
        // Fetch pending garage requests for "Manage Demands"
        db.collection("garages")
                .whereEqualTo("enabled", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    if (count > 0) {
                        pendingCount.setText(count + " Requests");
                        pendingCount.setVisibility(View.VISIBLE);
                    } else {
                        pendingCount.setText("No pending requests");
                        pendingCount.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    pendingCount.setText("Error loading");
                });
    }

    private void fetchPendingGaragesCount() {
        // Fetch total approved garages for "Manage Garages"
        db.collection("garages")
                .whereEqualTo("enabled", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    if (count > 0) {
                        pendingGaragesCount.setText(count + " Garages");
                        pendingGaragesCount.setVisibility(View.VISIBLE);
                    } else {
                        pendingGaragesCount.setText("No garages");
                        pendingGaragesCount.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    pendingGaragesCount.setText("Error loading");
                });
    }
}
