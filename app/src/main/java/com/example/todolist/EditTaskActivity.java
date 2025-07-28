package com.example.todolist;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.flexbox.FlexboxLayout;
import android.app.AlertDialog;
import android.graphics.Color;
import android.widget.Button;
import android.widget.TextView;
import java.util.Arrays;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;

public class EditTaskActivity extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    private ReminderAlarmManager reminderAlarmManager;
    private TagManager tagManager;
    private EditText editTitle, editDescription, editTopic;
    private CheckBox checkboxReminder;
    private Button btnSelectTags;
    private FlexboxLayout flexboxSelectedTags;
    private Task currentTask;
    private List<Tag> selectedTags = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize theme before calling super.onCreate()
        ThemeManager.getInstance(this).initializeTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        databaseHelper = new DatabaseHelper(this);
        reminderAlarmManager = new ReminderAlarmManager(this);
        tagManager = TagManager.getInstance(this);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Task");
        }

        initViews();
        loadTaskData();
        setupListeners();
    }

    private void initViews() {
        editTitle = findViewById(R.id.edit_title);
        editDescription = findViewById(R.id.edit_description);
        editTopic = findViewById(R.id.edit_topic);
        checkboxReminder = findViewById(R.id.checkbox_reminder);
        btnSelectTags = findViewById(R.id.btnSelectTags);
        flexboxSelectedTags = findViewById(R.id.flexboxSelectedTags);
    }

    private void loadTaskData() {
        int taskId = getIntent().getIntExtra("task_id", -1);

        android.util.Log.d("EditTaskActivity", "Loading task data - ID: " + taskId);

        if (taskId != -1) {
            // Load complete task data from database
            currentTask = databaseHelper.getTaskById(taskId);
            
            if (currentTask != null) {
                // Set text in EditText fields
                editTitle.setText(currentTask.getTitle() != null ? currentTask.getTitle() : "");
                editDescription.setText(currentTask.getDescription() != null ? currentTask.getDescription() : "");
                editTopic.setText(currentTask.getTopic() != null ? currentTask.getTopic() : "");
                
                // Set reminder checkbox - simplified UI
                checkboxReminder.setChecked(currentTask.isReminderEnabled());

                // Load and display tags
                selectedTags = new ArrayList<>(currentTask.getTags());
                updateSelectedTagsDisplay();

                android.util.Log.d("EditTaskActivity", "Task loaded successfully with ID: " + currentTask.getId());
            } else {
                android.util.Log.e("EditTaskActivity", "Task not found in database");
                Toast.makeText(this, "Error: Task not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            android.util.Log.e("EditTaskActivity", "Invalid task ID: " + taskId);
            Toast.makeText(this, "Error: Invalid task data", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupListeners() {
        findViewById(R.id.button_save).setOnClickListener(v -> saveTask());
        findViewById(R.id.button_cancel).setOnClickListener(v -> showCancelConfirmation());
        btnSelectTags.setOnClickListener(v -> showTagSelectionDialog());
        
        // No additional setup needed for reminder checkbox - just enable/disable automatic reminders
    }

    private void showTagSelectionDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_tag_selector, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText etTagSearch = dialogView.findViewById(R.id.etTagSearch);
        Button btnCreateNewTag = dialogView.findViewById(R.id.btnCreateNewTag);
        RecyclerView rvTagsSelector = dialogView.findViewById(R.id.rvTagsSelector);
        Button btnCancelTagSelection = dialogView.findViewById(R.id.btnCancelTagSelection);
        Button btnConfirmTagSelection = dialogView.findViewById(R.id.btnConfirmTagSelection);

        // Setup RecyclerView
        TagSelectionAdapter adapter = new TagSelectionAdapter(new ArrayList<>(selectedTags));
        rvTagsSelector.setLayoutManager(new LinearLayoutManager(this));
        rvTagsSelector.setAdapter(adapter);

        // Load all tags
        List<Tag> allTags = tagManager.getAllTags();
        adapter.updateTags(allTags);

        // Search functionality
        etTagSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                List<Tag> filteredTags = tagManager.searchTagsByName(query);
                adapter.updateTags(filteredTags);

                // Show/hide create new tag button
                boolean canCreateNew = !query.isEmpty() && tagManager.isTagNameAvailable(query);
                btnCreateNewTag.setVisibility(canCreateNew ? View.VISIBLE : View.GONE);
                btnCreateNewTag.setText("Tạo tag: \"" + query + "\"");
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // Create new tag
        btnCreateNewTag.setOnClickListener(v -> {
            String tagName = etTagSearch.getText().toString().trim();
            if (!tagName.isEmpty()) {
                showCreateTagDialog(tagName, (newTag) -> {
                    List<Tag> updatedTags = tagManager.getAllTags();
                    adapter.updateTags(updatedTags);
                    adapter.setTagSelected(newTag, true);
                    btnCreateNewTag.setVisibility(View.GONE);
                    etTagSearch.setText("");
                });
            }
        });

        // Dialog buttons
        btnCancelTagSelection.setOnClickListener(v -> dialog.dismiss());
        
        btnConfirmTagSelection.setOnClickListener(v -> {
            selectedTags = adapter.getSelectedTags();
            updateSelectedTagsDisplay();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateSelectedTagsDisplay() {
        flexboxSelectedTags.removeAllViews();

        for (Tag tag : selectedTags) {
            View tagChip = getLayoutInflater().inflate(R.layout.item_tag_chip, flexboxSelectedTags, false);
            
            View colorDot = tagChip.findViewById(R.id.viewTagColorDot);
            TextView tagName = tagChip.findViewById(R.id.tvTagChipName);
            View removeButton = tagChip.findViewById(R.id.ivRemoveTag);

            try {
                colorDot.setBackgroundColor(Color.parseColor(tag.getColor()));
            } catch (Exception e) {
                colorDot.setBackgroundColor(Color.GRAY);
            }
            
            tagName.setText(tag.getName());
            removeButton.setVisibility(View.VISIBLE);
            
            removeButton.setOnClickListener(v -> {
                selectedTags.remove(tag);
                updateSelectedTagsDisplay();
            });

            flexboxSelectedTags.addView(tagChip);
        }
    }

    private void showCancelConfirmation() {
        // Check if there are unsaved changes
        String currentTitle = editTitle.getText().toString().trim();
        String currentDescription = editDescription.getText().toString().trim();
        String currentTopicText = editTopic.getText().toString().trim();

        boolean hasChanges = !currentTitle.equals(currentTask.getTitle() != null ? currentTask.getTitle() : "") ||
                           !currentDescription.equals(currentTask.getDescription() != null ? currentTask.getDescription() : "") ||
                           !currentTopicText.equals(currentTask.getTopic() != null ? currentTask.getTopic() : "") ||
                           !selectedTags.equals(currentTask.getTags());

        if (hasChanges) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Discard Changes?")
                .setMessage("Are you sure you want to discard your changes?")
                .setPositiveButton("Discard", (dialog, which) -> finish())
                .setNegativeButton("Continue Editing", null)
                .show();
        } else {
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            showCancelConfirmation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveTask() {
        String title = editTitle.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String topic = editTopic.getText().toString().trim();

        android.util.Log.d("EditTaskActivity", "Saving task - Title: " + title);

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter task title", Toast.LENGTH_SHORT).show();
            editTitle.requestFocus();
            return;
        }

        if (currentTask == null) {
            Toast.makeText(this, "Error: Task data not loaded properly", Toast.LENGTH_SHORT).show();
            android.util.Log.e("EditTaskActivity", "currentTask is null!");
            return;
        }

        // Update task data
        currentTask.setTitle(title);
        currentTask.setDescription(description);
        currentTask.setTopic(topic.isEmpty() ? null : topic);
        currentTask.setTags(selectedTags);
        
        // Update reminder data - simplified (no specific time needed)
        currentTask.setReminderEnabled(checkboxReminder.isChecked());

        android.util.Log.d("EditTaskActivity", "Updating task with ID: " + currentTask.getId());

        try {
            int result = databaseHelper.updateTask(currentTask);
            android.util.Log.d("EditTaskActivity", "Update result: " + result);

            if (result > 0) {
                // Update alarm/reminder
                reminderAlarmManager.updateReminder(currentTask);
                
                // Update widget immediately after task update
                android.util.Log.d("EditTaskActivity", "Triggering widget update after task edit");
                TaskWidgetProvider.updateAllWidgets(this);
                
                Toast.makeText(this, "Task updated successfully", Toast.LENGTH_SHORT).show();

                // Return updated data
                Intent resultIntent = new Intent();
                resultIntent.putExtra("updated_task_id", currentTask.getId());
                resultIntent.putExtra("updated_task_title", currentTask.getTitle());
                resultIntent.putExtra("updated_task_description", currentTask.getDescription());
                resultIntent.putExtra("updated_task_topic", currentTask.getTopic());
                resultIntent.putExtra("updated_task_date", currentTask.getCreatedDate());
                resultIntent.putExtra("updated_task_completed", currentTask.isCompleted());
                resultIntent.putExtra("task_updated", true);
                setResult(RESULT_OK, resultIntent);

                finish();
            } else {
                Toast.makeText(this, "Failed to update task", Toast.LENGTH_SHORT).show();
                android.util.Log.e("EditTaskActivity", "Update failed - no rows affected");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error updating task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                         android.util.Log.e("EditTaskActivity", "Exception updating task", e);
        }
    }

    public interface OnTagCreatedListener {
        void onTagCreated(Tag tag);
    }

    private void showCreateTagDialog(String initialName, OnTagCreatedListener listener) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_tag, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText etNewTagName = dialogView.findViewById(R.id.etNewTagName);
        RecyclerView rvColorPicker = dialogView.findViewById(R.id.rvColorPicker);
        View viewPreviewColor = dialogView.findViewById(R.id.viewPreviewColor);
        TextView tvPreviewName = dialogView.findViewById(R.id.tvPreviewName);
        Button btnCancelCreateTag = dialogView.findViewById(R.id.btnCancelCreateTag);
        Button btnConfirmCreateTag = dialogView.findViewById(R.id.btnConfirmCreateTag);

        // Set initial name
        etNewTagName.setText(initialName);
        tvPreviewName.setText(initialName.isEmpty() ? "Tag Name" : initialName);
        
        // Set initial button state
        btnConfirmCreateTag.setEnabled(!initialName.isEmpty() && tagManager.isTagNameAvailable(initialName));

        // Setup color picker
        List<String> colors = Arrays.asList(tagManager.getAllColors());
        ColorPickerAdapter colorAdapter = new ColorPickerAdapter(colors);
        rvColorPicker.setLayoutManager(new GridLayoutManager(this, 5)); // 5 columns
        rvColorPicker.setAdapter(colorAdapter);

        // Set initial preview color
        String initialColor = colorAdapter.getSelectedColor();
        try {
            viewPreviewColor.setBackgroundColor(Color.parseColor(initialColor));
        } catch (Exception e) {
            viewPreviewColor.setBackgroundColor(Color.GRAY);
        }

        // Color selection listener
        colorAdapter.setOnColorSelectedListener((color, position) -> {
            try {
                viewPreviewColor.setBackgroundColor(Color.parseColor(color));
            } catch (Exception e) {
                viewPreviewColor.setBackgroundColor(Color.GRAY);
            }
        });

        // Name change listener
        etNewTagName.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String name = s.toString().trim();
                tvPreviewName.setText(name.isEmpty() ? "Tag Name" : name);
                btnConfirmCreateTag.setEnabled(!name.isEmpty() && tagManager.isTagNameAvailable(name));
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // Dialog buttons
        btnCancelCreateTag.setOnClickListener(v -> dialog.dismiss());
        
        btnConfirmCreateTag.setOnClickListener(v -> {
            String tagName = etNewTagName.getText().toString().trim();
            String selectedColor = colorAdapter.getSelectedColor();
            
            if (!tagName.isEmpty() && tagManager.isTagNameAvailable(tagName)) {
                Tag newTag = tagManager.createTag(tagName, selectedColor);
                if (listener != null) {
                    listener.onTagCreated(newTag);
                }
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Tên tag đã tồn tại hoặc không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}
