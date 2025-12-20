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
import com.example.dresscode1.network.dto.WardrobeItem;

import java.util.ArrayList;
import java.util.List;

public class WardrobeItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ADD_BUTTON = 0;
    private static final int TYPE_ITEM = 1;

    private List<WardrobeItem> items = new ArrayList<>();
    private OnItemClickListener listener;
    private OnAddButtonClickListener addButtonListener;
    private int selectedPosition = -1;

    public interface OnItemClickListener {
        void onItemClick(WardrobeItem item, int position);
    }

    public interface OnAddButtonClickListener {
        void onAddButtonClick();
    }

    public WardrobeItemAdapter(OnItemClickListener listener, OnAddButtonClickListener addButtonListener) {
        this.listener = listener;
        this.addButtonListener = addButtonListener;
    }

    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        // 注意：position 是实际item的索引，需要转换为RecyclerView的位置（+1因为第一个是加号按钮）
        if (oldPosition >= 0) {
            notifyItemChanged(oldPosition + 1);
        }
        if (selectedPosition >= 0) {
            notifyItemChanged(selectedPosition + 1);
        }
    }

    public WardrobeItem getSelectedItem() {
        if (selectedPosition >= 0 && selectedPosition < items.size()) {
            return items.get(selectedPosition);
        }
        return null;
    }

    public void setItems(List<WardrobeItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addItem(WardrobeItem item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(0, item);  // 添加到最前面（最左边）
        notifyItemInserted(0);
    }

    @Override
    public int getItemViewType(int position) {
        // 第一个位置是加号按钮
        return position == 0 ? TYPE_ADD_BUTTON : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ADD_BUTTON) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_wardrobe_add_button, parent, false);
            return new AddButtonViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_wardrobe_image, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AddButtonViewHolder) {
            ((AddButtonViewHolder) holder).bind();
        } else if (holder instanceof ItemViewHolder) {
            // position 0 是加号按钮，所以实际item的索引是 position - 1
            int itemIndex = position - 1;
            if (itemIndex >= 0 && itemIndex < items.size()) {
                ((ItemViewHolder) holder).bind(items.get(itemIndex), itemIndex);
            }
        }
    }

    @Override
    public int getItemCount() {
        // 加号按钮 + 图片数量
        return 1 + (items != null ? items.size() : 0);
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
        private ImageView ivImage;
        private TextView tvSourceType;
        private View selectedIndicator;
        private ImageView ivCheckMark;
        private androidx.cardview.widget.CardView cardView;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivImage);
            tvSourceType = itemView.findViewById(R.id.tvSourceType);
            selectedIndicator = itemView.findViewById(R.id.selectedIndicator);
            ivCheckMark = itemView.findViewById(R.id.ivCheckMark);
            cardView = itemView.findViewById(R.id.cardView);
        }

        public void bind(WardrobeItem item, int position) {
            // 加载图片 - 优先使用 imagePath，如果为空则使用 postImagePath
            String imagePath = item.getImagePath();
            if (imagePath == null || imagePath.isEmpty()) {
                imagePath = item.getPostImagePath();
            }
            
            if (imagePath != null && !imagePath.isEmpty()) {
                String imageUrl = ApiClient.getImageUrl(imagePath);
                if (imageUrl != null) {
                    Glide.with(itemView.getContext())
                            .load(imageUrl)
                            .centerCrop()
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_report_image)
                            .into(ivImage);
                } else {
                    // 如果 URL 为 null，显示占位符
                    ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } else {
                // 如果图片路径为空，显示占位符
                ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            // 显示来源标识
            tvSourceType.setText(item.getSourceTypeText());

            // 设置选中状态
            boolean isSelected = position == selectedPosition;
            if (isSelected) {
                // 选中状态：显示边框、选中标记、增加阴影和缩放
                if (selectedIndicator != null) {
                    selectedIndicator.setVisibility(View.VISIBLE);
                }
                if (ivCheckMark != null) {
                    ivCheckMark.setVisibility(View.VISIBLE);
                }
                if (cardView != null) {
                    cardView.setCardElevation(8f);
                }
                itemView.setAlpha(1.0f);
                itemView.setScaleX(1.05f);
                itemView.setScaleY(1.05f);
            } else {
                // 未选中状态：隐藏边框和标记、正常阴影和大小
                if (selectedIndicator != null) {
                    selectedIndicator.setVisibility(View.GONE);
                }
                if (ivCheckMark != null) {
                    ivCheckMark.setVisibility(View.GONE);
                }
                if (cardView != null) {
                    cardView.setCardElevation(2f);
                }
                itemView.setAlpha(0.85f);
                itemView.setScaleX(1.0f);
                itemView.setScaleY(1.0f);
            }

            // 点击事件
            itemView.setOnClickListener(v -> {
                setSelectedPosition(position);
                if (listener != null) {
                    listener.onItemClick(item, position);
                }
            });
        }
    }
}

