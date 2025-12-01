package com.example.mini_projet;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import de.hdodenhof.circleimageview.CircleImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.NestedScrollView;
import androidx.activity.EdgeToEdge;

import com.example.mini_projet.models.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SingupClientActivity extends AppCompatActivity {

    // UI
    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private TextView tvNameError, tvEmailError, tvPasswordError, tvConfirmError;
    private Button btnCreateAccount;
    private Button btnGoogleSignIn;
    private TextView loginLink;
    private NestedScrollView scrollView;
    private ConstraintLayout container;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText etPhone;
    private TextView tvPhoneError;

    // Location fields (for mechanics)
    private Button btnGetLocation;
    private TextView tvLocationLabel, tvLocationError, tvLocationStatus;
    private GeoPoint capturedLocation = null;
    private FusedLocationProviderClient fusedLocationClient;

    // Photo upload
    private Button btnSelectPhoto;
    private TextView tvPhotoStatus, tvPhotoLabel, tvPhotoError;
    private CircleImageView ivPhotoPreview;
    private Uri selectedPhotoUri = null;
    private ActivityResultLauncher<String> imagePickerLauncher;

    // User type
    private String userType = "client"; // default to client

    // Google Sign-In
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_singup_client);

        // Get user type from intent (default to "client")
        userType = getIntent().getStringExtra("USER_TYPE");
        if (userType == null || userType.isEmpty()) {
            userType = "client";
        }

        // Bind views
        container = findViewById(R.id.container);
        scrollView = findViewById(R.id.scroll_view);

        etName = findViewById(R.id.name);
        etEmail = findViewById(R.id.email);
        etPassword = findViewById(R.id.password);
        etConfirmPassword = findViewById(R.id.confirm_password);

        tvNameError = findViewById(R.id.name_error);
        tvEmailError = findViewById(R.id.email_error);
        tvPasswordError = findViewById(R.id.password_error);
        tvConfirmError = findViewById(R.id.confirm_error);

        btnCreateAccount = findViewById(R.id.createAccountButton);
        btnGoogleSignIn = findViewById(R.id.googleSignInButton);
        loginLink = findViewById(R.id.textView10);

        // Firebase init
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // phone
        etPhone = findViewById(R.id.phone);
        tvPhoneError = findViewById(R.id.phone_error);

        // Photo upload
        btnSelectPhoto = findViewById(R.id.selectPhotoButton);
        tvPhotoStatus = findViewById(R.id.photoStatus);
        tvPhotoLabel = findViewById(R.id.photoLabel);
        tvPhotoError = findViewById(R.id.photo_error);
        ivPhotoPreview = findViewById(R.id.photoPreview);

        // Update photo label based on user type
        if (userType.equalsIgnoreCase("mechanic")) {
            tvPhotoLabel.setText("Profile Photo (Required)");
        } else {
            tvPhotoLabel.setText("Profile Photo (Optional)");
        }

        // Initialize image picker
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedPhotoUri = uri;
                        ivPhotoPreview.setImageURI(uri);
                        ivPhotoPreview.setVisibility(View.VISIBLE);
                        tvPhotoStatus.setText("✓ Photo selected");
                        tvPhotoStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        tvPhotoError.setVisibility(View.GONE);
                    }
                });

        // Location fields (for mechanics)
        btnGetLocation = findViewById(R.id.getLocationButton);
        tvLocationLabel = findViewById(R.id.locationLabel);
        tvLocationError = findViewById(R.id.location_error);
        tvLocationStatus = findViewById(R.id.locationStatus);

        // Location fields are no longer used for mechanics during signup
        tvLocationLabel.setVisibility(View.GONE);
        btnGetLocation.setVisibility(View.GONE);
        tvLocationError.setVisibility(View.GONE);
        tvLocationStatus.setVisibility(View.GONE);

        // Animation
        container.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        container.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        Animation fadeInUp = AnimationUtils.loadAnimation(
                                SingupClientActivity.this, R.anim.fade_in_up);
                        container.startAnimation(fadeInUp);
                    }
                });

        scrollView.requestFocus();

        // Validation listeners
        etName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateName();
            }
        });
        etEmail.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateEmail();
            }
        });
        etPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePassword();
                validateConfirm();
            }
        });
        etPhone.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePhone();
            }
        });

        etConfirmPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateConfirm();
            }
        });

        // Photo selection button
        btnSelectPhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        // Location button (for mechanics)
        if (userType.equalsIgnoreCase("mechanic")) {
            btnGetLocation.setOnClickListener(v -> requestLocationPermissionAndCapture());
        }

        // Create Account button
        btnCreateAccount.setOnClickListener(v -> signupClient());

        // Google Sign-In button
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        // Navigate to Login
        loginLink.setOnClickListener(v -> startActivity(new Intent(SingupClientActivity.this, MainActivity2.class)));
    }

    private void signupClient() {
        boolean nameOk = validateName();
        boolean emailOk = validateEmail();
        boolean passwordOk = validatePassword();
        boolean confirmOk = validateConfirm();
        boolean phoneOk = validatePhone();

        // Validate photo for mechanics
        boolean photoOk = true;
        if (userType.equalsIgnoreCase("mechanic")) {
            photoOk = validatePhoto();
        }

        // Location validation removed for mechanics
        boolean locationOk = true;

        if (!(nameOk && emailOk && passwordOk && confirmOk && phoneOk && photoOk && locationOk))
            return;

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        btnCreateAccount.setEnabled(false);
        Toast.makeText(this, "Creating account, please wait...", Toast.LENGTH_SHORT).show();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    btnCreateAccount.setEnabled(true);

                    if (task.isSuccessful()) {
                        String userId = auth.getCurrentUser().getUid();

                        // Use captured location for mechanics, default for clients
                        GeoPoint geoPoint = (userType.equalsIgnoreCase("mechanic") && capturedLocation != null)
                                ? capturedLocation
                                : new GeoPoint(0, 0);

                        // Create user with correct role
                        User user = new User(userId, name, email, geoPoint, "", phone, userType);

                        // Save to Firestore
                        db.collection("users").document(userId).set(user)
                                .addOnSuccessListener(aVoid -> {
                                    // Upload photo if selected
                                    if (selectedPhotoUri != null) {
                                        uploadPhotoToCloudinary(userId);
                                    } else {
                                        // No photo, show appropriate message based on user type
                                        // Both Client and Mechanic can login immediately
                                        Toast.makeText(SingupClientActivity.this, "Account created successfully!",
                                                Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(SingupClientActivity.this, MainActivity2.class));
                                        finish();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(SingupClientActivity.this, "Error saving data", Toast.LENGTH_LONG)
                                            .show();
                                    finish();
                                });

                    } else {
                        Exception e = task.getException();
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(this, "This email is already registered", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Signup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                        e.printStackTrace();
                    }
                });
    }

    // Validation helpers
    private boolean validateName() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            tvNameError.setText("Name is required");
            tvNameError.setVisibility(TextView.VISIBLE);
            return false;
        }
        tvNameError.setVisibility(TextView.GONE);
        return true;
    }

    private boolean validatePhone() {
        String phone = etPhone.getText().toString().trim();
        if (phone.isEmpty()) {
            tvPhoneError.setText("Phone is required");
            tvPhoneError.setVisibility(TextView.VISIBLE);
            return false;
        } else if (!android.util.Patterns.PHONE.matcher(phone).matches()) {
            tvPhoneError.setText("Enter valid phone");
            tvPhoneError.setVisibility(TextView.VISIBLE);
            return false;
        }
        tvPhoneError.setVisibility(TextView.GONE);
        return true;
    }

    private boolean validateEmail() {
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            tvEmailError.setText("Email is required");
            tvEmailError.setVisibility(TextView.VISIBLE);
            return false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tvEmailError.setText("Enter valid email");
            tvEmailError.setVisibility(TextView.VISIBLE);
            return false;
        }
        tvEmailError.setVisibility(TextView.GONE);
        return true;
    }

    private boolean validatePassword() {
        String pwd = etPassword.getText().toString();
        if (pwd.isEmpty()) {
            tvPasswordError.setText("Password required");
            tvPasswordError.setVisibility(TextView.VISIBLE);
            return false;
        } else if (pwd.length() < 6) {
            tvPasswordError.setText("At least 6 characters");
            tvPasswordError.setVisibility(TextView.VISIBLE);
            return false;
        }
        tvPasswordError.setVisibility(TextView.GONE);
        return true;
    }

    private boolean validateConfirm() {
        String pwd = etPassword.getText().toString();
        String confirm = etConfirmPassword.getText().toString();
        if (confirm.isEmpty()) {
            tvConfirmError.setText("Confirm required");
            tvConfirmError.setVisibility(TextView.VISIBLE);
            return false;
        } else if (!confirm.equals(pwd)) {
            tvConfirmError.setText("Passwords do not match");
            tvConfirmError.setVisibility(TextView.VISIBLE);
            return false;
        }
        tvConfirmError.setVisibility(TextView.GONE);
        return true;
    }

    private boolean validateLocation() {
        if (capturedLocation == null) {
            tvLocationError.setText("Please capture your location");
            tvLocationError.setVisibility(TextView.VISIBLE);
            return false;
        }
        tvLocationError.setVisibility(TextView.GONE);
        return true;
    }

    private boolean validatePhoto() {
        if (selectedPhotoUri == null) {
            tvPhotoError.setText("Photo is required for mechanics");
            tvPhotoError.setVisibility(TextView.VISIBLE);
            return false;
        }
        tvPhotoError.setVisibility(TextView.GONE);
        return true;
    }

    private void requestLocationPermissionAndCapture() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            captureLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
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
                Toast.makeText(this, "Location permission is required for mechanics", Toast.LENGTH_LONG).show();
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

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        capturedLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                        tvLocationStatus.setText("✓ Location captured: " +
                                String.format("%.4f, %.4f", location.getLatitude(), location.getLongitude()));
                        tvLocationStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        tvLocationError.setVisibility(View.GONE);
                    } else {
                        tvLocationStatus.setText("Unable to get location. Try again.");
                        tvLocationStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    }
                })
                .addOnFailureListener(e -> {
                    tvLocationStatus.setText("Error getting location");
                    tvLocationStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadPhotoToCloudinary(String userId) {
        if (selectedPhotoUri == null)
            return;

        Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show();

        CloudinaryHelper.uploadImage(this, selectedPhotoUri, new CloudinaryHelper.CloudinaryUploadCallback() {
            @Override
            public void onUploadStart() {
                Log.d("SignupClient", "Photo upload started");
            }

            @Override
            public void onUploadProgress(int progress) {
                Log.d("SignupClient", "Upload progress: " + progress + "%");
            }

            @Override
            public void onUploadSuccess(String imageUrl) {
                // Update user photo URL in Firestore
                db.collection("users").document(userId)
                        .update("photoUrl", imageUrl)
                        .addOnSuccessListener(aVoid -> {
                            // Both Client and Mechanic can login immediately
                            Toast.makeText(SingupClientActivity.this, "Account created successfully!",
                                    Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SingupClientActivity.this, MainActivity2.class));
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(SingupClientActivity.this, "Photo uploaded but failed to save URL",
                                    Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SingupClientActivity.this, MainActivity2.class));
                            finish();
                        });
            }

            @Override
            public void onUploadError(String error) {
                Toast.makeText(SingupClientActivity.this, "Photo upload failed: " + error, Toast.LENGTH_SHORT).show();
                // Still proceed to login even if photo upload fails
                startActivity(new Intent(SingupClientActivity.this, MainActivity2.class));
                finish();
            }
        });
    }

    private abstract class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }


    private void signInWithGoogle() {
        // Sign out first to force account picker to show
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w("GoogleSignIn", "Google sign in failed", e);
                Toast.makeText(this, "Google sign-in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();
                            String email = firebaseUser.getEmail();
                            String name = firebaseUser.getDisplayName();
                            String photoUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "";

                            // Check if user already exists
                            db.collection("users").document(userId).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            // User exists, just login
                                            Toast.makeText(SingupClientActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(SingupClientActivity.this, home.class));
                                            finish();
                                        } else {
                                            // New user, create with the appropriate role (client or mechanic)
                                            User newUser = new User(userId, name, email, new GeoPoint(0, 0), photoUrl, "", userType);
                                            db.collection("users").document(userId).set(newUser)
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(SingupClientActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                                                        startActivity(new Intent(SingupClientActivity.this, home.class));
                                                        finish();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(SingupClientActivity.this, "Error saving user data", Toast.LENGTH_SHORT).show();
                                                    });
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(SingupClientActivity.this, "Error checking user", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Toast.makeText(SingupClientActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
