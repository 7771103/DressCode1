package com.example.dresscode1.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.dresscode1.R;
import com.example.dresscode1.network.dto.UserListItem;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.UserViewHolder> {

    private List<UserListItem> users = new ArrayList<>();
    private OnUserActionListener listener;
    private int currentUserId;

    public interface OnUserActionListener {
        void onUserClick(UserListItem user);
        void onFollowClick(UserListItem user, int position);
    }

    public UserListAdapter(OnUserActionListener listener, int currentUserId) {
        this.listener = listener;
        this.currentUserId = currentUserId;
    }

    public void setOnUserActionListener(OnUserActionListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<UserListItem> users) {
        this.users = users != null ? users : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void appendUsers(List<UserListItem> newUsers) {
        if (newUsers != null && !newUsers.isEmpty()) {
            int startPosition = users.size();
            this.users.addAll(newUsers);
            notifyItemRangeInserted(startPosition, newUsers.size());
        }
    }

    public void updateUser(int position, UserListItem user) {
        if (position >= 0 && position < users.size()) {
            users.set(position, user);
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
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
        private CircleImageView ivAvatar;
        private TextView tvNickname;
        private TextView tvPhone;
        private Button btnFollow;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvNickname = itemView.findViewById(R.id.tvNickname);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            btnFollow = itemView.findViewById(R.id.btnFollow);

            // 点击整个item跳转到用户主页
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onUserClick(users.get(position));
                }
            });

            // 点击关注按钮
            btnFollow.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onFollowClick(users.get(position), position);
                }
            });
        }

        public void bind(UserListItem user) {
            tvNickname.setText(user.getNickname() != null ? user.getNickname() : "用户" + user.getId());
            tvPhone.setText(user.getPhone() != null ? user.getPhone() : "");

            // 加载头像
            if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                String avatarUrl = "http://10.134.17.29:5000" + user.getAvatar();
                Glide.with(itemView.getContext()).load(avatarUrl).into(ivAvatar);
            } else {
                ivAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            // 更新关注按钮状态
            if (currentUserId > 0 && currentUserId != user.getId()) {
                btnFollow.setVisibility(View.VISIBLE);
                if (user.isFollowing()) {
                    btnFollow.setText("已关注");
                    btnFollow.setBackgroundTintList(ContextCompat.getColorStateList(itemView.getContext(), R.color.text_secondary));
                } else {
                    btnFollow.setText("关注");
                    btnFollow.setBackgroundTintList(ContextCompat.getColorStateList(itemView.getContext(), R.color.primary_blue_gray));
                }
            } else {
                btnFollow.setVisibility(View.GONE);
            }
        }
    }
}

