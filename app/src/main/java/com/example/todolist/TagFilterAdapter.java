package com.example.todolist;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Context;
import android.content.res.TypedArray;
import java.util.List;
import java.util.ArrayList;

public class TagFilterAdapter extends RecyclerView.Adapter<TagFilterAdapter.TagFilterViewHolder> {
    private List<TagWithCount> tagsWithCount = new ArrayList<>();
    private List<Tag> selectedTags = new ArrayList<>();
    private OnTagFilterListener listener;

    public interface OnTagFilterListener {
        void onTagFilterChanged(List<Tag> selectedTags);
    }

    public static class TagWithCount {
        private Tag tag;
        private int taskCount;

        public TagWithCount(Tag tag, int taskCount) {
            this.tag = tag;
            this.taskCount = taskCount;
        }

        public Tag getTag() { return tag; }
        public int getTaskCount() { return taskCount; }
    }

    public TagFilterAdapter() {}

    public void setOnTagFilterListener(OnTagFilterListener listener) {
        this.listener = listener;
    }

    public void updateTagsWithCount(List<TagWithCount> tagsWithCount) {
        this.tagsWithCount = new ArrayList<>(tagsWithCount);
        notifyDataSetChanged();
    }

    public void setSelectedTags(List<Tag> selectedTags) {
        this.selectedTags = new ArrayList<>(selectedTags);
        notifyDataSetChanged();
    }

    public List<Tag> getSelectedTags() {
        return new ArrayList<>(selectedTags);
    }

    public void clearSelection() {
        selectedTags.clear();
        notifyDataSetChanged();
        if (listener != null) {
            listener.onTagFilterChanged(selectedTags);
        }
    }

    private boolean isTagSelected(Tag tag) {
        for (Tag selectedTag : selectedTags) {
            if (selectedTag.getId() == tag.getId()) {
                return true;
            }
        }
        return false;
    }

    private void toggleTagSelection(Tag tag) {
        boolean wasSelected = false;
        for (int i = 0; i < selectedTags.size(); i++) {
            if (selectedTags.get(i).getId() == tag.getId()) {
                selectedTags.remove(i);
                wasSelected = true;
                break;
            }
        }
        
        if (!wasSelected) {
            selectedTags.add(tag);
        }
        
        notifyDataSetChanged();
        if (listener != null) {
            listener.onTagFilterChanged(selectedTags);
        }
    }

    @NonNull
    @Override
    public TagFilterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tag_filter, parent, false);
        return new TagFilterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TagFilterViewHolder holder, int position) {
        TagWithCount tagWithCount = tagsWithCount.get(position);
        Tag tag = tagWithCount.getTag();
        boolean isSelected = isTagSelected(tag);

        // Set tag name
        holder.tvTagName.setText(tag.getName());
        
        // Set task count
        holder.tvTaskCount.setText("(" + tagWithCount.getTaskCount() + ")");
        
        // Set color dot
        try {
            holder.viewTagColorDot.setBackgroundColor(Color.parseColor(tag.getColor()));
        } catch (Exception e) {
            holder.viewTagColorDot.setBackgroundColor(Color.GRAY);
        }
        
        // Set selected state
        holder.itemView.setSelected(isSelected);
        
        // Update text colors based on selection
        if (isSelected) {
            holder.tvTagName.setTextColor(Color.WHITE);
            holder.tvTaskCount.setTextColor(Color.WHITE);
        } else {
            // Use theme-aware colors that work with both light and dark mode
            int primaryTextColor = getThemeTextColor(holder.itemView.getContext(), android.R.attr.textColorPrimary);
            int secondaryTextColor = getThemeTextColor(holder.itemView.getContext(), android.R.attr.textColorSecondary);
            holder.tvTagName.setTextColor(primaryTextColor);
            holder.tvTaskCount.setTextColor(secondaryTextColor);
        }
        
        // Handle click
        holder.itemView.setOnClickListener(v -> toggleTagSelection(tag));
    }

    @Override
    public int getItemCount() {
        return tagsWithCount.size();
    }

    /**
     * Helper method to get theme-aware text color
     */
    private int getThemeTextColor(Context context, int attrRes) {
        TypedArray typedArray = context.obtainStyledAttributes(new int[]{attrRes});
        int color = typedArray.getColor(0, Color.BLACK); // fallback to black
        typedArray.recycle();
        return color;
    }

    static class TagFilterViewHolder extends RecyclerView.ViewHolder {
        View viewTagColorDot;
        TextView tvTagName;
        TextView tvTaskCount;

        TagFilterViewHolder(@NonNull View itemView) {
            super(itemView);
            viewTagColorDot = itemView.findViewById(R.id.viewTagColorDot);
            tvTagName = itemView.findViewById(R.id.tvTagName);
            tvTaskCount = itemView.findViewById(R.id.tvTaskCount);
        }
    }
} 