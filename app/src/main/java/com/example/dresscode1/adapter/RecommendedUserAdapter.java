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
import com.example.dresscode1.network.dto.UserListItem;

import java.util.ArrayList;
import java.util.List;

public class RecommendedUserAdapter extends RecyclerView.Adapter<RecommendedUserAdapter.UserViewHolder> {

    private List<UserListItem> users = new ArrayList<>();
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(int userId);
    }

    public RecommendedUserAdapter(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<UserListItem> users) {
        this.users = users != null ? users : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recommended_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserListItem user = users.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivUserAvatar;
        private TextView tvUserNickname;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvUserNickname = itemView.findViewById(R.id.tvUserNickname);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onUserClick(users.get(position).getId());
                }
            });
        }

        public void bind(UserListItem user) {
            // 设置用户昵称
            tvUserNickname.setText(user.getNickname() != null ? user.getNickname() : "未知用户");

            // 加载用户头像
            String avatarUrl = ApiClient.getAvatarUrl(user.getAvatar());
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
        }
    }
}

