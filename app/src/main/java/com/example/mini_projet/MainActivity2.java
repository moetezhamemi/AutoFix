package com.example.mini_projet;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.NestedScrollView;
import androidx.activity.EdgeToEdge;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity2 extends AppCompatActivity {

    // Google Sign-In
    private Button btnGoogleSignIn;
    private com.google.android.gms.auth.api.signin.GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    private String selectedRole = "client"; // Default, but will be set by dialog

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // UI
    private EditText etEmail, etPassword;
    private TextView tvEmailError, tvPasswordError;
    private Button btnLogin;
    private TextView createAccount;
    private ConstraintLayout container;
    private NestedScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configure Google Sign-In
        com.google.android.gms.auth.api.signin.GoogleSignInOptions gso = new com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso);

        // ---- Bind views ----------------------------------------------------
        container = findViewById(R.id.container);
        scrollView = findViewById(R.id.scroll_view);

        etEmail = findViewById(R.id.email);
        etPassword = findViewById(R.id.password);
        tvEmailError = findViewById(R.id.email_error);
        tvPasswordError = findViewById(R.id.password_error);
        btnLogin = findViewById(R.id.loginButton);
        createAccount = findViewById(R.id.textView10);
        btnGoogleSignIn = findViewById(R.id.googleSignInButton);

        container.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        container.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        Animation fadeInUp = AnimationUtils.loadAnimation(
                                MainActivity2.this, R.anim.fade_in_up);
                        container.startAnimation(fadeInUp);
                    }
                });
        // === Gestion du changement d'email depuis le profil ===
        if (getIntent().hasExtra("EMAIL_PREFILL")) {
            String prefill = getIntent().getStringExtra("EMAIL_PREFILL");
            etEmail.setText(prefill);
        }

        if (getIntent().getBooleanExtra("SHOW_EMAIL_CHANGE_MESSAGE", false)) {
            Toast.makeText(this, "Email modifié ! Connectez-vous avec votre NOUVEL email", Toast.LENGTH_LONG).show();
        }
        scrollView.requestFocus();

        // Text Watchers for live validation
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
            }
        });

        // Login button
        btnLogin.setOnClickListener(v -> {
            boolean emailOk = validateEmail();
            boolean passwordOk = validatePassword();

            if (emailOk && passwordOk) {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString();

                // Firebase sign in
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // Login success, check if user is enabled
                                checkUserEnabled(mAuth.getCurrentUser().getUid());
                            } else {
                                // Login failed, show error
                                tvPasswordError.setText("Email or password is incorrect");
                                tvPasswordError.setVisibility(View.VISIBLE);
                            }
                        });
            }
        });

        // Google Sign-In Button
        btnGoogleSignIn.setOnClickListener(v -> showRoleSelectionDialog());

        // ---- Navigate to Sign-up ------------------------------------------
        createAccount.setOnClickListener(v -> {
            // Show dialog to choose user type
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Choose Account Type")
                    .setMessage("Please select the type of account you want to create:")
                    .setPositiveButton("Client", (dialog, which) -> {
                        Intent intent = new Intent(MainActivity2.this, SingupClientActivity.class);
                        intent.putExtra("USER_TYPE", "client");
                        startActivity(intent);
                    })
                    .setNegativeButton("Mechanic", (dialog, which) -> {
                        Intent intent = new Intent(MainActivity2.this, SingupClientActivity.class);
                        intent.putExtra("USER_TYPE", "mechanic");
                        startActivity(intent);
                    })
                    .setNeutralButton("Cancel", null)
                    .show();
        });
    }

    private void showRoleSelectionDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Complete your profile")
                .setMessage("Are you a Client or a Mechanic?")
                .setPositiveButton("Client", (dialog, which) -> {
                    selectedRole = "client";
                    signInWithGoogle();
                })
                .setNegativeButton("Mechanic", (dialog, which) -> {
                    selectedRole = "mechanic";
                    signInWithGoogle();
                })
                .setNeutralButton("Cancel", null)
                .show();
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
            com.google.android.gms.tasks.Task<com.google.android.gms.auth.api.signin.GoogleSignInAccount> task = com.google.android.gms.auth.api.signin.GoogleSignIn
                    .getSignedInAccountFromIntent(data);
            try {
                com.google.android.gms.auth.api.signin.GoogleSignInAccount account = task
                        .getResult(com.google.android.gms.common.api.ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (com.google.android.gms.common.api.ApiException e) {
                Toast.makeText(this, "Google sign-in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        com.google.firebase.auth.AuthCredential credential = com.google.firebase.auth.GoogleAuthProvider
                .getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();
                            String email = firebaseUser.getEmail();
                            String name = firebaseUser.getDisplayName();
                            String photoUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString()
                                    : "";

                            // Check if user already exists
                            db.collection("users").document(userId).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            // User exists, ignore selectedRole and use existing role
                                            checkUserEnabled(userId);
                                        } else {
                                            // New user, create with selectedRole
                                            com.example.mini_projet.models.User newUser = new com.example.mini_projet.models.User(
                                                    userId, name, email,
                                                    new com.google.firebase.firestore.GeoPoint(0, 0), photoUrl, "",
                                                    selectedRole);
                                            db.collection("users").document(userId).set(newUser)
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(MainActivity2.this,
                                                                "Account created successfully!", Toast.LENGTH_SHORT)
                                                                .show();
                                                        checkUserEnabled(userId);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(MainActivity2.this, "Error saving user data",
                                                                Toast.LENGTH_SHORT).show();
                                                    });
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(MainActivity2.this, "Error checking user", Toast.LENGTH_SHORT)
                                                .show();
                                    });
                        }
                    } else {
                        Toast.makeText(MainActivity2.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validateEmail() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            tvEmailError.setText("Email is required");
            tvEmailError.setVisibility(TextView.VISIBLE);
            return false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tvEmailError.setText("Enter a valid email address");
            tvEmailError.setVisibility(TextView.VISIBLE);
            return false;
        } else {
            tvEmailError.setVisibility(TextView.GONE);
            return true;
        }
    }

    private boolean validatePassword() {
        String pwd = etPassword.getText().toString();

        if (pwd.isEmpty()) {
            tvPasswordError.setText("Password is required");
            tvPasswordError.setVisibility(TextView.VISIBLE);
            return false;
        } else if (pwd.length() < 6) {
            tvPasswordError.setText("Password must be at least 6 characters");
            tvPasswordError.setVisibility(TextView.VISIBLE);
            return false;
        } else {
            tvPasswordError.setVisibility(TextView.GONE);
            return true;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkUserEnabled(currentUser.getUid());
        }
    }

    private void checkUserEnabled(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean enabled = documentSnapshot.getBoolean("enabled");
                        if (enabled != null && enabled) {
                            // User is enabled
                            String role = documentSnapshot.getString("role");
                            // Debug: Show role
                            Toast.makeText(MainActivity2.this, "Welcome " + role, Toast.LENGTH_SHORT).show();

                            if (role != null && role.trim().equalsIgnoreCase("admin")) {
                                // Redirect to Admin Dashboard
                                startActivity(new Intent(MainActivity2.this, AdminDashboardActivity.class));
                            } else if (role != null && role.trim().equalsIgnoreCase("mechanic")) {
                                // Redirect to Mechanic Dashboard
                                startActivity(new Intent(MainActivity2.this, MechanicDashboardActivity.class));
                            } else {
                                // Redirect to Client Home
                                startActivity(new Intent(MainActivity2.this, HomeActivity.class));
                            }
                            finish();
                        } else {
                            // User is disabled (e.g. mechanic pending approval)
                            mAuth.signOut();
                            new android.app.AlertDialog.Builder(MainActivity2.this)
                                    .setTitle("Account Pending")
                                    .setMessage(
                                            "Your request has been sent to the admin. We will inform you by email when your request is accepted. Thank you!")
                                    .setCancelable(false)
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    } else {
                        // User document not found? Should not happen usually
                        mAuth.signOut();
                        Toast.makeText(MainActivity2.this, "User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    mAuth.signOut();
                    Toast.makeText(MainActivity2.this, "Error checking user status: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
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
}
