package com.example.dresscode1.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.dresscode1.R;
import com.example.dresscode1.network.ApiClient;
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
        private ImageView ivUserAvatar;
        private TextView tvUserNickname;
        private TextView tvContent;
        private TextView tvCreatedAt;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvUserNickname = itemView.findViewById(R.id.tvUserNickname);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
        }

        public void bind(Comment comment) {
            tvUserNickname.setText(comment.getUserNickname() != null ? comment.getUserNickname() : "未知用户");
            tvContent.setText(comment.getContent() != null ? comment.getContent() : "");
            
            // 加载用户头像
            String avatarUrl = ApiClient.getAvatarUrl(comment.getUserAvatar());
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(avatarUrl)
                        .circleCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .into(ivUserAvatar);
            } else {
                // 如果没有头像，清除图片显示
                ivUserAvatar.setImageDrawable(null);
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


