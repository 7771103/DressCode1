package com.example.dresscode1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.dresscode1.database.entity.CommentEntity;
import com.example.dresscode1.network.ApiClient;
import com.example.dresscode1.network.dto.Comment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
    
    private List<CommentEntity> commentList = new ArrayList<>();
    
    public void setComments(List<CommentEntity> comments) {
        this.commentList = comments != null ? comments : new ArrayList<>();
        notifyDataSetChanged();
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
        CommentEntity comment = commentList.get(position);
        holder.bind(comment);
    }
    
    @Override
    public int getItemCount() {
        return commentList.size();
    }
    
    static class CommentViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivAvatar;
        private TextView tvNickname;
        private TextView tvContent;
        private TextView tvTime;
        
        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvNickname = itemView.findViewById(R.id.tvNickname);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
        
        public void bind(CommentEntity comment) {
            tvNickname.setText(comment.userNickname != null ? comment.userNickname : "用户");
            tvContent.setText(comment.content != null ? comment.content : "");
            
            // 加载头像
            if (comment.userAvatar != null && !comment.userAvatar.isEmpty()) {
                String fullUrl = comment.userAvatar.startsWith("http") 
                    ? comment.userAvatar 
                    : ApiClient.getBaseUrl() + comment.userAvatar;
                Glide.with(itemView.getContext())
                    .load(fullUrl)
                    .circleCrop()
                    .into(ivAvatar);
            }
            
            // 格式化时间
            if (comment.createdAt != null) {
                tvTime.setText(formatTime(comment.createdAt));
            } else {
                tvTime.setText("");
            }
        }
        
        private String formatTime(String timeStr) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date date = sdf.parse(timeStr);
                if (date == null) return "";
                
                long diff = System.currentTimeMillis() - date.getTime();
                long minutes = diff / (1000 * 60);
                long hours = diff / (1000 * 60 * 60);
                long days = diff / (1000 * 60 * 60 * 24);
                
                if (minutes < 1) {
                    return "刚刚";
                } else if (minutes < 60) {
                    return minutes + "分钟前";
                } else if (hours < 24) {
                    return hours + "小时前";
                } else if (days < 7) {
                    return days + "天前";
                } else {
                    SimpleDateFormat format = new SimpleDateFormat("MM-dd", Locale.getDefault());
                    return format.format(date);
                }
            } catch (Exception e) {
                return "";
            }
        }
    }
}


