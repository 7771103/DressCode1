package com.example.dresscode1.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dresscode1.R;
import com.example.dresscode1.network.dto.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 1;
    private static final int TYPE_ASSISTANT = 2;

    private List<ChatMessage> messages = new ArrayList<>();
    private OnRecommendationClickListener recommendationClickListener;
    
    public List<ChatMessage> getMessages() {
        return messages;
    }
    
    public interface OnRecommendationClickListener {
        void onPostClick(int postId);
        void onUserClick(int userId);
    }
    
    public void setOnRecommendationClickListener(OnRecommendationClickListener listener) {
        this.recommendationClickListener = listener;
    }

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
        private LinearLayout llRecommendations;
        private LinearLayout llRecommendedPosts;
        private LinearLayout llRecommendedUsers;
        private TextView tvRecommendedPostsTitle;
        private RecyclerView rvRecommendedPosts;
        private RecyclerView rvRecommendedUsers;
        private RecommendedPostAdapter postAdapter;
        private RecommendedUserAdapter userAdapter;

        public AssistantMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvMessageContent = itemView.findViewById(R.id.tvMessageContent);
            
            // 查找推荐区域
            llRecommendations = itemView.findViewById(R.id.llRecommendations);
            llRecommendedPosts = itemView.findViewById(R.id.llRecommendedPosts);
            llRecommendedUsers = itemView.findViewById(R.id.llRecommendedUsers);
            tvRecommendedPostsTitle = itemView.findViewById(R.id.tvRecommendedPostsTitle);
            rvRecommendedPosts = itemView.findViewById(R.id.rvRecommendedPosts);
            rvRecommendedUsers = itemView.findViewById(R.id.rvRecommendedUsers);
            
            // 设置水平滑动的LayoutManager
            if (rvRecommendedPosts != null) {
                LinearLayoutManager postLayoutManager = new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false);
                rvRecommendedPosts.setLayoutManager(postLayoutManager);
                // 设置padding以便第一个和最后一个item可以正确显示
                int padding = (int) (8 * itemView.getContext().getResources().getDisplayMetrics().density);
                rvRecommendedPosts.setPadding(padding, 0, padding, 0);
                // 确保RecyclerView可以滚动
                rvRecommendedPosts.setHasFixedSize(false);
                postAdapter = new RecommendedPostAdapter(post -> {
                    if (recommendationClickListener != null && post != null) {
                        recommendationClickListener.onPostClick(post.getId());
                    }
                });
                rvRecommendedPosts.setAdapter(postAdapter);
            }
            
            if (rvRecommendedUsers != null) {
                LinearLayoutManager userLayoutManager = new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false);
                rvRecommendedUsers.setLayoutManager(userLayoutManager);
                // 设置padding以便第一个和最后一个item可以正确显示
                int padding = (int) (8 * itemView.getContext().getResources().getDisplayMetrics().density);
                rvRecommendedUsers.setPadding(padding, 0, padding, 0);
                // 确保RecyclerView可以滚动
                rvRecommendedUsers.setHasFixedSize(false);
                userAdapter = new RecommendedUserAdapter(userId -> {
                    if (recommendationClickListener != null) {
                        recommendationClickListener.onUserClick(userId);
                    }
                });
                rvRecommendedUsers.setAdapter(userAdapter);
            }
        }

        public void bind(ChatMessage message) {
            tvMessageContent.setText(message.getContent() != null ? message.getContent() : "");
            
            // 显示推荐的帖子和博主
            boolean hasPostRecommendations = message.getRecommendedPostObjects() != null && !message.getRecommendedPostObjects().isEmpty();
            boolean hasUserRecommendations = message.getRecommendedUserObjects() != null && !message.getRecommendedUserObjects().isEmpty();
            boolean hasRecommendations = hasPostRecommendations || hasUserRecommendations;
            
            android.util.Log.d("MessageAdapter", "Binding message - hasPostRecommendations: " + hasPostRecommendations + 
                ", hasUserRecommendations: " + hasUserRecommendations + 
                ", postCount: " + (message.getRecommendedPostObjects() != null ? message.getRecommendedPostObjects().size() : 0) +
                ", userCount: " + (message.getRecommendedUserObjects() != null ? message.getRecommendedUserObjects().size() : 0));
            
            if (hasRecommendations && llRecommendations != null) {
                llRecommendations.setVisibility(View.VISIBLE);
                
                // 显示帖子推荐
                if (hasPostRecommendations && llRecommendedPosts != null && postAdapter != null) {
                    llRecommendedPosts.setVisibility(View.VISIBLE);
                    postAdapter.setPosts(message.getRecommendedPostObjects());
                    // 传递推荐类型信息
                    if (message.getPostRecommendationTypes() != null) {
                        postAdapter.setPostRecommendationTypes(message.getPostRecommendationTypes());
                    }
                    
                    // 根据推荐类型更新标题
                    if (tvRecommendedPostsTitle != null && message.getPostRecommendationTypes() != null) {
                        boolean hasHot = false;
                        boolean hasRecommended = false;
                        for (String type : message.getPostRecommendationTypes().values()) {
                            if ("hot".equals(type)) {
                                hasHot = true;
                            } else if ("recommended".equals(type)) {
                                hasRecommended = true;
                            }
                        }
                        
                        if (hasHot && !hasRecommended) {
                            // 只有热门推荐
                            tvRecommendedPostsTitle.setText("🔥 热门推荐");
                        } else if (hasRecommended && hasHot) {
                            // 既有推荐又有热门推荐
                            tvRecommendedPostsTitle.setText("📌 推荐帖子（包含热门推荐）");
                        } else {
                            // 只有推荐
                            tvRecommendedPostsTitle.setText("📌 推荐帖子");
                        }
                    }
                    
                    android.util.Log.d("MessageAdapter", "Showing " + message.getRecommendedPostObjects().size() + " recommended posts");
                } else if (llRecommendedPosts != null) {
                    llRecommendedPosts.setVisibility(View.GONE);
                }
                
                // 显示博主推荐
                if (hasUserRecommendations && llRecommendedUsers != null && userAdapter != null) {
                    llRecommendedUsers.setVisibility(View.VISIBLE);
                    userAdapter.setUsers(message.getRecommendedUserObjects());
                    android.util.Log.d("MessageAdapter", "Showing " + message.getRecommendedUserObjects().size() + " recommended users");
                } else if (llRecommendedUsers != null) {
                    llRecommendedUsers.setVisibility(View.GONE);
                }
            } else {
                if (llRecommendations != null) {
                    llRecommendations.setVisibility(View.GONE);
                }
            }
        }
    }
}

