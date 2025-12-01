package com.example.mini_projet.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mini_projet.R;
import com.example.mini_projet.HomeActivity;
import com.example.mini_projet.models.Message;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    public static final int MSG_TYPE_LEFT = 0;
    public static final int MSG_TYPE_RIGHT = 1;
    public static final int MSG_TYPE_LOCATION_LEFT = 2;
    public static final int MSG_TYPE_LOCATION_RIGHT = 3;

    private Context mContext;
    private List<Message> mChat;
    private String currentUserId;

    public MessageAdapter(Context mContext, List<Message> mChat, String currentUserId) {
        this.mChat = mChat;
        this.mContext = mContext;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == MSG_TYPE_RIGHT) {
            view = LayoutInflater.from(mContext).inflate(R.layout.item_message_sent, parent, false);
        } else if (viewType == MSG_TYPE_LEFT) {
            view = LayoutInflater.from(mContext).inflate(R.layout.item_message_received, parent, false);
        } else if (viewType == MSG_TYPE_LOCATION_RIGHT) {
            view = LayoutInflater.from(mContext).inflate(R.layout.item_message_location_sent, parent, false);
        } else {
            view = LayoutInflater.from(mContext).inflate(R.layout.item_message_location_received, parent, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.ViewHolder holder, int position) {
        Message chat = mChat.get(position);

        if ("location".equals(chat.getType())) {
            holder.show_message.setText("Location shared");
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(mContext, HomeActivity.class);
                intent.putExtra("sharedLatitude", chat.getLatitude());
                intent.putExtra("sharedLongitude", chat.getLongitude());
                mContext.startActivity(intent);
            });
        } else {
            holder.show_message.setText(chat.getText());
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return mChat.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView show_message;

        public ViewHolder(View itemView) {
            super(itemView);
            show_message = itemView.findViewById(R.id.show_message);
        }
    }

    @Override
    public int getItemViewType(int position) {
        Message message = mChat.get(position);
        boolean isCurrentUser = message.getSenderId().equals(currentUserId);
        boolean isLocation = "location".equals(message.getType());

        if (isLocation) {
            return isCurrentUser ? MSG_TYPE_LOCATION_RIGHT : MSG_TYPE_LOCATION_LEFT;
        } else {
            return isCurrentUser ? MSG_TYPE_RIGHT : MSG_TYPE_LEFT;
        }
    }
}
