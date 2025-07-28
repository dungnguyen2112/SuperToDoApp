package com.example.todolist;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ColorPickerAdapter extends RecyclerView.Adapter<ColorPickerAdapter.ColorViewHolder> {
    private List<String> colors;
    private int selectedPosition = 0; // Default first color selected
    private OnColorSelectedListener listener;

    public interface OnColorSelectedListener {
        void onColorSelected(String color, int position);
    }

    public ColorPickerAdapter(List<String> colors) {
        this.colors = colors;
    }

    public void setOnColorSelectedListener(OnColorSelectedListener listener) {
        this.listener = listener;
    }

    public String getSelectedColor() {
        if (selectedPosition >= 0 && selectedPosition < colors.size()) {
            return colors.get(selectedPosition);
        }
        return colors.get(0); // fallback to first color
    }

    public void setSelectedColor(String color) {
        for (int i = 0; i < colors.size(); i++) {
            if (colors.get(i).equals(color)) {
                int oldPosition = selectedPosition;
                selectedPosition = i;
                notifyItemChanged(oldPosition);
                notifyItemChanged(selectedPosition);
                break;
            }
        }
    }

    @NonNull
    @Override
    public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_color_picker, parent, false);
        return new ColorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
        String color = colors.get(position);
        
        // Set color by creating a new drawable
        try {
            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            drawable.setColor(Color.parseColor(color));
            drawable.setStroke(4, Color.GRAY);
            holder.viewColorSample.setBackground(drawable);
        } catch (Exception e) {
            holder.viewColorSample.setBackgroundColor(Color.GRAY);
        }
        
        // Show/hide selected indicator
        boolean isSelected = position == selectedPosition;
        holder.ivColorSelected.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        
        // Handle click
        holder.itemView.setOnClickListener(v -> {
            int oldPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            
            notifyItemChanged(oldPosition);
            notifyItemChanged(selectedPosition);
            
            if (listener != null) {
                listener.onColorSelected(color, selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return colors.size();
    }

    static class ColorViewHolder extends RecyclerView.ViewHolder {
        View viewColorSample;
        ImageView ivColorSelected;

        ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            viewColorSample = itemView.findViewById(R.id.viewColorSample);
            ivColorSelected = itemView.findViewById(R.id.ivColorSelected);
        }
    }
} 