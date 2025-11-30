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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditGarageActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;

    private EditText etGarageName, etGarageDescription, etGaragePhone;
    private TextView tvLocationStatus;
    private Button btnGetLocation, btnSelectPhoto, btnUpdateGarage;
    private CircleImageView ivPhotoPreview;
    private ImageView btnBack;

    private String garageId;
    private GeoPoint capturedLocation = null;
    private Uri selectedPhotoUri = null;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private String currentPhotoUrl = "";

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_garage);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        garageId = getIntent().getStringExtra("garageId");
        if (garageId == null) {
            Toast.makeText(this, "Error: Garage not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        fetchGarageDetails();

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedPhotoUri = uri;
                        ivPhotoPreview.setImageURI(uri);
                    }
                });

        setupListeners();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        etGarageName = findViewById(R.id.etGarageName);
        etGarageDescription = findViewById(R.id.etGarageDescription);
        etGaragePhone = findViewById(R.id.etGaragePhone);
        tvLocationStatus = findViewById(R.id.tvLocationStatus);
        btnGetLocation = findViewById(R.id.btnGetLocation);
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto);
        btnUpdateGarage = findViewById(R.id.btnUpdateGarage);
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnGetLocation.setOnClickListener(v -> requestLocationPermissionAndCapture());
        btnSelectPhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnUpdateGarage.setOnClickListener(v -> updateGarage());
    }

    private void fetchGarageDetails() {
        db.collection("garages").document(garageId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Garage garage = documentSnapshot.toObject(Garage.class);
                        if (garage != null) {
                            etGarageName.setText(garage.getName());
                            etGarageDescription.setText(garage.getDescription());
                            etGaragePhone.setText(garage.getPhone());
                            
                            if (garage.getAddress() != null) {
                                capturedLocation = garage.getAddress();
                                tvLocationStatus.setText(String.format("%.4f, %.4f", 
                                        capturedLocation.getLatitude(), capturedLocation.getLongitude()));
                            }

                            if (garage.getPhotoUrl() != null && !garage.getPhotoUrl().isEmpty()) {
                                currentPhotoUrl = garage.getPhotoUrl();
                                Picasso.get().load(currentPhotoUrl).into(ivPhotoPreview);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching details", Toast.LENGTH_SHORT).show());
    }

    private void updateGarage() {
        String name = etGarageName.getText().toString().trim();
        String description = etGarageDescription.getText().toString().trim();
        String phone = etGaragePhone.getText().toString().trim();

        if (name.isEmpty() || description.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedPhotoUri != null) {
            uploadPhotoAndSave(name, description, phone);
        } else {
            saveGarageData(name, description, phone, currentPhotoUrl);
        }
    }

    private void uploadPhotoAndSave(String name, String description, String phone) {
        Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show();
        CloudinaryHelper.uploadImage(this, selectedPhotoUri, new CloudinaryHelper.CloudinaryUploadCallback() {
            @Override
            public void onUploadStart() {}

            @Override
            public void onUploadProgress(int progress) {}

            @Override
            public void onUploadSuccess(String imageUrl) {
                saveGarageData(name, description, phone, imageUrl);
            }

            @Override
            public void onUploadError(String error) {
                Toast.makeText(EditGarageActivity.this, "Photo upload failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveGarageData(String name, String description, String phone, String photoUrl) {
        db.collection("garages").document(garageId)
                .update(
                        "name", name,
                        "description", description,
                        "phone", phone,
                        "photoUrl", photoUrl,
                        "address", capturedLocation
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Garage updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
    }

    private void requestLocationPermissionAndCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            captureLocation();
        } else {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void captureLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) return;

        tvLocationStatus.setText("Getting location...");
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                capturedLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                tvLocationStatus.setText(String.format("%.4f, %.4f", 
                        location.getLatitude(), location.getLongitude()));
            } else {
                tvLocationStatus.setText("Failed to get location");
            }
        });
    }
}
