package com.example.mini_projet.adapter;

import android.content.Context;
import android.content.Intent;
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

public class MechanicGarageAdapter extends RecyclerView.Adapter<MechanicGarageAdapter.ViewHolder> {

    private Context context;
    private List<Garage> garageList;
    private FirebaseFirestore db;

    public MechanicGarageAdapter(Context context, List<Garage> garageList) {
        this.context = context;
        this.garageList = garageList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_mechanic_garage, parent, false);
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

        // Edit Button
        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, com.example.mini_projet.EditGarageActivity.class);
            intent.putExtra("garageId", garage.getId());
            context.startActivity(intent);
        });

        // Delete Button
        holder.btnDelete.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(context)
                    .setTitle("Delete Garage")
                    .setMessage("Are you sure you want to delete this garage?")
                    .setPositiveButton("Delete", (dialog, which) -> deleteGarage(garage, position))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void deleteGarage(Garage garage, int position) {
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
    }

    @Override
    public int getItemCount() {
        return garageList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView garagePhoto;
        TextView garageName, garagePhone, garageDescription;
        ImageView btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            garagePhoto = itemView.findViewById(R.id.garagePhoto);
            garageName = itemView.findViewById(R.id.garageName);
            garagePhone = itemView.findViewById(R.id.garagePhone);
            garageDescription = itemView.findViewById(R.id.garageDescription);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
