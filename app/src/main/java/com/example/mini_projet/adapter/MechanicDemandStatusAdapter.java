package com.example.mini_projet.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mini_projet.R;
import com.example.mini_projet.models.Garage;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MechanicDemandStatusAdapter extends RecyclerView.Adapter<MechanicDemandStatusAdapter.ViewHolder> {

    private Context context;
    private List<Garage> garageList;
    private FirebaseFirestore db;

    public MechanicDemandStatusAdapter(Context context, List<Garage> garageList) {
        this.context = context;
        this.garageList = garageList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_mechanic_demand_status, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Garage garage = garageList.get(position);

        holder.garageName.setText(garage.getName());
        holder.garagePhone.setText(garage.getPhone());

        // Status Logic
        if (garage.isEnabled()) {
            holder.statusChip.setText("Accepted");
            holder.statusChip.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
        } else if ("REJECTED".equals(garage.getStatus())) {
            holder.statusChip.setText("Rejected");
            holder.statusChip.setBackgroundColor(Color.parseColor("#F44336")); // Red
        } else {
            holder.statusChip.setText("Pending");
            holder.statusChip.setBackgroundColor(Color.parseColor("#FFA000")); // Orange
        }

        // Show delete button only for ACCEPTED or REJECTED status
        if (garage.isEnabled() || "REJECTED".equals(garage.getStatus())) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> showDeleteConfirmation(garage, position));
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }

        // Load photo
        if (garage.getPhotoUrl() != null && !garage.getPhotoUrl().isEmpty()) {
            Picasso.get().load(garage.getPhotoUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(holder.garagePhoto);
        } else {
            holder.garagePhoto.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    private void showDeleteConfirmation(Garage garage, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Garage")
                .setMessage("Are you sure you want to delete this garage request from the list?")
                .setPositiveButton("Delete", (dialog, which) -> deleteGarage(garage, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteGarage(Garage garage, int position) {
        // If REJECTED, delete from Firestore completely
        if ("REJECTED".equals(garage.getStatus())) {
            db.collection("garages").document(garage.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Garage deleted", Toast.LENGTH_SHORT).show();
                        garageList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, garageList.size());
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Error deleting garage", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // If ACCEPTED, just remove from list (keep in Firestore)
            Toast.makeText(context, "Removed from list", Toast.LENGTH_SHORT).show();
            garageList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, garageList.size());
        }
    }

    @Override
    public int getItemCount() {
        return garageList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView garagePhoto;
        TextView garageName, garagePhone, statusChip;
        ImageView btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            garagePhoto = itemView.findViewById(R.id.garagePhoto);
            garageName = itemView.findViewById(R.id.garageName);
            garagePhone = itemView.findViewById(R.id.garagePhone);
            statusChip = itemView.findViewById(R.id.statusChip);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
