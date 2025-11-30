package com.example.mini_projet.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mini_projet.R;
import com.example.mini_projet.models.Review;

import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private Context context;
    private List<Review> reviewList;
    private String currentUserId;
    private OnReviewActionListener listener;

    public interface OnReviewActionListener {
        void onEdit(Review review);
        void onDelete(Review review);
    }

    public ReviewAdapter(Context context, List<Review> reviewList, String currentUserId, OnReviewActionListener listener) {
        this.context = context;
        this.reviewList = reviewList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviewList.get(position);

        holder.userName.setText(review.getUserName());
        holder.reviewComment.setText(review.getComment());
        holder.reviewTime.setText(getTimeAgo(review.getTimestamp()));

        // Show edit/delete buttons only if the current user is the author
        if (currentUserId != null && currentUserId.equals(review.getUserId())) {
            holder.btnEditReview.setVisibility(View.VISIBLE);
            holder.btnDeleteReview.setVisibility(View.VISIBLE);

            holder.btnEditReview.setOnClickListener(v -> {
                if (listener != null) listener.onEdit(review);
            });

            holder.btnDeleteReview.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(review);
            });
        } else {
            holder.btnEditReview.setVisibility(View.GONE);
            holder.btnDeleteReview.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    private String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else {
            return "Just now";
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView userName, reviewComment, reviewTime;
        ImageView btnEditReview, btnDeleteReview;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            reviewComment = itemView.findViewById(R.id.reviewComment);
            reviewTime = itemView.findViewById(R.id.reviewTime);
            btnEditReview = itemView.findViewById(R.id.btnEditReview);
            btnDeleteReview = itemView.findViewById(R.id.btnDeleteReview);
        }
    }
}
