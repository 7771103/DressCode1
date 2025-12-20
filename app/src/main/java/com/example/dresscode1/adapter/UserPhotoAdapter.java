package com.example.dresscode1.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.dresscode1.R;

import java.util.ArrayList;
import java.util.List;

public class UserPhotoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ADD_BUTTON = 0;
    private static final int TYPE_ITEM = 1;

    private List<Uri> photoUris = new ArrayList<>();
    private OnItemClickListener listener;
    private OnAddButtonClickListener addButtonListener;
    private int selectedPosition = -1;

    public interface OnItemClickListener {
        void onItemClick(Uri photoUri, int position);
    }

    public interface OnAddButtonClickListener {
        void onAddButtonClick();
    }

    public UserPhotoAdapter(OnItemClickListener listener, OnAddButtonClickListener addButtonListener) {
        this.listener = listener;
        this.addButtonListener = addButtonListener;
    }

    public void setPhotos(List<Uri> photos) {
        this.photoUris = photos != null ? photos : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addPhoto(Uri photoUri) {
        if (photoUris == null) {
            photoUris = new ArrayList<>();
        }
        photoUris.add(photoUri);
        notifyItemInserted(photoUris.size() - 1);
    }

    public Uri getSelectedPhoto() {
        if (selectedPosition >= 0 && selectedPosition < photoUris.size()) {
            return photoUris.get(selectedPosition);
        }
        return null;
    }

    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        if (oldPosition >= 0) {
            notifyItemChanged(oldPosition);
        }
        if (selectedPosition >= 0) {
            notifyItemChanged(selectedPosition);
        }
    }

    @Override
    public int getItemViewType(int position) {
        // 最后一个位置是加号按钮
        return position == getItemCount() - 1 ? TYPE_ADD_BUTTON : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ADD_BUTTON) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user_photo_add_button, parent, false);
            return new AddButtonViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user_photo, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AddButtonViewHolder) {
            ((AddButtonViewHolder) holder).bind();
        } else if (holder instanceof ItemViewHolder) {
            if (position < photoUris.size()) {
                ((ItemViewHolder) holder).bind(photoUris.get(position), position);
            }
        }
    }

    @Override
    public int getItemCount() {
        // 照片数量 + 加号按钮
        return (photoUris != null ? photoUris.size() : 0) + 1;
    }

    class AddButtonViewHolder extends RecyclerView.ViewHolder {
        private View itemView;

        public AddButtonViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
        }

        public void bind() {
            itemView.setOnClickListener(v -> {
                if (addButtonListener != null) {
                    addButtonListener.onAddButtonClick();
                }
            });
        }
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivPhoto;
        private View itemView;
        private View selectedIndicator;
        private ImageView ivCheckMark;
        private androidx.cardview.widget.CardView cardView;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            ivPhoto = itemView.findViewById(R.id.ivPhoto);
            selectedIndicator = itemView.findViewById(R.id.selectedIndicator);
            ivCheckMark = itemView.findViewById(R.id.ivCheckMark);
            cardView = itemView.findViewById(R.id.cardView);
        }

        public void bind(Uri photoUri, int position) {
            // 加载图片 - 使用 fitCenter 确保图片完整显示
            Glide.with(itemView.getContext())
                    .load(photoUri)
                    .fitCenter()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(ivPhoto);

            // 设置选中状态
            boolean isSelected = position == selectedPosition;
            if (isSelected) {
                // 选中状态：显示边框、选中标记、增加阴影和缩放
                selectedIndicator.setVisibility(View.VISIBLE);
                ivCheckMark.setVisibility(View.VISIBLE);
                cardView.setCardElevation(8f);
                itemView.setAlpha(1.0f);
                itemView.setScaleX(1.05f);
                itemView.setScaleY(1.05f);
            } else {
                // 未选中状态：隐藏边框和标记、正常阴影和大小
                selectedIndicator.setVisibility(View.GONE);
                ivCheckMark.setVisibility(View.GONE);
                cardView.setCardElevation(2f);
                itemView.setAlpha(0.85f);
                itemView.setScaleX(1.0f);
                itemView.setScaleY(1.0f);
            }

            // 点击事件
            itemView.setOnClickListener(v -> {
                setSelectedPosition(position);
                if (listener != null) {
                    listener.onItemClick(photoUri, position);
                }
            });
        }
    }
}

