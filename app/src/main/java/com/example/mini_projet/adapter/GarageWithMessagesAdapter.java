package com.example.mini_projet.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mini_projet.MechanicGarageChatListActivity;
import com.example.mini_projet.R;
import com.example.mini_projet.models.Garage;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class GarageWithMessagesAdapter extends RecyclerView.Adapter<GarageWithMessagesAdapter.ViewHolder> {

    private Context context;
    private List<Garage> garageList;
    private Map<String, Integer> messageCountMap;

    public GarageWithMessagesAdapter(Context context, List<Garage> garageList, Map<String, Integer> messageCountMap) {
        this.context = context;
        this.garageList = garageList;
        this.messageCountMap = messageCountMap;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_garage_with_messages, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Garage garage = garageList.get(position);

        holder.garageName.setText(garage.getName());

        int messageCount = messageCountMap.getOrDefault(garage.getId(), 0);
        holder.messageCount.setText(messageCount + " conversation" + (messageCount > 1 ? "s" : ""));
        holder.messageBadge.setText(String.valueOf(messageCount));

        // Load garage photo
        if (garage.getPhotoUrl() != null && !garage.getPhotoUrl().isEmpty()) {
            Picasso.get()
                    .load(garage.getPhotoUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(holder.garageIcon);
        } else {
            holder.garageIcon.setImageResource(R.drawable.ic_profile_placeholder);
        }

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MechanicGarageChatListActivity.class);
            intent.putExtra("garageId", garage.getId());
            intent.putExtra("garageName", garage.getName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return garageList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView garageIcon;
        TextView garageName, messageCount, messageBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            garageIcon = itemView.findViewById(R.id.garageIcon);
            garageName = itemView.findViewById(R.id.garageName);
            messageCount = itemView.findViewById(R.id.messageCount);
            messageBadge = itemView.findViewById(R.id.messageBadge);
        }
    }
}
