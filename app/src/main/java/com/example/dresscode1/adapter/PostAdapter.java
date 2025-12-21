package com.example.dresscode1.adapter;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
    private boolean useStaggeredLayout; // 是否使用瀑布流布局

    public interface OnPostActionListener {
        void onLikeClick(Post post, int position);
        void onCommentClick(Post post, int position);
        void onCollectClick(Post post, int position);
        void onPostClick(Post post);
        void onUserClick(Post post);
        void onDeleteClick(Post post, int position);
    }

    public PostAdapter(OnPostActionListener listener, int currentUserId) {
        this.listener = listener;
        this.currentUserId = currentUserId;
        this.useStaggeredLayout = false;
    }

    public PostAdapter(OnPostActionListener listener, int currentUserId, boolean useStaggeredLayout) {
        this.listener = listener;
        this.currentUserId = currentUserId;
        this.useStaggeredLayout = useStaggeredLayout;
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

    public void removePost(int position) {
        if (position >= 0 && position < posts.size()) {
            posts.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, posts.size() - position);
        }
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = useStaggeredLayout ? R.layout.item_post_staggered : R.layout.item_post;
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutRes, parent, false);
        return new PostViewHolder(view, useStaggeredLayout);
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
        private MaterialButton btnDelete;
        private LinearLayout llTags;
        private boolean isStaggeredLayout;

        public PostViewHolder(@NonNull View itemView, boolean isStaggeredLayout) {
            super(itemView);
            this.isStaggeredLayout = isStaggeredLayout;
            tvUserNickname = itemView.findViewById(R.id.tvUserNickname);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
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
            btnDelete = itemView.findViewById(R.id.btnDelete);
            
            // 只有非瀑布流布局才有这些视图
            if (!isStaggeredLayout) {
                tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
                llTags = itemView.findViewById(R.id.llTags);
            }
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

            // 设置时间（仅非瀑布流布局）
            if (!isStaggeredLayout && tvCreatedAt != null) {
                if (post.getCreatedAt() != null && !post.getCreatedAt().isEmpty()) {
                    tvCreatedAt.setText(TimeUtils.formatRelativeTime(post.getCreatedAt()));
                } else {
                    tvCreatedAt.setText("");
                }
            }

            // 设置内容（瀑布流布局限制3行）
            String content = post.getContent() != null ? post.getContent() : "";
            tvContent.setText(content);
            if (isStaggeredLayout) {
                tvContent.setMaxLines(3);
                tvContent.setEllipsize(android.text.TextUtils.TruncateAt.END);
            }
            
            tvLikeCount.setText(String.valueOf(post.getLikeCount()));
            tvCommentCount.setText(String.valueOf(post.getCommentCount()));
            tvCollectCount.setText(String.valueOf(post.getCollectCount()));

            // 显示标签（仅非瀑布流布局）
            if (!isStaggeredLayout && llTags != null) {
                displayTags(post.getTags());
            }

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
            
            // 删除按钮（仅当是当前用户的帖子时显示）
            if (btnDelete != null) {
                if (currentUserId > 0 && post.getUserId() == currentUserId) {
                    btnDelete.setVisibility(View.VISIBLE);
                    btnDelete.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onDeleteClick(post, getAdapterPosition());
                        }
                    });
                } else {
                    btnDelete.setVisibility(View.GONE);
                }
            }
        }

        private void displayTags(List<String> tags) {
            llTags.removeAllViews();
            
            if (tags == null || tags.isEmpty()) {
                llTags.setVisibility(View.GONE);
                return;
            }

            llTags.setVisibility(View.VISIBLE);
            int marginEnd = (int) (8 * itemView.getContext().getResources().getDisplayMetrics().density);
            int marginBottom = (int) (4 * itemView.getContext().getResources().getDisplayMetrics().density);
            int padding = (int) (12 * itemView.getContext().getResources().getDisplayMetrics().density);
            
            // 获取屏幕宽度
            int screenWidth = itemView.getContext().getResources().getDisplayMetrics().widthPixels;
            int availableWidth = screenWidth - (int) (32 * itemView.getContext().getResources().getDisplayMetrics().density); // 减去左右padding
            
            LinearLayout currentRow = null;
            int currentRowWidth = 0;
            
            for (String tag : tags) {
                if (tag == null || tag.trim().isEmpty()) {
                    continue;
                }
                
                TextView tagView = new TextView(itemView.getContext());
                tagView.setText(tag.trim());
                tagView.setTextSize(12);
                tagView.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.primary_blue_gray));
                tagView.setPadding(padding, padding / 2, padding, padding / 2);
                
                // 创建圆角背景
                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setCornerRadius(16);
                drawable.setColor(Color.parseColor("#E3F2FD")); // 浅蓝色背景
                tagView.setBackground(drawable);
                
                // 测量标签宽度
                tagView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int tagWidth = tagView.getMeasuredWidth() + marginEnd;
                
                // 如果需要换行或当前行为空
                if (currentRow == null || currentRowWidth + tagWidth > availableWidth) {
                    currentRow = new LinearLayout(itemView.getContext());
                    currentRow.setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    rowParams.setMargins(0, 0, 0, marginBottom);
                    currentRow.setLayoutParams(rowParams);
                    llTags.addView(currentRow);
                    currentRowWidth = 0;
                }
                
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, marginEnd, 0);
                tagView.setLayoutParams(params);
                
                currentRow.addView(tagView);
                currentRowWidth += tagWidth;
            }
        }
    }
}

