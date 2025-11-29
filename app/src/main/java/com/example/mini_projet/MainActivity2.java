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

public class MainActivity2 extends AppCompatActivity {

    // UI
    private EditText etEmail, etPassword;
    private TextView tvEmailError, tvPasswordError;
    private Button btnLogin;
    private TextView createAccount;
    private NestedScrollView scrollView;
    private ConstraintLayout container;

    // Firebase
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // ---- Bind views ----------------------------------------------------
        container   = findViewById(R.id.container);
        scrollView  = findViewById(R.id.scroll_view);

        etEmail     = findViewById(R.id.email);
        etPassword  = findViewById(R.id.password);
        tvEmailError    = findViewById(R.id.email_error);
        tvPasswordError = findViewById(R.id.password_error);
        btnLogin    = findViewById(R.id.loginButton);
        createAccount = findViewById(R.id.textView10);

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
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateEmail();
            }
        });

        etPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
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
                                // Login success, go to HomeActivity
                                Intent intent = new Intent(MainActivity2.this, home.class);
                                startActivity(intent);
                                finish();
                            } else {
                                // Login failed, show error
                                tvPasswordError.setText("Email or password is incorrect");
                                tvPasswordError.setVisibility(View.VISIBLE);
                            }
                        });
            }
        });

        // ---- Navigate to Sign-up ------------------------------------------
        createAccount.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity2.this, SingupClientActivity.class));
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


    private abstract class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void afterTextChanged(Editable s) {}
    }
}
