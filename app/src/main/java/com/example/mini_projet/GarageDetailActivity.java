package com.example.mini_projet;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mini_projet.adapter.ReviewAdapter;
import com.example.mini_projet.models.Chat;
import com.example.mini_projet.models.Garage;
import com.example.mini_projet.models.Review;
import com.example.mini_projet.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class GarageDetailActivity extends AppCompatActivity {

    private ImageView garagePhoto, btnMessage, btnCall, btnBack;
    private TextView garageName, ratingText, reviewCount, garageDescription, noReviewsText;
    private Button btnSubmitReview;
    private LinearLayout reviewInputSection;
    private EditText etReviewComment;
    private RecyclerView reviewsRecyclerView;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String garageId;
    private Garage garage;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_garage_detail);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        garageId = getIntent().getStringExtra("garageId");

        if (garageId == null) {
            Toast.makeText(this, "Error loading garage", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        reviewList = new ArrayList<>();
        initializeViews();
        loadGarageDetails();
        loadReviews();
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
        btnBack = findViewById(R.id.btnBack);
        reviewInputSection = findViewById(R.id.reviewInputSection);
        etReviewComment = findViewById(R.id.etReviewComment);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);
        reviewsRecyclerView = findViewById(R.id.reviewsRecyclerView);
        noReviewsText = findViewById(R.id.noReviewsText);

        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        reviewAdapter = new ReviewAdapter(this, reviewList, currentUserId, new ReviewAdapter.OnReviewActionListener() {
            @Override
            public void onEdit(Review review) {
                showEditReviewDialog(review);
            }

            @Override
            public void onDelete(Review review) {
                showDeleteConfirmationDialog(review);
            }
        });
        reviewsRecyclerView.setAdapter(reviewAdapter);
    }

    private void showEditReviewDialog(Review review) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Comment");

        final EditText input = new EditText(this);
        input.setText(review.getComment());
        input.setSelection(input.getText().length());

        // Add padding and margin
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(40, 20, 40, 20);
        input.setLayoutParams(params);

        LinearLayout container = new LinearLayout(this);
        container.addView(input);
        container.setPadding(40, 20, 40, 20);

        builder.setView(container);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newComment = input.getText().toString().trim();
            if (!newComment.isEmpty()) {
                updateReview(review, newComment);
            } else {
                Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateReview(Review review, String newComment) {
        db.collection("reviews").document(review.getId())
                .update("comment", newComment, "timestamp", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Comment updated", Toast.LENGTH_SHORT).show();
                    loadReviews();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update comment", Toast.LENGTH_SHORT).show());
    }

    private void showDeleteConfirmationDialog(Review review) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setPositiveButton("Delete", (dialog, which) -> deleteReview(review))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteReview(Review review) {
        db.collection("reviews").document(review.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Comment deleted", Toast.LENGTH_SHORT).show();
                    loadReviews();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete comment", Toast.LENGTH_SHORT).show());
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

    private void loadReviews() {
        db.collection("reviews")
                .whereEqualTo("garageId", garageId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reviewList.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            Review review = document.toObject(Review.class);
                            if (review != null && review.getComment() != null && !review.getComment().isEmpty()) {
                                review.setId(document.getId());
                                reviewList.add(review);
                            }
                        }
                        if (reviewList.isEmpty()) {
                            noReviewsText.setVisibility(View.VISIBLE);
                        } else {
                            reviewAdapter.notifyDataSetChanged();
                            noReviewsText.setVisibility(View.GONE);
                        }
                    } else {
                        noReviewsText.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    // Fallback without ordering if index is missing
                    loadReviewsUnordered();
                });
    }

    private void loadReviewsUnordered() {
        db.collection("reviews")
                .whereEqualTo("garageId", garageId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reviewList.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            Review review = document.toObject(Review.class);
                            if (review != null && review.getComment() != null && !review.getComment().isEmpty()) {
                                review.setId(document.getId());
                                reviewList.add(review);
                            }
                        }

                        // Sort by timestamp descending (newest first)
                        reviewList.sort((r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));

                        if (reviewList.isEmpty()) {
                            noReviewsText.setVisibility(View.VISIBLE);
                        } else {
                            reviewAdapter.notifyDataSetChanged();
                            noReviewsText.setVisibility(View.GONE);
                        }
                    } else {
                        noReviewsText.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Click on rating to show rating dialog
        ratingText.setOnClickListener(v -> {
            if (auth.getCurrentUser() == null) {
                Toast.makeText(this, "Please login to rate this garage", Toast.LENGTH_SHORT).show();
                return;
            }
            showRatingDialog();
        });

        btnCall.setOnClickListener(v -> {
            if (garage != null && garage.getPhone() != null) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + garage.getPhone()));
                startActivity(intent);
            }
        });

        btnMessage.setOnClickListener(v -> {
            if (auth.getCurrentUser() == null) {
                Toast.makeText(this, "Please login to send message", Toast.LENGTH_SHORT).show();
                return;
            }

            if (garage != null && garage.getMechanicId() != null) {
                String ownerId = garage.getMechanicId();
                String myId = auth.getCurrentUser().getUid();
                String currentGarageId = garage.getId();

                db.collection("chats")
                        .whereArrayContains("participants", myId)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            String existingChatId = null;
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                Chat chat = doc.toObject(Chat.class);
                                // Check if chat is with this garage
                                if (chat != null && currentGarageId.equals(chat.getGarageId())) {
                                    existingChatId = chat.getChatId();
                                    break;
                                }
                            }

                            Intent intent = new Intent(GarageDetailActivity.this, ChatActivity.class);
                            if (existingChatId != null) {
                                intent.putExtra("chatId", existingChatId);
                            }
                            intent.putExtra("otherUserId", ownerId);
                            intent.putExtra("garageId", currentGarageId);
                            startActivity(intent);
                        });
            } else {
                Toast.makeText(this, "Cannot contact this garage", Toast.LENGTH_SHORT).show();
            }
        });

        btnSubmitReview.setOnClickListener(v -> submitReview());
    }

    private void showRatingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rate_garage, null);

        RatingBar dialogRatingBar = dialogView.findViewById(R.id.dialogRatingBar);
        Button btnSubmitRating = dialogView.findViewById(R.id.btnSubmitRating);

        AlertDialog dialog = builder.setView(dialogView).create();

        btnSubmitRating.setOnClickListener(v -> {
            float rating = dialogRatingBar.getRating();
            if (rating > 0) {
                // Save rating directly
                submitRatingOnly(rating);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void submitRatingOnly(float rating) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login to rate", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        db.collection("reviews")
                .whereEqualTo("garageId", garageId)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    DocumentSnapshot ratingDoc = null;

                    // Find existing document with rating > 0
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Double r = doc.getDouble("rating");
                            if (r != null && r > 0) {
                                ratingDoc = doc;
                                break;
                            }
                        }
                    }

                    if (ratingDoc != null) {
                        // Update existing rating
                        ratingDoc.getReference().update("rating", rating, "timestamp", System.currentTimeMillis())
                                .addOnSuccessListener(aVoid -> {
                                    updateGarageRating();
                                    Toast.makeText(this, "Rating updated successfully", Toast.LENGTH_SHORT).show();
                                    loadReviews();
                                })
                                .addOnFailureListener(e -> Toast
                                        .makeText(this, "Failed to update rating", Toast.LENGTH_SHORT).show());
                    } else {
                        // Create new review for rating
                        createNewReview(userId, rating, "");
                    }
                });
    }

    private void submitReview() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login to submit a comment", Toast.LENGTH_SHORT).show();
            return;
        }

        String comment = etReviewComment.getText().toString().trim();

        if (comment.isEmpty()) {
            Toast.makeText(this, "Please write a comment", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        // Always create a new review for comments (allowing multiple comments)
        createNewReview(userId, 0, comment);
    }

    private void createNewReview(String userId, float rating, String comment) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String userName = "Anonymous";
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && user.getName() != null) {
                            userName = user.getName();
                        }
                    }

                    String reviewId = db.collection("reviews").document().getId();
                    Review review = new Review(reviewId, garageId, userId, userName, rating, comment,
                            System.currentTimeMillis());

                    db.collection("reviews").document(reviewId)
                            .set(review)
                            .addOnSuccessListener(aVoid -> {
                                if (rating > 0)
                                    updateGarageRating();
                                if (!comment.isEmpty()) {
                                    Toast.makeText(this, "Comment submitted successfully", Toast.LENGTH_SHORT).show();
                                    etReviewComment.setText("");
                                } else {
                                    Toast.makeText(this, "Rating submitted successfully", Toast.LENGTH_SHORT).show();
                                }
                                loadReviews();
                            })
                            .addOnFailureListener(
                                    e -> Toast.makeText(this, "Failed to submit", Toast.LENGTH_SHORT).show());
                });
    }

    private void updateGarageRating() {
        db.collection("reviews")
                .whereEqualTo("garageId", garageId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        float totalRating = 0;
                        int count = 0;

                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            Review review = document.toObject(Review.class);
                            if (review != null && review.getRating() > 0) {
                                totalRating += review.getRating();
                                count++;
                            }
                        }

                        if (count > 0) {
                            float averageRating = totalRating / count;
                            db.collection("garages").document(garageId)
                                    .update("rating", averageRating, "reviewCount", count)
                                    .addOnSuccessListener(aVoid -> loadGarageDetails());
                        }
                    }
                });
    }
}
