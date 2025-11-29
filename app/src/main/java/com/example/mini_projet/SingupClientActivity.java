package com.example.mini_projet;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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

import com.example.mini_projet.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

public class SingupClientActivity extends AppCompatActivity {

    // UI
    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private TextView tvNameError, tvEmailError, tvPasswordError, tvConfirmError;
    private Button btnCreateAccount;
    private TextView loginLink;
    private NestedScrollView scrollView;
    private ConstraintLayout container;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText etPhone;
    private TextView tvPhoneError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_singup_client);

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
        loginLink = findViewById(R.id.textView10);

        // Firebase init
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        //phone
        etPhone = findViewById(R.id.phone);
        tvPhoneError = findViewById(R.id.phone_error);

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
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validateName(); }
        });
        etEmail.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validateEmail(); }
        });
        etPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePassword();
                validateConfirm();
            }
        });
        etPhone.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { validatePhone(); }
        });

        etConfirmPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validateConfirm(); }
        });

        // Create Account button
        btnCreateAccount.setOnClickListener(v -> signupClient());

        // Navigate to Login
        loginLink.setOnClickListener(v -> startActivity(new Intent(SingupClientActivity.this, MainActivity2.class)));
    }

    private void signupClient() {
        boolean nameOk = validateName();
        boolean emailOk = validateEmail();
        boolean passwordOk = validatePassword();
        boolean confirmOk = validateConfirm();
        boolean phoneOk = validatePhone();

        if (!(nameOk && emailOk && passwordOk && confirmOk && phoneOk)) return;

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        btnCreateAccount.setEnabled(false); // disable button immediately
        Toast.makeText(this, "Creating account, please wait...", Toast.LENGTH_SHORT).show();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    btnCreateAccount.setEnabled(true);

                    if (task.isSuccessful()) {

                        String userId = auth.getCurrentUser().getUid();
                        User user = new User(userId, name, email, new GeoPoint(0, 0), "", phone, "client");

                        // ON SAUVEGARDE D'ABORD DANS FIRESTORE
                        db.collection("users").document(userId).set(user)
                                .addOnSuccessListener(aVoid -> {
                                    // UNIQUEMENT APRÈS que c'est sauvegardé → on redirige
                                    Toast.makeText(SingupClientActivity.this, "Compte créé avec succès !", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(SingupClientActivity.this, MainActivity2.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(SingupClientActivity.this, "Erreur de sauvegarde", Toast.LENGTH_LONG).show();

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
        if (name.isEmpty()) { tvNameError.setText("Name is required"); tvNameError.setVisibility(TextView.VISIBLE); return false; }
        tvNameError.setVisibility(TextView.GONE);
        return true;
    }
    private boolean validatePhone() {
        String phone = etPhone.getText().toString().trim();
        if (phone.isEmpty()) { tvPhoneError.setText("Phone is required"); tvPhoneError.setVisibility(TextView.VISIBLE); return false; }
        else if (!android.util.Patterns.PHONE.matcher(phone).matches()) { tvPhoneError.setText("Enter valid phone"); tvPhoneError.setVisibility(TextView.VISIBLE); return false; }
        tvPhoneError.setVisibility(TextView.GONE);
        return true;
    }

    private boolean validateEmail() {
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) { tvEmailError.setText("Email is required"); tvEmailError.setVisibility(TextView.VISIBLE); return false; }
        else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { tvEmailError.setText("Enter valid email"); tvEmailError.setVisibility(TextView.VISIBLE); return false; }
        tvEmailError.setVisibility(TextView.GONE);
        return true;
    }
    private boolean validatePassword() {
        String pwd = etPassword.getText().toString();
        if (pwd.isEmpty()) { tvPasswordError.setText("Password required"); tvPasswordError.setVisibility(TextView.VISIBLE); return false; }
        else if (pwd.length() < 6) { tvPasswordError.setText("At least 6 characters"); tvPasswordError.setVisibility(TextView.VISIBLE); return false; }
        tvPasswordError.setVisibility(TextView.GONE);
        return true;
    }
    private boolean validateConfirm() {
        String pwd = etPassword.getText().toString();
        String confirm = etConfirmPassword.getText().toString();
        if (confirm.isEmpty()) { tvConfirmError.setText("Confirm required"); tvConfirmError.setVisibility(TextView.VISIBLE); return false; }
        else if (!confirm.equals(pwd)) { tvConfirmError.setText("Passwords do not match"); tvConfirmError.setVisibility(TextView.VISIBLE); return false; }
        tvConfirmError.setVisibility(TextView.GONE);
        return true;
    }

    private abstract class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void afterTextChanged(Editable s) {}
    }
}
