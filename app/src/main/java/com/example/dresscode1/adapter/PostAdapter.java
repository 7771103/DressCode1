package com.example.dresscode1.adapter;

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
import com.example.dresscode1.PostDetailActivity;
import com.example.dresscode1.R;
import com.example.dresscode1.network.ApiClient;
import com.example.dresscode1.network.dto.Post;
import com.example.dresscode1.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> posts = new ArrayList<>();
    private OnPostActionListener listener;
    private int currentUserId;

    public interface OnPostActionListener {
        void onLikeClick(Post post, int position);
        void onCommentClick(Post post, int position);
        void onCollectClick(Post post, int position);
        void onPostClick(Post post);
        void onUserClick(Post post);
    }

    public PostAdapter(OnPostActionListener listener, int currentUserId) {
        this.listener = listener;
        this.currentUserId = currentUserId;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts != null ? posts : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void appendPosts(List<Post> newPosts) {
        if (newPosts != null && !newPosts.isEmpty()) {
            int startPosition = posts.size();
            this.posts.addAll(newPosts);
            notifyItemRangeInserted(startPosition, newPosts.size());
        }
    }

    public void updatePost(int position, Post post) {
        if (position >= 0 && position < posts.size()) {
            posts.set(position, post);
            notifyItemChanged(position);
        }
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
        Post post = posts.get(position);
        holder.bind(post);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        private TextView tvUserNickname;
        private ImageView ivUserAvatar;
        private TextView tvCreatedAt;
        private TextView tvContent;
        private ImageView ivPostImage;
        private TextView tvLikeCount;
        private TextView ivLike;
        private TextView tvCommentCount;
        private TextView tvCollectCount;
        private TextView ivCollect;
        private LinearLayout btnLike;
        private LinearLayout btnComment;
        private LinearLayout btnCollect;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserNickname = itemView.findViewById(R.id.tvUserNickname);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
            tvContent = itemView.findViewById(R.id.tvContent);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            ivLike = itemView.findViewById(R.id.ivLike);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            tvCollectCount = itemView.findViewById(R.id.tvCollectCount);
            ivCollect = itemView.findViewById(R.id.ivCollect);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
            btnCollect = itemView.findViewById(R.id.btnCollect);
        }

        public void bind(Post post) {
            tvUserNickname.setText(post.getUserNickname() != null ? post.getUserNickname() : "未知用户");
            
            // 加载用户头像
            String avatarUrl = ApiClient.getAvatarUrl(post.getUserAvatar());
            String nickname = post.getUserNickname() != null && !post.getUserNickname().isEmpty() 
                ? post.getUserNickname().substring(0, 1) : "?";
            
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(avatarUrl)
                        .circleCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .into(ivUserAvatar);
            } else {
                // 如果没有头像，显示昵称首字符（使用圆形背景）
                ivUserAvatar.setImageDrawable(null);
                // 这里可以设置一个默认的占位图或者保持背景色
            }

            // 设置时间
            if (post.getCreatedAt() != null && !post.getCreatedAt().isEmpty()) {
                tvCreatedAt.setText(TimeUtils.formatRelativeTime(post.getCreatedAt()));
            } else {
                tvCreatedAt.setText("");
            }

            tvContent.setText(post.getContent() != null ? post.getContent() : "");
            tvLikeCount.setText(String.valueOf(post.getLikeCount()));
            tvCommentCount.setText(String.valueOf(post.getCommentCount()));
            tvCollectCount.setText(String.valueOf(post.getCollectCount()));

            // 设置点赞状态
            if (post.isLiked()) {
                ivLike.setText("❤️");
                ivLike.setTextColor(itemView.getContext().getColor(R.color.error_red));
            } else {
                ivLike.setText("🤍");
                ivLike.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
            }

            // 设置收藏状态
            if (post.isCollected()) {
                ivCollect.setText("⭐");
                ivCollect.setTextColor(itemView.getContext().getColor(R.color.warning_yellow));
            } else {
                ivCollect.setText("☆");
                ivCollect.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
            }

            // 加载图片 - 从dataset中的图片
            String imageUrl = ApiClient.getImageUrl(post.getImagePath());
            if (imageUrl != null) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(android.R.color.transparent)
                        .error(android.R.color.transparent)
                        .into(ivPostImage);
            } else {
                ivPostImage.setImageDrawable(null);
            }

            // 设置点击事件
            btnLike.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLikeClick(post, getAdapterPosition());
                }
            });

            btnComment.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCommentClick(post, getAdapterPosition());
                }
            });

            btnCollect.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCollectClick(post, getAdapterPosition());
                }
            });

            // 点击用户头像或昵称跳转到用户主页
            View.OnClickListener userClickListener = v -> {
                if (listener != null) {
                    listener.onUserClick(post);
                }
            };
            ivUserAvatar.setOnClickListener(userClickListener);
            tvUserNickname.setOnClickListener(userClickListener);

            // 点击整个item跳转到详情页
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPostClick(post);
                }
            });
        }
    }
}

