package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {
    private static final int PAGE_SIZE = 5; // 5 tasks per page

    private DatabaseHelper databaseHelper;
    private TaskAdapter taskAdapter;
    private RecyclerView recyclerView;
    private EditText editTitle, editDescription, editTopic, editDeadline;
    private Button buttonAddTask, buttonClearFilter;
    private Spinner spinnerTopicFilter;
    private List<Task> allTasks;

    // UI elements for collapsible add task section
    private LinearLayout layoutAddTaskHeader, layoutAddTaskContent;
    private ImageView iconToggle;
    private boolean isAddTaskExpanded = false;

    // UI elements for collapsible tasks due next week section
    private LinearLayout layoutTasksDueNextWeekHeader, layoutTasksDueNextWeekContent;
    private ImageView iconTasksDueToggle;
    private boolean isTasksDueNextWeekExpanded = true; // Mặc định mở

    // Pagination variables
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    private String currentFilterTopic = null;

    private ReminderService reminderService;
    private ReminderAlarmManager reminderAlarmManager;

    private RecyclerView recyclerTasksDueNextWeek;
    private TaskAdapter tasksDueNextWeekAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize theme before calling super.onCreate()
        ThemeManager.getInstance(this).initializeTheme();

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        reminderService = new ReminderService();
        reminderAlarmManager = new ReminderAlarmManager(this);

        initViews();
        setupDatabase();
        setupRecyclerView();
        setupClickListeners();
        setupAddTaskToggle();
        loadInitialTasks();

        // Move setupTopicFilter to the end and add safety check
        try {
            setupTopicFilter();
        } catch (Exception e) {
            e.printStackTrace();
            // Initialize empty filter if error occurs
            List<String> emptyTopics = new ArrayList<>();
            emptyTopics.add("All Topics");
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, emptyTopics);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            if (spinnerTopicFilter != null) {
                spinnerTopicFilter.setAdapter(adapter);
            }
        }

        recyclerTasksDueNextWeek = findViewById(R.id.recycler_tasks_due_next_week);
        tasksDueNextWeekAdapter = new TaskAdapter(this, new ArrayList<>());
        // Đảm bảo adapter này cũng có listener để click vào task hoạt động
        tasksDueNextWeekAdapter.setOnTaskClickListener(this);
        recyclerTasksDueNextWeek.setLayoutManager(new LinearLayoutManager(this));
        recyclerTasksDueNextWeek.setAdapter(tasksDueNextWeekAdapter);
        loadTasksDueNextWeek();
    }

    private void initViews() {
        // Setup toolbar first
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recycler_tasks);
        editTitle = findViewById(R.id.edit_title);
        editDescription = findViewById(R.id.edit_description);
        editTopic = findViewById(R.id.edit_topic);
        editDeadline = findViewById(R.id.edit_deadline);
        buttonAddTask = findViewById(R.id.button_add_task);
        buttonClearFilter = findViewById(R.id.button_clear_filter);
        spinnerTopicFilter = findViewById(R.id.spinner_topic_filter);

        // New UI elements
        layoutAddTaskHeader = findViewById(R.id.layout_add_task_header);
        layoutAddTaskContent = findViewById(R.id.layout_add_task_content);
        iconToggle = findViewById(R.id.icon_toggle);

        // Tasks Due Next Week UI elements
        layoutTasksDueNextWeekHeader = findViewById(R.id.layout_tasks_due_next_week_header);
        layoutTasksDueNextWeekContent = findViewById(R.id.layout_tasks_due_next_week_content);
        iconTasksDueToggle = findViewById(R.id.icon_tasks_due_toggle);
    }

    private void setupDatabase() {
        databaseHelper = new DatabaseHelper(this);
    }

    private void setupRecyclerView() {
        allTasks = new ArrayList<>();
        taskAdapter = new TaskAdapter(this, allTasks);
        taskAdapter.setOnTaskClickListener(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(taskAdapter);

        // Improved scroll listener for pagination
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading && hasMoreData) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    // Load more when user scrolls to near the end (within 2 items)
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2) {
                        android.util.Log.d("MainActivity", "Triggering load more - visible: " + visibleItemCount + ", total: " + totalItemCount + ", first: " + firstVisibleItemPosition);
                        loadMoreTasks();
                    }
                }
            }
        });
    }

    private void setupClickListeners() {
        buttonAddTask.setOnClickListener(v -> addTask());
        buttonClearFilter.setOnClickListener(v -> clearFilter());

        // Add date/time picker for deadline field
        editDeadline.setOnClickListener(v -> showDateTimePicker());
        editDeadline.setFocusable(false); // Prevent keyboard from showing
        editDeadline.setClickable(true);
    }

    private void setupAddTaskToggle() {
        layoutAddTaskHeader.setOnClickListener(v -> toggleAddTaskSection());

        // Setup Tasks Due Next Week toggle
        layoutTasksDueNextWeekHeader.setOnClickListener(v -> toggleTasksDueNextWeekSection());
    }

    private void toggleAddTaskSection() {
        isAddTaskExpanded = !isAddTaskExpanded;

        if (isAddTaskExpanded) {
            layoutAddTaskContent.setVisibility(View.VISIBLE);
            iconToggle.setImageResource(android.R.drawable.arrow_up_float);
        } else {
            layoutAddTaskContent.setVisibility(View.GONE);
            iconToggle.setImageResource(android.R.drawable.arrow_down_float);
            clearInputFields(); // Clear fields when collapsing
        }
    }

    private void toggleTasksDueNextWeekSection() {
        isTasksDueNextWeekExpanded = !isTasksDueNextWeekExpanded;

        if (isTasksDueNextWeekExpanded) {
            layoutTasksDueNextWeekContent.setVisibility(View.VISIBLE);
            iconTasksDueToggle.setImageResource(android.R.drawable.arrow_up_float);
        } else {
            layoutTasksDueNextWeekContent.setVisibility(View.GONE);
            iconTasksDueToggle.setImageResource(android.R.drawable.arrow_down_float);
        }
    }

    private void addTask() {
        String title = editTitle.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String topic = editTopic.getText().toString().trim();
        String deadline = editDeadline.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter task title", Toast.LENGTH_SHORT).show();
            return;
        }

        Task task = new Task(title, description, topic.isEmpty() ? null : topic);

        // Set deadline if provided
        if (!deadline.isEmpty()) {
            task.setDeadline(deadline);
        }

        long id = databaseHelper.addTask(task);

        if (id > 0) {
            task.setId((int) id);
            
            // Set reminder if enabled (for future enhancement, could add reminder UI to MainActivity)
            if (task.isReminderEnabled()) {
                reminderAlarmManager.setReminder(task);
            }
            
            Toast.makeText(this, "Task added successfully", Toast.LENGTH_SHORT).show();
            clearInputFields();
            toggleAddTaskSection(); // Collapse after adding
            resetPaginationAndLoad();
            setupTopicFilter();
            loadTasksDueNextWeek(); // Refresh the due next week list
        } else {
            Toast.makeText(this, "Failed to add task", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearInputFields() {
        editTitle.setText("");
        editDescription.setText("");
        editTopic.setText("");
        editDeadline.setText("");
    }

    private void loadInitialTasks() {
        currentPage = 0;
        hasMoreData = true;
        allTasks.clear();
        taskAdapter.updateTasks(allTasks);
        loadMoreTasks();
    }

    private void loadMoreTasks() {
        if (isLoading || !hasMoreData) return;

        isLoading = true;
        taskAdapter.setLoading(true);

        // Add debug logging to check pagination
        android.util.Log.d("MainActivity", "Loading page: " + currentPage + ", offset: " + (currentPage * PAGE_SIZE));

        // Reduced delay for better UX
        new android.os.Handler().postDelayed(() -> {
            List<Task> newTasks;
            int offset = currentPage * PAGE_SIZE;

            try {
                if (currentFilterTopic == null || "All Topics".equals(currentFilterTopic)) {
                    newTasks = databaseHelper.getAllTasks(PAGE_SIZE, offset);
                    android.util.Log.d("MainActivity", "Loaded " + newTasks.size() + " tasks without filter");
                } else {
                    newTasks = databaseHelper.getTasksByTopic(currentFilterTopic, PAGE_SIZE, offset);
                    android.util.Log.d("MainActivity", "Loaded " + newTasks.size() + " tasks with filter: " + currentFilterTopic);
                }

                // Check if we have fewer tasks than requested (means we reached the end)
                if (newTasks.size() < PAGE_SIZE) {
                    hasMoreData = false;
                    android.util.Log.d("MainActivity", "No more data available");
                }

                // Important: Only add new tasks, don't replace all
                if (currentPage == 0) {
                    // First page - replace all data
                    allTasks.clear();
                    allTasks.addAll(newTasks);
                    taskAdapter.updateTasks(allTasks);
                    android.util.Log.d("MainActivity", "First page loaded, total tasks: " + allTasks.size());
                } else {
                    // Subsequent pages - append new data
                    int oldSize = allTasks.size();
                    allTasks.addAll(newTasks);
                    taskAdapter.addTasks(newTasks);
                    android.util.Log.d("MainActivity", "Page " + currentPage + " loaded, total tasks: " + allTasks.size() + " (added " + newTasks.size() + ")");
                }

                currentPage++;

            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Error loading tasks", e);
                hasMoreData = false;
            } finally {
                isLoading = false;
                taskAdapter.setLoading(false);
            }

        }, 300); // Reduced to 300ms for faster loading
    }

    private void resetPaginationAndLoad() {
        currentPage = 0;
        hasMoreData = true;
        allTasks.clear();
        taskAdapter.updateTasks(allTasks);
        loadMoreTasks();
    }

    private void setupTopicFilter() {
        List<String> topics = databaseHelper.getAllTopics();
        topics.add(0, "All Topics");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, topics);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTopicFilter.setAdapter(adapter);

        // Set selection to "All Topics" first without triggering listener
        spinnerTopicFilter.setSelection(0, false);

        // Add listener after setting initial selection
        spinnerTopicFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedTopic = (String) parent.getItemAtPosition(position);
                // Only filter if it's not the initial setup or if it's actually different
                if (currentFilterTopic == null && position == 0) {
                    // Initial setup with "All Topics" - don't reload
                    return;
                }
                android.util.Log.d("MainActivity", "Filter changed to: " + selectedTopic);
                filterTasksByTopic(selectedTopic);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void filterTasksByTopic(String topic) {
        currentFilterTopic = topic;
        resetPaginationAndLoad();
    }

    private void clearFilter() {
        spinnerTopicFilter.setSelection(0);
        currentFilterTopic = null;
        resetPaginationAndLoad();
    }

    // TaskAdapter.OnTaskClickListener implementation
    @Override
    public void onTaskClick(Task task) {
        // Open TaskDetailActivity instead of editing directly
        Intent intent = new Intent(this, TaskDetailActivity.class);
        intent.putExtra("task_id", task.getId());
        intent.putExtra("task_title", task.getTitle());
        intent.putExtra("task_description", task.getDescription());
        intent.putExtra("task_topic", task.getTopic());
        intent.putExtra("task_date", task.getCreatedDate());
        intent.putExtra("task_completed", task.isCompleted());

        startActivityForResult(intent, 200); // REQUEST_CODE for TaskDetail
    }

    @Override
    public void onTaskDelete(Task task) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete \"" + task.getTitle() + "\"?")
            .setPositiveButton("Delete", (dialog, which) -> {
                try {
                    // Cancel reminder alarm before deleting
                    reminderAlarmManager.cancelReminder(task.getId());
                    
                    int result = databaseHelper.deleteTask(task.getId());
                    if (result > 0) {
                        taskAdapter.removeTask(task.getId());
                        Toast.makeText(this, "Task deleted successfully", Toast.LENGTH_SHORT).show();
                        setupTopicFilter(); // Refresh topic filter
                        loadTasksDueNextWeek(); // Refresh the due next week list
                    } else {
                        Toast.makeText(this, "Failed to delete task", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Error deleting task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    android.util.Log.e("MainActivity", "Error deleting task", e);
                }
            })
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }

    @Override
    public void onTaskStatusChange(Task task) {
        try {
            int result = databaseHelper.updateTask(task);
            if (result > 0) {
                String message = task.isCompleted() ? "Task marked as completed" : "Task marked as incomplete";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                taskAdapter.updateTask(task);
            } else {
                // Revert the change if update failed
                task.setCompleted(!task.isCompleted());
                taskAdapter.updateTask(task);
                Toast.makeText(this, "Failed to update task status", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            // Revert the change if update failed
            task.setCompleted(!task.isCompleted());
            taskAdapter.updateTask(task);
            Toast.makeText(this, "Error updating task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            android.util.Log.e("MainActivity", "Error updating task", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == 200) { // TaskDetailActivity result
                handleTaskDetailResult(data);
            }
        }
    }

    private void handleTaskDetailResult(Intent data) {
        if (data.getBooleanExtra("task_deleted", false)) {
            // Task was deleted in TaskDetailActivity
            int deletedTaskId = data.getIntExtra("deleted_task_id", -1);
            if (deletedTaskId != -1) {
                taskAdapter.removeTask(deletedTaskId);
                setupTopicFilter(); // Refresh topic filter
                loadTasksDueNextWeek(); // Refresh the due next week list
                Toast.makeText(this, "Task deleted successfully", Toast.LENGTH_SHORT).show();
            }
        } else if (data.getBooleanExtra("task_updated", false)) {
            // Task was updated in TaskDetailActivity
            int updatedTaskId = data.getIntExtra("updated_task_id", -1);
            if (updatedTaskId != -1) {
                // Create updated task object
                Task updatedTask = new Task();
                updatedTask.setId(updatedTaskId);
                updatedTask.setTitle(data.getStringExtra("updated_task_title"));
                updatedTask.setDescription(data.getStringExtra("updated_task_description"));
                updatedTask.setTopic(data.getStringExtra("updated_task_topic"));
                updatedTask.setCreatedDate(data.getStringExtra("updated_task_date"));
                updatedTask.setCompleted(data.getBooleanExtra("updated_task_completed", false));

                taskAdapter.updateTask(updatedTask);
                setupTopicFilter(); // Refresh topic filter in case topic changed
                loadTasksDueNextWeek(); // Refresh the due next week list
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Handle theme selection
        if (id == R.id.action_select_theme) {
            showThemeSelectionDialog();
            return true;
        }

        // Handle PIN settings
        if (id == R.id.action_change_pin) {
            showChangePinDialog();
            return true;
        }

        if (id == R.id.action_remove_pin) {
            showRemovePinDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showThemeSelectionDialog() {
        String[] themes = {"System Default", "Dark", "Light"};
        int checkedItem = ThemeManager.getInstance(this).getCurrentThemeIndex();

        new AlertDialog.Builder(this)
            .setTitle("Select Theme")
            .setSingleChoiceItems(themes, checkedItem, (dialog, which) -> {
                // Apply theme immediately
                ThemeManager.getInstance(this).applyThemeByIndex(which);
                recreate(); // Restart activity to apply theme
                dialog.dismiss();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void loadTasksDueNextWeek() {
        List<Task> tasksDueNextWeek = new ArrayList<>();
        for (Task task : allTasks) {
            if (task.getDeadline() != null && isWithinNextWeek(task.getDeadline())) {
                tasksDueNextWeek.add(task);
            }
        }
        tasksDueNextWeekAdapter.updateTasks(tasksDueNextWeek);
    }

    private boolean isWithinNextWeek(String deadline) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            java.util.Date deadlineDate = sdf.parse(deadline);
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            java.util.Date today = calendar.getTime();
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 7);
            java.util.Date nextWeek = calendar.getTime();
            return deadlineDate.after(today) && deadlineDate.before(nextWeek);
        } catch (java.text.ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void showDateTimePicker() {
        // Get current date and time for default values
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int year = calendar.get(java.util.Calendar.YEAR);
        int month = calendar.get(java.util.Calendar.MONTH);
        int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = calendar.get(java.util.Calendar.MINUTE);

        // Show Date Picker first
        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
            this,
            (view, selectedYear, selectedMonth, selectedDay) -> {
                // After date is selected, show Time Picker
                android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(
                    this,
                    (timeView, selectedHour, selectedMinute) -> {
                        // Format and set the selected date and time
                        String formattedDateTime = String.format(
                            java.util.Locale.getDefault(),
                            "%04d-%02d-%02d %02d:%02d",
                            selectedYear,
                            selectedMonth + 1, // Month is 0-based
                            selectedDay,
                            selectedHour,
                            selectedMinute
                        );
                        editDeadline.setText(formattedDateTime);
                    },
                    hour,
                    minute,
                    true // Use 24-hour format
                );
                timePickerDialog.setTitle("Chọn thời gian");
                timePickerDialog.show();
            },
            year,
            month,
            day
        );

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.setTitle("Chọn ngày");
        datePickerDialog.show();
    }

    private void showChangePinDialog() {
        Intent intent = new Intent(this, PinEntryActivity.class);
        intent.putExtra("setup_mode", true);
        startActivity(intent);
    }

    private void showRemovePinDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Remove PIN")
            .setMessage("Are you sure you want to remove PIN protection? This will make your app accessible without PIN verification.")
            .setPositiveButton("Remove", (dialog, which) -> {
                PinManager pinManager = new PinManager(this);
                pinManager.clearPin();
                Toast.makeText(this, "PIN protection removed", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }
}
