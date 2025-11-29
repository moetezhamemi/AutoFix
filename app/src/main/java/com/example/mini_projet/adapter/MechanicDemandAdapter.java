package com.example.mini_projet.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mini_projet.R;
import com.example.mini_projet.models.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MechanicDemandAdapter extends RecyclerView.Adapter<MechanicDemandAdapter.ViewHolder> {

    private Context context;
    private List<User> mechanicList;
    private FirebaseFirestore db;

    public MechanicDemandAdapter(Context context, List<User> mechanicList) {
        this.context = context;
        this.mechanicList = mechanicList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_mechanic_demand, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User mechanic = mechanicList.get(position);

        holder.mechanicName.setText(mechanic.getName());
        
        // Format location if available
        if (mechanic.getAddress() != null) {
            holder.mechanicLocation.setText(String.format("%.4f, %.4f", 
                mechanic.getAddress().getLatitude(), mechanic.getAddress().getLongitude()));
        } else {
            holder.mechanicLocation.setText("Location not available");
        }

        // Load photo
        if (mechanic.getPhotoUrl() != null && !mechanic.getPhotoUrl().isEmpty()) {
            Picasso.get().load(mechanic.getPhotoUrl())
                .placeholder(R.drawable.ic_profile_placeholder)
                .into(holder.mechanicPhoto);
        } else {
            holder.mechanicPhoto.setImageResource(R.drawable.ic_profile_placeholder);
        }

        // Accept Button
        holder.btnAccept.setOnClickListener(v -> {
            acceptMechanic(mechanic, position);
        });

        // Reject Button
        holder.btnReject.setOnClickListener(v -> {
            rejectMechanic(mechanic, position);
        });
    }

    private void acceptMechanic(User mechanic, int position) {
        db.collection("users").document(mechanic.getId())
                .update("enabled", true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Mechanic accepted", Toast.LENGTH_SHORT).show();
                    mechanicList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, mechanicList.size());
                    
                    // Send Email
                    sendEmail(mechanic.getEmail(), mechanic.getName());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error accepting mechanic", Toast.LENGTH_SHORT).show();
                });
    }

    private void rejectMechanic(User mechanic, int position) {
        // For now, we delete the user document. 
        // Alternatively, we could set a status field to "rejected".
        db.collection("users").document(mechanic.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Mechanic rejected", Toast.LENGTH_SHORT).show();
                    mechanicList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, mechanicList.size());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error rejecting mechanic", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendEmail(String email, String name) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        intent.putExtra(Intent.EXTRA_SUBJECT, "AutoFix Account Activation");
        intent.putExtra(Intent.EXTRA_TEXT, "Hello " + name + ",\n\nYour account has been activated by the admin. You can now login to the AutoFix application.\n\nBest regards,\nAutoFix Team");
        
        try {
            context.startActivity(Intent.createChooser(intent, "Send email..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(context, "No email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return mechanicList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView mechanicPhoto;
        TextView mechanicName, mechanicLocation;
        ImageView btnAccept, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mechanicPhoto = itemView.findViewById(R.id.mechanicPhoto);
            mechanicName = itemView.findViewById(R.id.mechanicName);
            mechanicLocation = itemView.findViewById(R.id.mechanicLocation);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
