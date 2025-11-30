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
import com.example.mini_projet.models.Garage;
import com.example.mini_projet.models.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class GarageRequestAdapter extends RecyclerView.Adapter<GarageRequestAdapter.ViewHolder> {

    private Context context;
    private List<Garage> garageList;
    private FirebaseFirestore db;

    public GarageRequestAdapter(Context context, List<Garage> garageList) {
        this.context = context;
        this.garageList = garageList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_garage_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Garage garage = garageList.get(position);

        holder.garageName.setText(garage.getName());
        holder.garagePhone.setText(garage.getPhone());

        // Load photo
        if (garage.getPhotoUrl() != null && !garage.getPhotoUrl().isEmpty()) {
            Picasso.get().load(garage.getPhotoUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(holder.garagePhoto);
        } else {
            holder.garagePhoto.setImageResource(R.drawable.ic_profile_placeholder);
        }

        // Accept Button
        holder.btnAccept.setOnClickListener(v -> {
            acceptGarage(garage, position);
        });

        // Reject Button
        holder.btnReject.setOnClickListener(v -> {
            rejectGarage(garage, position);
        });
    }

    private void acceptGarage(Garage garage, int position) {
        db.collection("garages").document(garage.getId())
                .update("enabled", true, "status", "ACCEPTED")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Garage accepted", Toast.LENGTH_SHORT).show();
                    garageList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, garageList.size());

                    // Get mechanic email and send notification
                    getMechanicEmailAndNotify(garage.getMechanicId(), garage.getName());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error accepting garage", Toast.LENGTH_SHORT).show();
                });
    }

    private void rejectGarage(Garage garage, int position) {
        db.collection("garages").document(garage.getId())
                .update("status", "REJECTED")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Garage rejected", Toast.LENGTH_SHORT).show();
                    garageList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, garageList.size());

                    // Get mechanic email and send rejection notification
                    getMechanicEmailAndNotifyRejection(garage.getMechanicId(), garage.getName());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error rejecting garage", Toast.LENGTH_SHORT).show();
                });
    }

    private void getMechanicEmailAndNotify(String mechanicId, String garageName) {
        db.collection("users").document(mechanicId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User mechanic = documentSnapshot.toObject(User.class);
                        if (mechanic != null && mechanic.getEmail() != null) {
                            String subject = "AutoFix Garage Approval";
                            String body = "Hello " + mechanic.getName() + ",\n\nYour garage '" + garageName +
                                    "' has been approved by the admin. You can now start managing your garage services.\n\nBest regards,\nAutoFix Team";
                            sendEmail(mechanic.getEmail(), subject, body);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error fetching mechanic details", Toast.LENGTH_SHORT).show();
                });
    }

    private void getMechanicEmailAndNotifyRejection(String mechanicId, String garageName) {
        db.collection("users").document(mechanicId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User mechanic = documentSnapshot.toObject(User.class);
                        if (mechanic != null && mechanic.getEmail() != null) {
                            String subject = "AutoFix Garage Request Update";
                            String body = "Hello " + mechanic.getName()
                                    + ",\n\nWe regret to inform you that your request for garage '" + garageName +
                                    "' has been declined by the admin.\n\nBest regards,\nAutoFix Team";
                            sendEmail(mechanic.getEmail(), subject, body);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error fetching mechanic details", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendEmail(String email, String subject, String body) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] { email });
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);

        try {
            context.startActivity(Intent.createChooser(intent, "Send email..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(context, "No email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return garageList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView garagePhoto;
        TextView garageName, garagePhone;
        ImageView btnAccept, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            garagePhoto = itemView.findViewById(R.id.garagePhoto);
            garageName = itemView.findViewById(R.id.garageName);
            garagePhone = itemView.findViewById(R.id.garagePhone);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
