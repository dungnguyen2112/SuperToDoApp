package com.example.todolist;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class TagSelectionAdapter extends RecyclerView.Adapter<TagSelectionAdapter.TagViewHolder> {
    private List<Tag> tags;
    private List<Tag> selectedTags;

    public TagSelectionAdapter(List<Tag> initialSelectedTags) {
        this.tags = new ArrayList<>();
        this.selectedTags = new ArrayList<>(initialSelectedTags);
    }

    @NonNull
    @Override
    public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tag_selectable, parent, false);
        return new TagViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
        Tag tag = tags.get(position);
        
        holder.tvTagName.setText(tag.getName());
        
        // Set tag color
        try {
            holder.viewTagColor.setBackgroundColor(Color.parseColor(tag.getColor()));
        } catch (Exception e) {
            holder.viewTagColor.setBackgroundColor(Color.GRAY);
        }
        
        // Set checkbox state
        boolean isSelected = isTagSelected(tag);
        holder.cbTagSelected.setChecked(isSelected);
        
        // Handle click
        holder.itemView.setOnClickListener(v -> {
            boolean newState = !holder.cbTagSelected.isChecked();
            holder.cbTagSelected.setChecked(newState);
            setTagSelected(tag, newState);
        });
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }

    public void updateTags(List<Tag> newTags) {
        this.tags.clear();
        this.tags.addAll(newTags);
        notifyDataSetChanged();
    }

    public void setTagSelected(Tag tag, boolean selected) {
        if (selected) {
            if (!isTagSelected(tag)) {
                selectedTags.add(tag);
            }
        } else {
            selectedTags.removeIf(t -> t.getId() == tag.getId());
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

    public List<Tag> getSelectedTags() {
        return new ArrayList<>(selectedTags);
    }

    static class TagViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbTagSelected;
        View viewTagColor;
        TextView tvTagName;

        TagViewHolder(@NonNull View itemView) {
            super(itemView);
            cbTagSelected = itemView.findViewById(R.id.cbTagSelected);
            viewTagColor = itemView.findViewById(R.id.viewTagSelectableColor);
            tvTagName = itemView.findViewById(R.id.tvTagSelectableName);
        }
    }
} 