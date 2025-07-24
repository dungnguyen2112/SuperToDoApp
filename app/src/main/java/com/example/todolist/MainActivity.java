package com.example.todolist;

import android.content.Intent;
import android.os.Build;
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
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.sql.Time;
import java.time.LocalDateTime;
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
    private boolean initialLoadComplete = false;

    private ReminderService reminderService;
    private ReminderAlarmManager reminderAlarmManager;

    private RecyclerView recyclerTasksDueNextWeek;
    private TaskAdapter tasksDueNextWeekAdapter;

    @RequiresApi(api = Build.VERSION_CODES.O)
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
        
        // Setup test reminder - long press on toolbar
        findViewById(R.id.toolbar).setOnLongClickListener(v -> {
            testReminder();
            return true;
        });
        
        // Debug: Single tap on title to test filtering
        if (getSupportActionBar() != null) {
            findViewById(R.id.toolbar).setOnClickListener(v -> {
                debugTestFiltering();
            });
        }
        
        // Add a simple test: Long press Clear Filter button for instant test
        buttonClearFilter.setOnLongClickListener(v -> {
            android.util.Log.d("MainActivity", "Quick filter test triggered");
            List<String> topics = databaseHelper.getAllTopics();
            if (!topics.isEmpty()) {
                String firstTopic = topics.get(0);
                android.util.Log.d("MainActivity", "Quick test: Filtering by " + firstTopic);
                currentFilterTopic = firstTopic;
                applyFilterImmediately();
                return true;
            }
            Toast.makeText(this, "No topics available for quick test", Toast.LENGTH_SHORT).show();
            return true;
        });
        
        // Normal click clears filter, but also test widget update
        buttonClearFilter.setOnClickListener(v -> {
            clearFilter();
            // Also update widget when clearing filter
            TaskWidgetProvider.updateAllWidgets(this);
            Toast.makeText(this, "🔄 Filter cleared & widget updated!", Toast.LENGTH_SHORT).show();
        });
        
        // Triple tap to cycle widget themes
        buttonClearFilter.setOnLongClickListener(v -> {
            showWidgetThemeDialog();
            return true;
        });

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
        
        // Update widget when app starts
        TaskWidgetProvider.updateAllWidgets(this);
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
                
                // Only enable pagination scrolling when NO filter is active
                boolean isFiltering = (currentFilterTopic != null && !"All Topics".equals(currentFilterTopic));
                
                if (layoutManager != null && !isLoading && hasMoreData && !isFiltering) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    // Load more when user scrolls to near the end (within 2 items)
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2) {
                        android.util.Log.d("MainActivity", "Triggering load more - visible: " + visibleItemCount + ", total: " + totalItemCount + ", first: " + firstVisibleItemPosition);
                        loadMoreTasks();
                    }
                } else if (isFiltering) {
                    android.util.Log.d("MainActivity", "Scroll ignored - filtering active");
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
            
            // Update widget immediately when task is added
            android.util.Log.d("MainActivity", "Triggering widget update after adding task");
            TaskWidgetProvider.updateAllWidgets(this);
            clearInputFields();
            toggleAddTaskSection(); // Collapse after adding
            
            // Refresh the task list immediately after adding
            applyFilterImmediately();
            
            setupTopicFilter();
            loadTasksDueNextWeek(); // Refresh the due next week list
            
            // Update widgets
            TaskWidgetProvider.updateAllWidgets(this);
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
        android.util.Log.d("MainActivity", "Loading initial tasks");
        currentFilterTopic = null; // Reset filter on initial load
        applyFilterImmediately();
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

        }, 300); // Standard delay for pagination
    }
    
    private void forceReloadTaskList() {
        android.util.Log.d("MainActivity", "Force reloading task list");
        initialLoadComplete = false;
        currentPage = 0;
        hasMoreData = true;
        isLoading = false;
        allTasks.clear();
        taskAdapter.updateTasks(allTasks);
        taskAdapter.setLoading(true);
        loadFilteredTasksImmediately();
    }
    
    private void applyFilterImmediately() {
        android.util.Log.d("MainActivity", "Applying filter immediately: " + currentFilterTopic);
        
        // Stop any existing loading
        isLoading = false;
        
        // Get filtered results directly from database
        List<Task> filteredTasks;
        
        if (currentFilterTopic == null || "All Topics".equals(currentFilterTopic)) {
            android.util.Log.d("MainActivity", "Loading all tasks (no filter)");
            filteredTasks = databaseHelper.getAllTasks(PAGE_SIZE, 0);
            hasMoreData = filteredTasks.size() >= PAGE_SIZE;
            currentPage = 1;
        } else {
            android.util.Log.d("MainActivity", "Loading tasks with filter: " + currentFilterTopic);
            filteredTasks = databaseHelper.getTasksByTopic(currentFilterTopic);
            hasMoreData = false; // No pagination for filtered results
            currentPage = 0;
        }
        
        android.util.Log.d("MainActivity", "Found " + filteredTasks.size() + " tasks");
        
        // Update UI immediately on main thread
        runOnUiThread(() -> {
            android.util.Log.d("MainActivity", "Starting UI update...");
            
            // Clear existing data
            int oldSize = allTasks.size();
            allTasks.clear();
            android.util.Log.d("MainActivity", "Cleared " + oldSize + " old tasks");
            
            // Add filtered results
            allTasks.addAll(filteredTasks);
            android.util.Log.d("MainActivity", "Added " + filteredTasks.size() + " new tasks");
            
            // Force adapter update
            taskAdapter.updateTasks(allTasks);
            taskAdapter.notifyDataSetChanged();
            android.util.Log.d("MainActivity", "Adapter updated and notified");
            
            // Hide loading
            taskAdapter.setLoading(false);
            
            // Mark as complete
            initialLoadComplete = true;
            
            android.util.Log.d("MainActivity", "UI update completed with " + allTasks.size() + " tasks");
            
            // Show result message
            //String message = currentFilterTopic == null ?
            //    "✅ All tasks (" + allTasks.size() + ")" :
            //    "🔍 " + currentFilterTopic + " (" + allTasks.size() + " tasks)";
            //Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
        });
    }

    private boolean isFilteringActive() {
        return currentFilterTopic != null && !"All Topics".equals(currentFilterTopic);
    }

    private void setupTopicFilter() {
        List<String> topics = databaseHelper.getAllTopics();
        topics.add(0, "All Topics");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, topics);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTopicFilter.setAdapter(adapter);

        // Set selection to "All Topics" first without triggering listener
        spinnerTopicFilter.setSelection(0, false);

        // Add listener after a delay to ensure initial load is stable
        new android.os.Handler().postDelayed(() -> {
            spinnerTopicFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    String selectedTopic = (String) parent.getItemAtPosition(position);
                    
                                         // Don't filter if initial load is not complete yet
                     if (!initialLoadComplete) {
                         android.util.Log.d("MainActivity", "Ignoring filter change - initial load not complete");
                         return;
                     }
                     
                     // Check if this is actually a different filter
                     String newFilter = selectedTopic.equals("All Topics") ? null : selectedTopic;
                     if ((currentFilterTopic == null && newFilter == null) || 
                         (currentFilterTopic != null && currentFilterTopic.equals(newFilter))) {
                         android.util.Log.d("MainActivity", "Filter unchanged - skipping");
                         return;
                     }
                     
                     android.util.Log.d("MainActivity", "Filter changed from '" + currentFilterTopic + "' to '" + selectedTopic + "'");
                     filterTasksByTopic(selectedTopic);
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
                 }, 100); // Reduced delay to 100ms for better responsiveness
    }

    private void filterTasksByTopic(String topic) {
        currentFilterTopic = topic.equals("All Topics") ? null : topic;
        
        android.util.Log.d("MainActivity", "Filtering by topic: " + currentFilterTopic);
        
        // Use completely separate filtering logic
        applyFilterImmediately();
    }
    
    private void loadFilteredTasksImmediately() {
        if (isLoading) return;
        
        isLoading = true;
        android.util.Log.d("MainActivity", "Loading filtered tasks immediately - filter: " + currentFilterTopic);
        
        // Use immediate execution for filtering - no delay
        try {
            List<Task> newTasks;
            
            if (currentFilterTopic == null || "All Topics".equals(currentFilterTopic)) {
                // For "All Topics" or no filter, start with first page for pagination
                newTasks = databaseHelper.getAllTasks(PAGE_SIZE, 0);
                android.util.Log.d("MainActivity", "Loaded " + newTasks.size() + " tasks without filter (pagination mode)");
                
                // Check if we have fewer tasks than requested (means we reached the end)
                if (newTasks.size() < PAGE_SIZE) {
                    hasMoreData = false;
                } else {
                    hasMoreData = true; // More pages available
                }
                currentPage = 1; // Set to 1 since we loaded first page
            } else {
                // For topic filtering, load ALL matching results immediately (no pagination)
                newTasks = databaseHelper.getTasksByTopic(currentFilterTopic);
                android.util.Log.d("MainActivity", "Loaded ALL " + newTasks.size() + " tasks with filter: " + currentFilterTopic);
                
                // No more data needed since we loaded everything
                hasMoreData = false;
                currentPage = 0; // No pagination for filtered results
            }

            // Update UI with results on main thread
            runOnUiThread(() -> {
                allTasks.clear();
                allTasks.addAll(newTasks);
                taskAdapter.updateTasks(allTasks);
                taskAdapter.notifyDataSetChanged(); // Force adapter refresh
                
                android.util.Log.d("MainActivity", "UI updated with " + allTasks.size() + " tasks");
                
                // Mark initial load as complete
                initialLoadComplete = true;
                android.util.Log.d("MainActivity", "Load completed successfully");
            });

        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error loading tasks", e);
            hasMoreData = false;
            runOnUiThread(() -> {
                initialLoadComplete = true;
            });
        } finally {
            isLoading = false;
            runOnUiThread(() -> {
                taskAdapter.setLoading(false);
            });
        }
    }

    private void clearFilter() {
        android.util.Log.d("MainActivity", "Clearing filter");
        
        spinnerTopicFilter.setSelection(0);
        currentFilterTopic = null;
        
        // Apply filter immediately (will load all tasks)
        applyFilterImmediately();
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
                    
                    // Update widget immediately when task is deleted
                    TaskWidgetProvider.updateAllWidgets(this);
                        setupTopicFilter(); // Refresh topic filter
                        loadTasksDueNextWeek(); // Refresh the due next week list

                        // Update widget immediately after deleting task
                        TaskWidgetProvider.updateAllWidgets(this);
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
            
            // Update widget immediately when task completion status changes
            android.util.Log.d("MainActivity", "Triggering widget update after task status change");
            TaskWidgetProvider.updateAllWidgets(this);
            
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                taskAdapter.updateTask(task);
                
                // Handle reminders based on task completion status
                if (task.isCompleted()) {
                    // Cancel all reminders when task is completed
                    reminderAlarmManager.cancelReminder(task.getId());
                } else if (task.isReminderEnabled()) {
                    // Re-enable reminders if task is unmarked and has reminders enabled
                    reminderAlarmManager.setReminder(task);
                }

                // Update widget immediately after status change
                TaskWidgetProvider.updateAllWidgets(this);
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

                // Update widget immediately after task deletion from detail activity
                TaskWidgetProvider.updateAllWidgets(this);
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

                // Update widget immediately after task update from detail activity
                TaskWidgetProvider.updateAllWidgets(this);
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
        
        // Debug PIN status
        if (id == android.R.id.home) {
            PinManager pinManager = new PinManager(this);
            boolean isPinSet = pinManager.isPinSet();
            android.util.Log.d("MainActivity", "PIN Status Debug - PIN set: " + isPinSet);
            Toast.makeText(this, "PIN Status: " + (isPinSet ? "SET" : "NOT SET"), Toast.LENGTH_LONG).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void testReminder() {
        // Create a test task for reminder testing
        Task testTask = new Task("Test Reminder Task", "This is a test reminder to check if notifications work", "TEST");
        testTask.setId(99999); // Use a high ID to avoid conflicts
        testTask.setReminderEnabled(true);
        testTask.setCompleted(false);

        LocalDateTime now1 = LocalDateTime.now();
        int hour1 = now1.getHour();
        int minute1 = now1.getMinute();
        
        // Show dialog to choose test time
        int finalHour = hour1;
        int finalMinute = minute1;
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Test Reminder")
            .setMessage("Choose when to test the reminder:")
            .setPositiveButton("Test", (dialog, which) -> {
                reminderAlarmManager.setTestReminder(testTask, finalHour, finalMinute);
                Toast.makeText(this, "Test reminder set for now! Check your notifications.", Toast.LENGTH_LONG).show();
            })
            .setNegativeButton("In 1 minute", (dialog, which) -> {
                java.util.Calendar now = java.util.Calendar.getInstance();
                now.add(java.util.Calendar.MINUTE, 1);
                int hour = now.get(java.util.Calendar.HOUR_OF_DAY);
                int minute = now.get(java.util.Calendar.MINUTE);
                
                reminderAlarmManager.setTestReminder(testTask, hour, minute);
                Toast.makeText(this, "Test reminder set for " + hour + ":" + String.format("%02d", minute) + "! Check your notifications.", Toast.LENGTH_LONG).show();
            })
            .setNeutralButton("Cancel", null)
            .show();
        
        android.util.Log.d("MainActivity", "Test reminder dialog shown");
    }
    
    private void debugTestFiltering() {
        android.util.Log.d("MainActivity", "Debug: Testing filtering");
        
        // Get all available topics for testing
        List<String> topics = databaseHelper.getAllTopics();
        
        if (topics.isEmpty()) {
            Toast.makeText(this, "No topics available for filtering test", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create test dialog with available topics
        String[] topicArray = topics.toArray(new String[0]);
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Debug: Test Filtering")
            .setItems(topicArray, (dialog, which) -> {
                String selectedTopic = topicArray[which];
                android.util.Log.d("MainActivity", "Debug: Manually filtering by: " + selectedTopic);
                
                // Force filter manually
                currentFilterTopic = selectedTopic;
                applyFilterImmediately();
            })
            .setNegativeButton("Clear Filter", (dialog, which) -> {
                android.util.Log.d("MainActivity", "Debug: Clearing filter");
                currentFilterTopic = null;
                applyFilterImmediately();
            })
            .show();
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
        
        // Load ALL tasks from database, not just the ones currently loaded in pagination
        List<Task> allTasksFromDb = databaseHelper.getAllTasks();
        
        for (Task task : allTasksFromDb) {
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
            
            // Get today at start of day (00:00:00)
            java.util.Calendar todayCalendar = java.util.Calendar.getInstance();
            todayCalendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
            todayCalendar.set(java.util.Calendar.MINUTE, 0);
            todayCalendar.set(java.util.Calendar.SECOND, 0);
            todayCalendar.set(java.util.Calendar.MILLISECOND, 0);
            java.util.Date todayStart = todayCalendar.getTime();
            
            // Get next week at end of day (23:59:59)
            java.util.Calendar nextWeekCalendar = java.util.Calendar.getInstance();
            nextWeekCalendar.add(java.util.Calendar.DAY_OF_YEAR, 7);
            nextWeekCalendar.set(java.util.Calendar.HOUR_OF_DAY, 23);
            nextWeekCalendar.set(java.util.Calendar.MINUTE, 59);
            nextWeekCalendar.set(java.util.Calendar.SECOND, 59);
            nextWeekCalendar.set(java.util.Calendar.MILLISECOND, 999);
            java.util.Date nextWeekEnd = nextWeekCalendar.getTime();
            
            // Include tasks from today to 7 days from now (inclusive)
            return !deadlineDate.before(todayStart) && !deadlineDate.after(nextWeekEnd);
        } catch (java.text.ParseException e) {
            android.util.Log.e("MainActivity", "Error parsing deadline date: " + deadline, e);
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

    private void showWidgetThemeDialog() {
        String[] themes = {"🌙 Dark Premium", "🌊 Ocean Blue", "💜 Purple Gradient"};
        new AlertDialog.Builder(this)
            .setTitle("🎨 Choose Widget Theme")
            .setItems(themes, (dialog, which) -> {
                String selectedTheme = themes[which];
                // Save theme preference and update widget layout
                android.content.SharedPreferences prefs = getSharedPreferences("widget_prefs", MODE_PRIVATE);
                prefs.edit().putInt("theme", which).apply();
                
                // Update all widgets with new theme
                TaskWidgetProvider.updateAllWidgets(this);
                Toast.makeText(this, "🎨 Applied: " + selectedTheme, Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Update widget when app comes to foreground  
        TaskWidgetProvider.updateAllWidgets(this);
    }
}
