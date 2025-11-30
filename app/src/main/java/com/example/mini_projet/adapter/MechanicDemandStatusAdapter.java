package com.example.mini_projet.adapter;

import android.content.Context;
import android.graphics.Color;
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

public class MechanicDemandStatusAdapter extends RecyclerView.Adapter<MechanicDemandStatusAdapter.ViewHolder> {

    private Context context;
    private List<Garage> garageList;

    public MechanicDemandStatusAdapter(Context context, List<Garage> garageList) {
        this.context = context;
        this.garageList = garageList;
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
        } else {
            holder.statusChip.setText("Pending");
            holder.statusChip.setBackgroundColor(Color.parseColor("#FFA000")); // Orange
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

    @Override
    public int getItemCount() {
        return garageList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView garagePhoto;
        TextView garageName, garagePhone, statusChip;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            garagePhoto = itemView.findViewById(R.id.garagePhoto);
            garageName = itemView.findViewById(R.id.garageName);
            garagePhone = itemView.findViewById(R.id.garagePhone);
            statusChip = itemView.findViewById(R.id.statusChip);
        }
    }
}
