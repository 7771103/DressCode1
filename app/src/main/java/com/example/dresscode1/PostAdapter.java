package com.example.dresscode1;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.dresscode1.database.entity.PostEntity;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    
    private List<PostEntity> postList;
    private int currentUserId;
    private static final String BASE_URL = "http://10.134.17.29:5000";
    private OnPostClickListener onPostClickListener;
    private OnLikeClickListener onLikeClickListener;
    private OnFavoriteClickListener onFavoriteClickListener;
    
    public interface OnPostClickListener {
        void onPostClick(int postId);
    }
    
    public interface OnLikeClickListener {
        void onLikeClick(PostEntity post);
    }
    
    public interface OnFavoriteClickListener {
        void onFavoriteClick(PostEntity post);
    }
    
    public PostAdapter(List<PostEntity> postList, int currentUserId) {
        this.postList = postList;
        this.currentUserId = currentUserId;
    }
    
    public void updatePostList(List<PostEntity> newList) {
        this.postList = newList;
        notifyDataSetChanged();
    }
    
    public void setOnPostClickListener(OnPostClickListener listener) {
        this.onPostClickListener = listener;
    }
    
    public void setOnLikeClickListener(OnLikeClickListener listener) {
        this.onLikeClickListener = listener;
    }
    
    public void setOnFavoriteClickListener(OnFavoriteClickListener listener) {
        this.onFavoriteClickListener = listener;
    }
    
    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        PostEntity post = postList.get(position);
        holder.bind(post);
    }
    
    @Override
    public int getItemCount() {
        return postList.size();
    }
    
    class PostViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivAvatar, ivPostImage, ivLike, ivComment, ivFavorite;
        private TextView tvNickname, tvCity, tvTime, tvContent, tvLikeCount, tvCommentCount, tvFavoriteCount;
        private LinearLayout layoutTags;
        
        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            ivLike = itemView.findViewById(R.id.ivLike);
            ivComment = itemView.findViewById(R.id.ivComment);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
            tvNickname = itemView.findViewById(R.id.tvNickname);
            tvCity = itemView.findViewById(R.id.tvCity);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            tvFavoriteCount = itemView.findViewById(R.id.tvFavoriteCount);
            layoutTags = itemView.findViewById(R.id.layoutTags);
        }
        
        public void bind(PostEntity post) {
            tvNickname.setText(post.userNickname != null ? post.userNickname : "用户");
            tvCity.setText(post.city != null ? post.city : "");
            tvContent.setText(post.content != null ? post.content : "");
            tvLikeCount.setText(String.valueOf(post.likeCount));
            tvCommentCount.setText(String.valueOf(post.commentCount));
            tvFavoriteCount.setText(String.valueOf(post.favoriteCount));
            
            // 加载头像
            if (post.userAvatar != null && !post.userAvatar.isEmpty()) {
                Glide.with(itemView.getContext())
                    .load(BASE_URL + post.userAvatar)
                    .circleCrop()
                    .into(ivAvatar);
            }
            
            // 加载帖子图片
            if (post.imageUrl != null && !post.imageUrl.isEmpty()) {
                String imageUrl = post.imageUrl.startsWith("http") 
                    ? post.imageUrl 
                    : BASE_URL + post.imageUrl;
                Glide.with(itemView.getContext())
                    .load(imageUrl)
                    .into(ivPostImage);
            }
            
            // 设置点赞状态
            ivLike.setColorFilter(post.isLiked 
                ? itemView.getContext().getColor(R.color.primary_blue_gray)
                : itemView.getContext().getColor(R.color.text_secondary));
            
            // 设置收藏状态
            ivFavorite.setColorFilter(post.isFavorited
                ? itemView.getContext().getColor(R.color.primary_blue_gray)
                : itemView.getContext().getColor(R.color.text_secondary));
            
            // 点击帖子图片进入详情
            ivPostImage.setOnClickListener(v -> {
                if (onPostClickListener != null) {
                    onPostClickListener.onPostClick(post.id);
                } else {
                    // 降级方案：直接启动 Activity（如果没有设置监听器）
                    Intent intent = new Intent(itemView.getContext(), PostDetailActivity.class);
                    intent.putExtra("postId", post.id);
                    intent.putExtra("currentUserId", currentUserId);
                    itemView.getContext().startActivity(intent);
                }
            });
            
            // 点赞
            ivLike.setOnClickListener(v -> {
                if (currentUserId > 0 && onLikeClickListener != null) {
                    onLikeClickListener.onLikeClick(post);
                }
            });
            
            // 收藏
            ivFavorite.setOnClickListener(v -> {
                if (currentUserId > 0 && onFavoriteClickListener != null) {
                    onFavoriteClickListener.onFavoriteClick(post);
                }
            });
            
            // 评论
            ivComment.setOnClickListener(v -> {
                if (onPostClickListener != null) {
                    onPostClickListener.onPostClick(post.id);
                } else {
                    // 降级方案：直接启动 Activity（如果没有设置监听器）
                    Intent intent = new Intent(itemView.getContext(), PostDetailActivity.class);
                    intent.putExtra("postId", post.id);
                    intent.putExtra("currentUserId", currentUserId);
                    itemView.getContext().startActivity(intent);
                }
            });
        }
    }
}

