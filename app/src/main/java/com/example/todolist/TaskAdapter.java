package com.example.todolist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_TASK = 0;
    private static final int TYPE_LOADING = 1;

    private final Context context;
    private List<Task> tasks;
    private OnTaskClickListener listener;
    private boolean isLoading = false;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onTaskDelete(Task task);
        void onTaskStatusChange(Task task);
    }

    public TaskAdapter(Context context, List<Task> tasks) {
        this.context = context;
        this.tasks = tasks;
    }

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.listener = listener;
    }

    public void updateTasks(List<Task> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
    }

    public void addTasks(List<Task> newTasks) {
        if (newTasks != null && !newTasks.isEmpty()) {
            int startPosition = tasks.size();
            // Remove loading item if it exists
            if (isLoading && startPosition > 0) {
                isLoading = false;
                notifyItemRemoved(startPosition);
            }
            this.tasks.addAll(newTasks);
            notifyItemRangeInserted(startPosition, newTasks.size());
        }
    }

    public void setLoading(boolean loading) {
        boolean wasLoading = isLoading;
        isLoading = loading;

        if (loading && !wasLoading) {
            // Add loading item
            notifyItemInserted(tasks.size());
        } else if (!loading && wasLoading) {
            // Remove loading item
            notifyItemRemoved(tasks.size());
        }
    }

    public void removeTask(int taskId) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId() == taskId) {
                tasks.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    public void updateTask(Task updatedTask) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId() == updatedTask.getId()) {
                tasks.set(i, updatedTask);
                notifyItemChanged(i);
                break;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isLoading && position == tasks.size()) {
            return TYPE_LOADING;
        }
        return TYPE_TASK;
    }

    @Override
    public int getItemCount() {
        return tasks.size() + (isLoading ? 1 : 0);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_LOADING) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
            return new TaskViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_TASK) {
            TaskViewHolder taskHolder = (TaskViewHolder) holder;
            Task task = tasks.get(position);

            taskHolder.textTitle.setText(task.getTitle());
            taskHolder.textDescription.setText(task.getDescription() != null && !task.getDescription().isEmpty()
                ? task.getDescription() : "No description");
            taskHolder.textDate.setText(task.getCreatedDate());

            // Handle topic display
            if (task.getTopic() != null && !task.getTopic().isEmpty()) {
                taskHolder.textTopic.setText(task.getTopic().toUpperCase());
                taskHolder.textTopic.setVisibility(View.VISIBLE);
            } else {
                taskHolder.textTopic.setVisibility(View.GONE);
            }

            // Set checkbox state without triggering listener
            taskHolder.checkboxCompleted.setOnCheckedChangeListener(null);
            taskHolder.checkboxCompleted.setChecked(task.isCompleted());

            // Set checkbox listener
            taskHolder.checkboxCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                task.setCompleted(isChecked);
                if (listener != null) {
                    listener.onTaskStatusChange(task);
                }
            });

            // Set click listeners
            taskHolder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskClick(task);
                }
            });

            taskHolder.buttonDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskDelete(task);
                }
            });
        }
        // Loading items don't need binding
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textDescription, textDate, textTopic;
        CheckBox checkboxCompleted;
        ImageButton buttonDelete;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_title);
            textDescription = itemView.findViewById(R.id.text_description);
            textDate = itemView.findViewById(R.id.text_date);
            textTopic = itemView.findViewById(R.id.text_topic);
            checkboxCompleted = itemView.findViewById(R.id.checkbox_completed);
            buttonDelete = itemView.findViewById(R.id.button_delete);
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;

        LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progress_bar);
        }
    }
}
