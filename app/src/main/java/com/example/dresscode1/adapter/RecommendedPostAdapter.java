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
import com.example.dresscode1.network.dto.Post;

import java.util.ArrayList;
import java.util.List;

public class RecommendedPostAdapter extends RecyclerView.Adapter<RecommendedPostAdapter.PostViewHolder> {

    private List<Post> posts = new ArrayList<>();
    private java.util.Map<Integer, String> postRecommendationTypes; // {postId: "recommended" or "hot"}
    private OnPostClickListener listener;

    public interface OnPostClickListener {
        void onPostClick(Post post);
    }

    public RecommendedPostAdapter(OnPostClickListener listener) {
        this.listener = listener;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts != null ? posts : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void setPostRecommendationTypes(java.util.Map<Integer, String> postRecommendationTypes) {
        this.postRecommendationTypes = postRecommendationTypes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recommended_post, parent, false);
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
        private ImageView ivPostImage;
        private TextView tvContent;
        private ImageView ivUserAvatar;
        private TextView tvUserNickname;
        private TextView tvRecommendationType;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            tvContent = itemView.findViewById(R.id.tvContent);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvUserNickname = itemView.findViewById(R.id.tvUserNickname);
            tvRecommendationType = itemView.findViewById(R.id.tvRecommendationType);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position < posts.size() && listener != null) {
                    listener.onPostClick(posts.get(position));
                }
            });
        }

        public void bind(Post post) {
            // 设置帖子内容
            if (post.getContent() != null && !post.getContent().isEmpty()) {
                tvContent.setText(post.getContent());
                tvContent.setVisibility(View.VISIBLE);
            } else {
                tvContent.setVisibility(View.GONE);
            }

            // 设置用户昵称
            tvUserNickname.setText(post.getUserNickname() != null ? post.getUserNickname() : "未知用户");

            // 加载用户头像
            String avatarUrl = ApiClient.getAvatarUrl(post.getUserAvatar());
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(avatarUrl)
                        .circleCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .into(ivUserAvatar);
            } else {
                ivUserAvatar.setImageDrawable(null);
            }

            // 加载帖子图片
            String imageUrl = ApiClient.getImageUrl(post.getImagePath());
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .centerCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .into(ivPostImage);
                ivPostImage.setVisibility(View.VISIBLE);
            } else {
                ivPostImage.setVisibility(View.GONE);
            }
            
            // 显示推荐类型标签
            if (tvRecommendationType != null && postRecommendationTypes != null) {
                String recommendationType = postRecommendationTypes.get(post.getId());
                if ("hot".equals(recommendationType)) {
                    tvRecommendationType.setText("热门");
                    tvRecommendationType.setVisibility(View.VISIBLE);
                } else if ("recommended".equals(recommendationType)) {
                    tvRecommendationType.setText("推荐");
                    tvRecommendationType.setVisibility(View.VISIBLE);
                } else {
                    tvRecommendationType.setVisibility(View.GONE);
                }
            } else if (tvRecommendationType != null) {
                tvRecommendationType.setVisibility(View.GONE);
            }
        }
    }
}

