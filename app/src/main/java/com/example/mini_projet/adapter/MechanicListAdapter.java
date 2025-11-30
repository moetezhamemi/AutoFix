package com.example.mini_projet.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mini_projet.MechanicGaragesListActivity;
import com.example.mini_projet.R;
import com.example.mini_projet.models.User;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MechanicListAdapter extends RecyclerView.Adapter<MechanicListAdapter.ViewHolder> {

    private Context context;
    private List<User> mechanicList;

    public MechanicListAdapter(Context context, List<User> mechanicList) {
        this.context = context;
        this.mechanicList = mechanicList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_mechanic, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User mechanic = mechanicList.get(position);

        holder.mechanicName.setText(mechanic.getName());
        holder.mechanicEmail.setText(mechanic.getEmail());

        // Load photo
        if (mechanic.getPhotoUrl() != null && !mechanic.getPhotoUrl().isEmpty()) {
            Picasso.get().load(mechanic.getPhotoUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(holder.mechanicPhoto);
        } else {
            holder.mechanicPhoto.setImageResource(R.drawable.ic_profile_placeholder);
        }

        // Click to view garages
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MechanicGaragesListActivity.class);
            intent.putExtra("mechanicId", mechanic.getId());
            intent.putExtra("mechanicName", mechanic.getName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return mechanicList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView mechanicPhoto;
        TextView mechanicName, mechanicEmail;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mechanicPhoto = itemView.findViewById(R.id.mechanicPhoto);
            mechanicName = itemView.findViewById(R.id.mechanicName);
            mechanicEmail = itemView.findViewById(R.id.mechanicEmail);
        }
    }
}
