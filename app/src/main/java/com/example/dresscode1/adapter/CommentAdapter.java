package com.example.dresscode1.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dresscode1.R;
import com.example.dresscode1.network.dto.Comment;

import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> comments = new ArrayList<>();

    public void setComments(List<Comment> comments) {
        this.comments = comments != null ? comments : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addComment(Comment comment) {
        if (comment != null) {
            comments.add(comment);
            notifyItemInserted(comments.size() - 1);
        }
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.bind(comment);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {
        private TextView tvUserAvatar;
        private TextView tvUserNickname;
        private TextView tvContent;
        private TextView tvCreatedAt;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserAvatar = itemView.findViewById(R.id.tvUserAvatar);
            tvUserNickname = itemView.findViewById(R.id.tvUserNickname);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
        }

        public void bind(Comment comment) {
            tvUserNickname.setText(comment.getUserNickname() != null ? comment.getUserNickname() : "未知用户");
            tvContent.setText(comment.getContent() != null ? comment.getContent() : "");
            
            // 设置头像（简化处理，显示昵称首字符）
            if (comment.getUserNickname() != null && !comment.getUserNickname().isEmpty()) {
                String firstChar = comment.getUserNickname().substring(0, 1);
                tvUserAvatar.setText(firstChar);
            } else {
                tvUserAvatar.setText("?");
            }

            // 设置时间
            if (comment.getCreatedAt() != null && !comment.getCreatedAt().isEmpty()) {
                tvCreatedAt.setText(" · " + formatTime(comment.getCreatedAt()));
            } else {
                tvCreatedAt.setText("");
            }
        }

        private String formatTime(String timeStr) {
            // 简化处理，直接显示时间字符串
            // 可以后续优化为相对时间（如"2小时前"）
            try {
                if (timeStr.length() > 16) {
                    return timeStr.substring(5, 16);
                }
                return timeStr;
            } catch (Exception e) {
                return timeStr;
            }
        }
    }
}


