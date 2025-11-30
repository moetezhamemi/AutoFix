package com.example.mini_projet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.mini_projet.models.Garage;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddGarageActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;

    // Form fields
    private EditText etGarageName, etGarageDescription, etGaragePhone;
    private TextView tvGarageNameError, tvGarageDescriptionError, tvGaragePhoneError;
    private TextView tvLocationStatus, tvLocationError, tvPhotoError;
    private Button btnGetLocation, btnSelectPhoto, btnSubmitGarage;
    private CircleImageView ivPhotoPreview;
    private ImageView btnBack;

    private GeoPoint capturedLocation = null;
    private Uri selectedPhotoUri = null;
    private ActivityResultLauncher<String> imagePickerLauncher;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_garage);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        etGarageName = findViewById(R.id.etGarageName);
        etGarageDescription = findViewById(R.id.etGarageDescription);
        etGaragePhone = findViewById(R.id.etGaragePhone);

        tvGarageNameError = findViewById(R.id.tvGarageNameError);
        tvGarageDescriptionError = findViewById(R.id.tvGarageDescriptionError);
        tvGaragePhoneError = findViewById(R.id.tvGaragePhoneError);
        tvLocationStatus = findViewById(R.id.tvLocationStatus);
        tvLocationError = findViewById(R.id.tvLocationError);
        tvPhotoError = findViewById(R.id.tvPhotoError);

        btnGetLocation = findViewById(R.id.btnGetLocation);
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto);
        btnSubmitGarage = findViewById(R.id.btnSubmitGarage);
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview);

        // Show placeholder initially
        ivPhotoPreview.setVisibility(View.VISIBLE);

        // Initialize image picker
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedPhotoUri = uri;
                        ivPhotoPreview.setImageURI(uri);
                        ivPhotoPreview.setVisibility(View.VISIBLE);
                        tvPhotoError.setVisibility(View.GONE);
                    }
                });

        // Setup click listeners
        btnBack.setOnClickListener(v -> finish());
        btnGetLocation.setOnClickListener(v -> requestLocationPermissionAndCapture());
        btnSelectPhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnSubmitGarage.setOnClickListener(v -> submitGarageRequest());
    }

    private void submitGarageRequest() {
        boolean nameOk = validateGarageName();
        boolean descOk = validateDescription();
        boolean phoneOk = validatePhone();
        boolean locationOk = validateLocation();
        boolean photoOk = validatePhoto();

        if (!(nameOk && descOk && phoneOk && locationOk && photoOk))
            return;

        String name = etGarageName.getText().toString().trim();
        String description = etGarageDescription.getText().toString().trim();
        String phone = etGaragePhone.getText().toString().trim();
        String mechanicId = mAuth.getCurrentUser().getUid();

        // Create garage with enabled=false
        String garageId = db.collection("garages").document().getId();
        Garage garage = new Garage(garageId, name, description, capturedLocation, phone, mechanicId, "");

        db.collection("garages").document(garageId)
                .set(garage)
                .addOnSuccessListener(aVoid -> {
                    if (selectedPhotoUri != null) {
                        uploadPhotoToCloudinary(garageId);
                    } else {
                        showSuccessMessage();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error creating garage request", Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadPhotoToCloudinary(String garageId) {
        Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show();

        CloudinaryHelper.uploadImage(this, selectedPhotoUri, new CloudinaryHelper.CloudinaryUploadCallback() {
            @Override
            public void onUploadStart() {
            }

            @Override
            public void onUploadProgress(int progress) {
            }

            @Override
            public void onUploadSuccess(String imageUrl) {
                db.collection("garages").document(garageId)
                        .update("photoUrl", imageUrl)
                        .addOnSuccessListener(aVoid -> showSuccessMessage())
                        .addOnFailureListener(e -> {
                            Toast.makeText(AddGarageActivity.this,
                                    "Photo uploaded but failed to save URL", Toast.LENGTH_SHORT).show();
                            showSuccessMessage();
                        });
            }

            @Override
            public void onUploadError(String error) {
                Toast.makeText(AddGarageActivity.this,
                        "Photo upload failed: " + error, Toast.LENGTH_SHORT).show();
                showSuccessMessage();
            }
        });
    }

    private void showSuccessMessage() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Request Sent")
                .setMessage("Your garage request has been sent to the admin. We will inform you by email when your request is accepted. Thank you!")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> finish())
                .show();
    }

    // Validation methods
    private boolean validateGarageName() {
        String name = etGarageName.getText().toString().trim();
        if (name.isEmpty()) {
            tvGarageNameError.setText("Garage name is required");
            tvGarageNameError.setVisibility(View.VISIBLE);
            return false;
        }
        tvGarageNameError.setVisibility(View.GONE);
        return true;
    }

    private boolean validateDescription() {
        String desc = etGarageDescription.getText().toString().trim();
        if (desc.isEmpty()) {
            tvGarageDescriptionError.setText("Description is required");
            tvGarageDescriptionError.setVisibility(View.VISIBLE);
            return false;
        }
        tvGarageDescriptionError.setVisibility(View.GONE);
        return true;
    }

    private boolean validatePhone() {
        String phone = etGaragePhone.getText().toString().trim();
        if (phone.isEmpty()) {
            tvGaragePhoneError.setText("Phone is required");
            tvGaragePhoneError.setVisibility(View.VISIBLE);
            return false;
        }
        tvGaragePhoneError.setVisibility(View.GONE);
        return true;
    }

    private boolean validateLocation() {
        if (capturedLocation == null) {
            tvLocationError.setText("Please capture your location");
            tvLocationError.setVisibility(View.VISIBLE);
            return false;
        }
        tvLocationError.setVisibility(View.GONE);
        return true;
    }

    private boolean validatePhoto() {
        if (selectedPhotoUri == null) {
            tvPhotoError.setText("Photo is required");
            tvPhotoError.setVisibility(View.VISIBLE);
            return false;
        }
        tvPhotoError.setVisibility(View.GONE);
        return true;
    }

    // Location methods
    private void requestLocationPermissionAndCapture() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            captureLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void captureLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        tvLocationStatus.setText("Getting location...");
        tvLocationStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        tvLocationStatus.setVisibility(View.VISIBLE);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        capturedLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                        tvLocationStatus.setText("✓ Location captured: " +
                                String.format("%.4f, %.4f", location.getLatitude(), location.getLongitude()));
                        tvLocationStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        tvLocationError.setVisibility(View.GONE);
                    } else {
                        tvLocationStatus.setText("Failed to get location");
                        tvLocationStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    }
                })
                .addOnFailureListener(e -> {
                    tvLocationStatus.setText("Error: " + e.getMessage());
                    tvLocationStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                });
    }
}
