package com.example.mini_projet.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mini_projet.R;
import com.example.mini_projet.models.Garage;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdminGarageAdapter extends RecyclerView.Adapter<AdminGarageAdapter.ViewHolder> {

    private Context context;
    private List<Garage> garageList;

    public AdminGarageAdapter(Context context, List<Garage> garageList) {
        this.context = context;
        this.garageList = garageList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_garage, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Garage garage = garageList.get(position);

        holder.garageName.setText(garage.getName());
        holder.garagePhone.setText(garage.getPhone());
        holder.garageDescription.setText(garage.getDescription());

        // Load photo
        if (garage.getPhotoUrl() != null && !garage.getPhotoUrl().isEmpty()) {
            Picasso.get().load(garage.getPhotoUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(holder.garagePhoto);
        } else {
            holder.garagePhoto.setImageResource(R.drawable.ic_profile_placeholder);
        }

        // Delete Button Click
        holder.btnDelete.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle("Delete Garage")
                    .setMessage("Are you sure you want to delete this garage?")
                    .setPositiveButton("Delete", (dialog, which) -> deleteGarage(garage, position))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void deleteGarage(Garage garage, int position) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("garages").document(garage.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    android.widget.Toast.makeText(context, "Garage deleted", android.widget.Toast.LENGTH_SHORT).show();
                    garageList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, garageList.size());
                })
                .addOnFailureListener(e -> {
                    android.widget.Toast.makeText(context, "Error deleting garage", android.widget.Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return garageList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView garagePhoto;
        TextView garageName, garagePhone, garageDescription;
        android.widget.ImageView btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            garagePhoto = itemView.findViewById(R.id.garagePhoto);
            garageName = itemView.findViewById(R.id.garageName);
            garagePhone = itemView.findViewById(R.id.garagePhone);
            garageDescription = itemView.findViewById(R.id.garageDescription);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
