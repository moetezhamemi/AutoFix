package com.example.mini_projet.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mini_projet.ChatActivity;
import com.example.mini_projet.R;
import com.example.mini_projet.models.Chat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;
import com.example.mini_projet.models.User;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    private Context context;
    private List<Chat> mChats;
    private FirebaseUser fuser;
    private boolean isMechanicView; // true if viewing as mechanic, false if viewing as client

    public ChatListAdapter(Context context, List<Chat> mChats) {
        this(context, mChats, false); // Default to client view
    }

    public ChatListAdapter(Context context, List<Chat> mChats, boolean isMechanicView) {
        this.context = context;
        this.mChats = mChats;
        this.isMechanicView = isMechanicView;
        fuser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Chat chat = mChats.get(position);

        // Determine other user's ID
        String otherUserId = null;
        for (String id : chat.getParticipants()) {
            if (!id.equals(fuser.getUid())) {
                otherUserId = id;
                break;
            }
        }

        // Set info based on garageId and view type
        if (chat.getGarageId() != null && !isMechanicView) {
            // Client view: show garage info
            FirebaseFirestore.getInstance().collection("garages").document(chat.getGarageId()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String photoUrl = documentSnapshot.getString("photoUrl");

                            holder.username.setText(name != null ? name : "Garage");

                            // Load garage photo
                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                try {
                                    Picasso.get()
                                            .load(photoUrl)
                                            .placeholder(R.drawable.ic_profile_placeholder)
                                            .error(R.drawable.ic_profile_placeholder)
                                            .into(holder.chat_icon);
                                } catch (Exception e) {
                                    holder.chat_icon.setImageResource(R.drawable.ic_profile_placeholder);
                                }
                            } else {
                                holder.chat_icon.setImageResource(R.drawable.ic_profile_placeholder);
                            }
                        } else {
                            holder.username.setText("Garage");
                            holder.chat_icon.setImageResource(R.drawable.ic_profile_placeholder);
                        }
                    })
                    .addOnFailureListener(e -> {
                        holder.username.setText("Garage");
                        holder.chat_icon.setImageResource(R.drawable.ic_profile_placeholder);
                    });
        } else {
            // Mechanic view OR no garageId: show user/client info
            if (otherUserId != null) {
                FirebaseFirestore.getInstance().collection("users").document(otherUserId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                if (user.getName() != null) {
                                    holder.username.setText(user.getName());
                                }

                                if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                                    try {
                                        Picasso.get()
                                                .load(user.getPhotoUrl())
                                                .placeholder(R.drawable.ic_profile_placeholder)
                                                .error(R.drawable.ic_profile_placeholder)
                                                .into(holder.chat_icon);
                                    } catch (Exception e) {
                                        holder.chat_icon.setImageResource(R.drawable.ic_profile_placeholder);
                                    }
                                } else {
                                    holder.chat_icon.setImageResource(R.drawable.ic_profile_placeholder);
                                }
                            }
                        });
            } else {
                holder.username.setText("User");
                holder.chat_icon.setImageResource(R.drawable.ic_profile_placeholder);
            }
        }

        // Set last message
        if (chat.getLastMessage() != null) {
            holder.last_message.setText(chat.getLastMessage());
        } else {
            holder.last_message.setText("");
        }

        // Click listener
        String finalOtherUserId = otherUserId;
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("chatId", chat.getChatId());
            intent.putExtra("otherUserId", finalOtherUserId);
            intent.putExtra("garageId", chat.getGarageId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return mChats.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView username;
        public TextView last_message;
        public CircleImageView chat_icon;

        public ViewHolder(View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.username);
            last_message = itemView.findViewById(R.id.last_message);
            chat_icon = itemView.findViewById(R.id.chat_icon);
        }
    }
}
