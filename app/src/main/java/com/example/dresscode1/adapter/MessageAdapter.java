package com.example.dresscode1.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dresscode1.R;
import com.example.dresscode1.network.dto.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 1;
    private static final int TYPE_ASSISTANT = 2;

    private List<ChatMessage> messages = new ArrayList<>();

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addMessage(ChatMessage message) {
        if (message != null) {
            messages.add(message);
            notifyItemInserted(messages.size() - 1);
        }
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        if ("user".equals(message.getRole())) {
            return TYPE_USER;
        } else {
            return TYPE_ASSISTANT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_user, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_assistant, parent, false);
            return new AssistantMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).bind(message);
        } else if (holder instanceof AssistantMessageViewHolder) {
            ((AssistantMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class UserMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMessageContent;

        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageContent = itemView.findViewById(R.id.tvMessageContent);
        }

        public void bind(ChatMessage message) {
            tvMessageContent.setText(message.getContent() != null ? message.getContent() : "");
        }
    }

    class AssistantMessageViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivAvatar;
        private TextView tvMessageContent;

        public AssistantMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvMessageContent = itemView.findViewById(R.id.tvMessageContent);
        }

        public void bind(ChatMessage message) {
            tvMessageContent.setText(message.getContent() != null ? message.getContent() : "");
        }
    }
}

