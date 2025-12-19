package com.example.dresscode1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dresscode1.network.dto.ChatMessage;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    
    private List<ChatMessage> messageList;
    
    public ChatAdapter(List<ChatMessage> messageList) {
        this.messageList = messageList;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = viewType == 0 ? R.layout.item_chat_user : R.layout.item_chat_assistant;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        holder.tvMessage.setText(message.getContent());
    }
    
    @Override
    public int getItemCount() {
        return messageList.size();
    }
    
    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).getRole().equals("user") ? 0 : 1;
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }
    }
}

