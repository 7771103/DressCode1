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
        // 按时间插入到正确位置，最新的在最前面（最左边）
        int insertPosition = findInsertPosition(item);
        items.add(insertPosition, item);
        notifyItemInserted(insertPosition);
    }
    
    /**
     * 根据时间找到新项应该插入的位置，保持按时间降序排序
     * 返回应该插入的位置索引
     */
    private int findInsertPosition(WardrobeItem newItem) {
        if (items.isEmpty()) {
            return 0;
        }
        
        String newTime = newItem.getCreatedAt();
        if (newTime == null) {
            // 如果新项没有时间，插入到最后
            return items.size();
        }
        
        // 找到第一个时间小于新项时间的位置
        for (int i = 0; i < items.size(); i++) {
            WardrobeItem existingItem = items.get(i);
            String existingTime = existingItem.getCreatedAt();
            
            if (existingTime == null) {
                // 如果已有项没有时间，新项应该排在它前面
                return i;
            }
            
            // 比较时间，新项时间更大（更新）则应该排在前面
            int compareResult = compareTime(newTime, existingTime);
            if (compareResult > 0) {
                // 新项时间更新，应该插入到这个位置
                return i;
            }
        }
        
        // 新项时间最旧，插入到最后
        return items.size();
    }
    
    /**
     * 比较两个时间字符串
     * @return 正数表示 time1 > time2（time1 更新），负数表示 time1 < time2（time1 更旧），0 表示相等
     */
    private int compareTime(String time1, String time2) {
        if (time1 == null && time2 == null) {
            return 0;
        }
        if (time1 == null) {
            return -1;
        }
        if (time2 == null) {
            return 1;
        }
        
        try {
            String t1 = normalizeTimeString(time1);
            String t2 = normalizeTimeString(time2);
            
            String[] formats = {
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS"
            };
            
            java.util.Date date1 = null;
            java.util.Date date2 = null;
            
            for (String format : formats) {
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(format, java.util.Locale.getDefault());
                    if (date1 == null) {
                        date1 = sdf.parse(t1);
                    }
                    if (date2 == null) {
                        date2 = sdf.parse(t2);
                    }
                    if (date1 != null && date2 != null) {
                        break;
                    }
                } catch (java.text.ParseException e) {
                    // 继续尝试下一个格式
                }
            }
            
            if (date1 != null && date2 != null) {
                return date1.compareTo(date2);
            }
        } catch (Exception e) {
            // 解析失败，按字符串比较
            return time1.compareTo(time2);
        }
        
        return 0;
    }
    
    /**
     * 规范化时间字符串，移除时区标识和毫秒部分
     */
    private String normalizeTimeString(String timeStr) {
        if (timeStr == null) {
            return "";
        }
        String result = timeStr.trim();
        // 移除时区标识
        if (result.contains("Z")) {
            result = result.replace("Z", "");
        }
        if (result.contains("+")) {
            result = result.substring(0, result.indexOf("+"));
        }
        if (result.contains("-") && result.lastIndexOf("-") > 10) {
            // 处理时区偏移，如 "-05:00"
            int lastDashIndex = result.lastIndexOf("-");
            if (lastDashIndex > 10 && result.length() > lastDashIndex + 5) {
                String potentialOffset = result.substring(lastDashIndex);
                if (potentialOffset.matches("-[0-9]{2}:[0-9]{2}")) {
                    result = result.substring(0, lastDashIndex);
                }
            }
        }
        // 处理毫秒部分，只保留到秒
        if (result.contains(".")) {
            int dotIndex = result.indexOf(".");
            result = result.substring(0, dotIndex);
        }
        return result;
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

