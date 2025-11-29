package com.example.mini_projet;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mini_projet.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import com.squareup.picasso.Picasso;

public class profile extends AppCompatActivity {

    private ImageView profileImage;
    private ImageButton btnAddPhoto;
    private View icAddIndicator;

    // Champs texte + edit + boutons
    private TextView nameText, emailText, phoneText;
    private EditText nameEdit, emailEdit, phoneEdit;
    private ImageButton nameEditButton, emailEditButton, phoneEditButton;
    private LinearLayout nameContainer, emailContainer, phoneContainer;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        profileImage.setImageURI(imageUri);
                        hideAddPhotoButtons();
                        uploadImageToFirebase(imageUri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Vues photo
        profileImage = findViewById(R.id.profile_image);
        btnAddPhoto = findViewById(R.id.btn_add_photo);
        icAddIndicator = findViewById(R.id.ic_add_indicator);

        // Vues texte
        nameText = findViewById(R.id.name_text);
        emailText = findViewById(R.id.email_text);
        phoneText = findViewById(R.id.phone_text);
        findViewById(R.id.password_edit_button).setOnClickListener(v -> showPasswordChangeDialog());

        nameEdit = findViewById(R.id.name_edit);
        emailEdit = findViewById(R.id.email_edit);
        phoneEdit = findViewById(R.id.phone_edit);

        nameEditButton = findViewById(R.id.name_edit_button);
        emailEditButton = findViewById(R.id.email_edit_button);
        phoneEditButton = findViewById(R.id.phone_edit_button);

        nameContainer = findViewById(R.id.name_container);
        emailContainer = findViewById(R.id.email_container);
        phoneContainer = findViewById(R.id.phone_container);

        // Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Clic photo
        profileImage.setOnClickListener(v -> openGallery());
        btnAddPhoto.setOnClickListener(v -> openGallery());
        findViewById(R.id.profile_image_container).setOnClickListener(v -> openGallery());

        // Boutons
        findViewById(R.id.back_button).setOnClickListener(v -> finish());

        findViewById(R.id.logout_button).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(profile.this, MainActivity2.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // === ACTIVATION DES CHAMPS ÉDITABLES ===
        setupEditableField(nameContainer, nameText, nameEdit, nameEditButton, "name");
        setupEditableField(emailContainer, emailText, emailEdit, emailEditButton, "email");
        setupEditableField(phoneContainer, phoneText, phoneEdit, phoneEditButton, "phone");
        loadUserProfile();
    }

    private void setupEditableField(LinearLayout container, TextView textView, EditText editText,
            ImageButton editButton, String fieldName) {
        editText.setText(textView.getText());

        editButton.setOnClickListener(v -> {
            if (editText.getVisibility() == View.VISIBLE) {
                // Mode validation
                String newValue = editText.getText().toString().trim();

                if (newValue.isEmpty() && !fieldName.equals("phone")) {
                    Toast.makeText(this, "Ce champ ne peut pas être vide", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Mise à jour visuelle (toujours faite)
                textView.setText(newValue);
                textView.setVisibility(View.VISIBLE);
                editText.setVisibility(View.GONE);
                editButton.setImageResource(R.drawable.ic_edit_pencil);

                // SAUVEGARDE DANS FIRESTORE : on sauvegarde TOUT... SAUF l'email
                if (!fieldName.equals("email")) {
                    String uid = auth.getCurrentUser().getUid();
                    db.collection("users").document(uid)
                            .update(fieldName, newValue)
                            .addOnSuccessListener(
                                    aVoid -> Toast.makeText(this, "Mis à jour !", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(
                                    e -> Toast.makeText(this, "Erreur sauvegarde", Toast.LENGTH_LONG).show());
                } else {
                    // Pour l'email : on ne sauvegarde RIEN ici → on passe directement à la
                    // réauthentification
                    showReauthPopup(newValue);
                }

            } else {
                // Mode édition
                textView.setVisibility(View.GONE);
                editText.setVisibility(View.VISIBLE);
                editText.requestFocus();
                editText.setSelection(editText.getText().length());
                editButton.setImageResource(R.drawable.ic_check_green);
            }
        });

        // Sauvegarde quand on sort du champ
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && editText.getVisibility() == View.VISIBLE) {
                editButton.performClick();
            }
        });
    }

    private void showReauthPopup(String newEmail) {
        View view = getLayoutInflater().inflate(R.layout.dialog_reauth_email, null);
        EditText passwordInput = view.findViewById(R.id.password_input);
        MaterialButton btnConfirm = view.findViewById(R.id.btn_confirm_email);
        MaterialButton btnCancel = view.findViewById(R.id.btn_cancel);

        AlertDialog dialog = new AlertDialog.Builder(this,
                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                .setView(view)
                .setCancelable(false)
                .create();

        dialog.show();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String password = passwordInput.getText().toString().trim();
            if (password.isEmpty()) {
                Toast.makeText(this, "Mot de passe requis", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = auth.getCurrentUser();
            if (user == null)
                return;

            if (newEmail.equals(user.getEmail())) {
                Toast.makeText(this, "L'email est identique à celui actuel", Toast.LENGTH_SHORT).show();
                return;
            }

            // Vérifier si l'email est déjà utilisé dans Firestore
            db.collection("users")
                    .whereEqualTo("email", newEmail)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            Toast.makeText(this, "Cet email est déjà utilisé", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Reauthentification avant tout changement
                        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
                        user.reauthenticate(credential)
                                .addOnSuccessListener(aVoid -> {
                                    // Mettre à jour l'email dans Firebase Auth
                                    user.updateEmail(newEmail)
                                            .addOnSuccessListener(aVoid2 -> {
                                                // Maintenant mettre à jour Firestore
                                                db.collection("users").document(user.getUid())
                                                        .update("email", newEmail)
                                                        .addOnSuccessListener(aVoid3 -> {
                                                            Toast.makeText(this, "Email mis à jour !",
                                                                    Toast.LENGTH_SHORT).show();
                                                            // Déconnexion obligatoire
                                                            auth.signOut();
                                                            Intent intent = new Intent(profile.this,
                                                                    MainActivity2.class);
                                                            intent.putExtra("EMAIL_PREFILL", newEmail);
                                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                            startActivity(intent);
                                                            finish();
                                                        });
                                            })
                                            .addOnFailureListener(e -> Toast
                                                    .makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_LONG)
                                                    .show());
                                })
                                .addOnFailureListener(
                                        e -> Toast.makeText(this, "Mot de passe incorrect", Toast.LENGTH_LONG).show());

                    })
                    .addOnFailureListener(e -> Toast
                            .makeText(this, "Erreur vérification email : " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }

    private void loadUserProfile() {
        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            nameText.setText(user.getName() != null ? user.getName() : "Nom inconnu");
                            nameEdit.setText(nameText.getText());

                            emailText.setText(
                                    user.getEmail() != null ? user.getEmail() : auth.getCurrentUser().getEmail());
                            emailEdit.setText(emailText.getText());

                            phoneText.setText(user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone()
                                    : "Non renseigné");
                            phoneEdit.setText(phoneText.getText());

                            if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                                Picasso.get().load(user.getPhotoUrl()).into(profileImage);
                                hideAddPhotoButtons();
                            } else {
                                showAddPhotoMode();
                            }
                        }
                    }
                });
    }

    private void reauthenticateAndChangeEmail(FirebaseUser user, String newEmail, String password) {

        String oldEmail = user.getEmail();
        AuthCredential credential = EmailAuthProvider.getCredential(oldEmail, password);

        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // Maintenant on peut modifier l’email dans Firebase Auth
                    user.updateEmail(newEmail)
                            .addOnSuccessListener(aVoid2 -> {
                                // Mettre à jour Firestore aussi
                                db.collection("users").document(user.getUid())
                                        .update("email", newEmail);

                                Toast.makeText(this, "Email mis à jour !", Toast.LENGTH_SHORT).show();

                                // Déconnexion obligatoire
                                auth.signOut();

                                Intent intent = new Intent(profile.this, MainActivity2.class);
                                intent.putExtra("EMAIL_PREFILL", newEmail);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Erreur Auth : " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Mot de passe incorrect", Toast.LENGTH_LONG).show();
                });
    }

    private void showAddPhotoMode() {
        profileImage.setImageResource(R.drawable.ic_profile_placeholder);
        btnAddPhoto.setVisibility(View.VISIBLE);
        icAddIndicator.setVisibility(View.VISIBLE);
    }

    private void hideAddPhotoButtons() {
        btnAddPhoto.setVisibility(View.GONE);
        icAddIndicator.setVisibility(View.GONE);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void uploadImageToFirebase(Uri imageUri) {
        String uid = auth.getCurrentUser().getUid();

        // Utiliser Cloudinary au lieu de Firebase Storage
        CloudinaryHelper.uploadImage(this, imageUri, new CloudinaryHelper.CloudinaryUploadCallback() {
            @Override
            public void onUploadStart() {
                Toast.makeText(profile.this, "Upload en cours...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUploadProgress(int progress) {
                // Optionnel: afficher une barre de progression
                // Pour l'instant, on ne fait rien
            }

            @Override
            public void onUploadSuccess(String imageUrl) {
                // Sauvegarder l'URL Cloudinary dans Firestore
                db.collection("users").document(uid)
                        .update("photoUrl", imageUrl)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(profile.this, "Photo mise à jour !", Toast.LENGTH_SHORT).show();
                            Picasso.get().load(imageUrl).into(profileImage);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(profile.this, "Erreur de sauvegarde dans Firestore", Toast.LENGTH_LONG)
                                    .show();
                        });
            }

            @Override
            public void onUploadError(String errorMessage) {
                Toast.makeText(profile.this, "Erreur upload: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showPasswordChangeDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_change_password, null);

        EditText oldPass = view.findViewById(R.id.old_password);
        EditText newPass = view.findViewById(R.id.new_password);
        EditText confirmPass = view.findViewById(R.id.confirm_password);

        TextView oldPassError = view.findViewById(R.id.old_password_error);
        TextView newPassError = view.findViewById(R.id.new_password_error);
        TextView confirmPassError = view.findViewById(R.id.confirm_password_error);

        AlertDialog dialog = new AlertDialog.Builder(this,
                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                .setView(view)
                .setCancelable(true)
                .create();

        dialog.show();

        // --- VALIDATION EN TEMPS RÉEL ---
        oldPass.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    oldPassError.setText("Mot de passe actuel requis");
                    oldPassError.setVisibility(View.VISIBLE);
                } else {
                    oldPassError.setVisibility(View.GONE);
                }
            }
        });

        newPass.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String val = s.toString().trim();
                if (val.isEmpty()) {
                    newPassError.setText("Nouveau mot de passe requis");
                    newPassError.setVisibility(View.VISIBLE);
                } else if (val.length() < 6) {
                    newPassError.setText("Le mot de passe doit contenir au moins 6 caractères");
                    newPassError.setVisibility(View.VISIBLE);
                } else {
                    newPassError.setVisibility(View.GONE);
                }
            }
        });

        confirmPass.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().trim().equals(newPass.getText().toString().trim())) {
                    confirmPassError.setText("Les mots de passe ne correspondent pas");
                    confirmPassError.setVisibility(View.VISIBLE);
                } else {
                    confirmPassError.setVisibility(View.GONE);
                }
            }
        });

        // --- BOUTON ANNULER ---
        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());

        // --- BOUTON CONFIRMER ---
        view.findViewById(R.id.btn_confirm_password).setOnClickListener(v -> {
            String oldP = oldPass.getText().toString().trim();
            String newP = newPass.getText().toString().trim();
            String confirmP = confirmPass.getText().toString().trim();

            // Vérification finale
            if (oldP.isEmpty() || newP.isEmpty() || newP.length() < 6 || !newP.equals(confirmP)) {
                Toast.makeText(this, "Veuillez corriger les erreurs avant de continuer", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null)
                return;

            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldP);
            user.reauthenticate(credential)
                    .addOnSuccessListener(aVoid -> {
                        user.updatePassword(newP)
                                .addOnSuccessListener(aVoid2 -> {
                                    Toast.makeText(this, "Mot de passe mis à jour !", Toast.LENGTH_LONG).show();
                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e -> {
                                    newPassError.setText("Erreur lors de la mise à jour : " + e.getMessage());
                                    newPassError.setVisibility(View.VISIBLE);
                                });
                    })
                    .addOnFailureListener(e -> {
                        oldPassError.setText("Mot de passe actuel incorrect");
                        oldPassError.setVisibility(View.VISIBLE);
                    });
        });
    }

    private boolean validateOldPass(EditText et, TextView error) {
        String s = et.getText().toString().trim();
        if (s.isEmpty()) {
            error.setText("Old password is required");
            error.setVisibility(View.VISIBLE);
            return false;
        }
        error.setVisibility(View.GONE);
        return true;
    }

    private boolean validateNewPass(EditText et, TextView error) {
        String s = et.getText().toString().trim();
        if (s.isEmpty()) {
            error.setText("New password is required");
            error.setVisibility(View.VISIBLE);
            return false;
        } else if (s.length() < 6) {
            error.setText("Must be at least 6 characters");
            error.setVisibility(View.VISIBLE);
            return false;
        }
        error.setVisibility(View.GONE);
        return true;
    }

    private boolean validateConfirmPass(EditText newPass, EditText confirm, TextView error) {
        String s1 = newPass.getText().toString().trim();
        String s2 = confirm.getText().toString().trim();

        if (!s1.equals(s2)) {
            error.setText("Passwords do not match");
            error.setVisibility(View.VISIBLE);
            return false;
        }
        error.setVisibility(View.GONE);
        return true;
    }

    private abstract class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }

    private void changeUserPassword(String oldPassword, String newPassword) {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null)
            return;

        String email = user.getEmail();
        AuthCredential credential = EmailAuthProvider.getCredential(email, oldPassword);

        // 1. Reauthentification obligatoire
        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // 2. Mise à jour du mot de passe
                    user.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_LONG).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error updating password: " + e.getMessage(), Toast.LENGTH_LONG)
                                        .show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Incorrect old password", Toast.LENGTH_LONG).show();
                });
    }

}