package com.example.todolist;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_UPDATE_WIDGET = "com.example.todolist.UPDATE_WIDGET";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_UPDATE_WIDGET.equals(intent.getAction())) {
            // Update all widgets when requested
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName componentName = new ComponentName(context, TaskWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        try {
            // Create an Intent to launch MainActivity
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Construct the RemoteViews object
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_task_list);
            
            // Apply selected theme
            applyWidgetTheme(context, views);

            try {
                // Get tasks due this week - widget will resize dynamically based on content
                DatabaseHelper dbHelper = new DatabaseHelper(context);
                List<Task> allTasks = dbHelper.getAllTasks();
                android.util.Log.d("TaskWidget", "Loaded " + allTasks.size() + " total tasks from database");

                // Filter tasks due this week
                int taskCount = 0;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat displaySdf = new SimpleDateFormat("dd/MM", Locale.getDefault());

                // Use improved logic for date range
                Calendar todayCalendar = Calendar.getInstance();
                todayCalendar.set(Calendar.HOUR_OF_DAY, 0);
                todayCalendar.set(Calendar.MINUTE, 0);
                todayCalendar.set(Calendar.SECOND, 0);
                todayCalendar.set(Calendar.MILLISECOND, 0);
                Date todayStart = todayCalendar.getTime();

                Calendar nextWeekCalendar = Calendar.getInstance();
                nextWeekCalendar.add(Calendar.DAY_OF_YEAR, 7);
                nextWeekCalendar.set(Calendar.HOUR_OF_DAY, 23);
                nextWeekCalendar.set(Calendar.MINUTE, 59);
                nextWeekCalendar.set(Calendar.SECOND, 59);
                nextWeekCalendar.set(Calendar.MILLISECOND, 999);
                Date nextWeekEnd = nextWeekCalendar.getTime();

                StringBuilder taskText = new StringBuilder();
                android.util.Log.d("TaskWidget", "=== WIDGET UPDATE START ===");
                android.util.Log.d("TaskWidget", "Date range: " + sdf.format(todayStart) + " to " + sdf.format(nextWeekEnd));

                for (Task task : allTasks) {
                    if (task.getDeadline() != null && !task.isCompleted()) {
                        try {
                            String deadlineStr = task.getDeadline();
                            // Handle both date-only and date-time formats
                            String dateOnlyStr = deadlineStr.contains(" ") ? deadlineStr.split(" ")[0] : deadlineStr;
                            Date deadlineDate = sdf.parse(dateOnlyStr);

                            if (deadlineDate != null && !deadlineDate.before(todayStart) && !deadlineDate.after(nextWeekEnd)) {
                                taskCount++;
                                android.util.Log.d("TaskWidget", "Task " + taskCount + ": " + task.getTitle() + " - " + dateOnlyStr);
                                
                                // Show only first 2 tasks in widget for cleaner look
                                if (taskCount <= 2) {
                                    // Balanced widget format: clean and proportional
                                    String taskTitle = task.getTitle();
                                    if (taskTitle.length() > 28) {
                                        taskTitle = taskTitle.substring(0, 28) + "...";
                                    }
                                    
                                    // Clean format with icon and status
                                    taskText.append("📌 ").append(taskTitle);
                                    
                                    // Add urgency indicator
                                    long daysLeft = (deadlineDate.getTime() - todayStart.getTime()) / (1000 * 60 * 60 * 24);
                                    if (daysLeft <= 0) {
                                        taskText.append(" 🔴");
                                    } else if (daysLeft <= 1) {
                                        taskText.append(" 🟡");
                                    } else if (daysLeft <= 3) {
                                        taskText.append(" 🟠");
                                    }
                                    
                                    // Date on same line with proper spacing
                                    taskText.append("   📅 ").append(displaySdf.format(deadlineDate));
                                    
                                    // Add proper spacing between tasks
                                    if (taskCount < 2) {
                                        taskText.append("\n\n");
                                    } else {
                                        taskText.append("\n");
                                    }
                                    
                                    android.util.Log.d("TaskWidget", "Added task to display: " + task.getTitle());
                                }
                            } else {
                                android.util.Log.d("TaskWidget", "Task filtered out - not in date range: " + task.getTitle() + " - " + dateOnlyStr);
                            }
                        } catch (Exception e) {
                            android.util.Log.e("TaskWidget", "Error parsing task date: " + e.getMessage());
                        }
                    } else {
                        if (task.isCompleted()) {
                            android.util.Log.d("TaskWidget", "Task filtered out - completed: " + task.getTitle());
                        } else {
                            android.util.Log.d("TaskWidget", "Task filtered out - no deadline: " + task.getTitle());
                        }
                    }
                }

                // Set update time
                SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                views.setTextViewText(R.id.widget_update_time, "Cập nhật: " + timeSdf.format(new Date()));

                // Update widget content with better formatting
                android.util.Log.d("TaskWidget", "Updating widget content - taskCount: " + taskCount);
                if (taskCount > 0) {
                    // Set title
                    views.setTextViewText(R.id.widget_title, "Tuần này");
                    views.setTextViewText(R.id.widget_subtitle, "Tasks sắp đến hạn");
                    android.util.Log.d("TaskWidget", "Set title");

                    if (taskText.length() > 0) {
                        String taskListText = taskText.toString().trim();
                        views.setTextViewText(R.id.widget_task_text, taskListText);
                        views.setViewVisibility(R.id.widget_task_text, View.VISIBLE);
                        views.setViewVisibility(R.id.widget_empty_container, View.GONE);
                        android.util.Log.d("TaskWidget", "Set task list with " + taskListText.length() + " characters, " + Math.min(taskCount, 2) + " tasks displayed");
                    } else {
                        views.setTextViewText(R.id.widget_task_text, "Đang tải tasks...");
                        views.setViewVisibility(R.id.widget_task_text, View.VISIBLE);
                        views.setViewVisibility(R.id.widget_empty_container, View.GONE);
                        android.util.Log.d("TaskWidget", "Set task list to loading message");
                    }

                    // Show task count
                    String countText = String.valueOf(taskCount);
                    views.setTextViewText(R.id.widget_count, countText);
                    views.setViewVisibility(R.id.widget_count, View.VISIBLE);
                    
                    // Add "View more" section if there are more than 2 tasks
                    android.util.Log.d("TaskWidget", "Checking view more: taskCount = " + taskCount);
                    if (taskCount > 2) {
                        int remainingTasks = taskCount - 2;
                        String viewMoreText = "Xem thêm " + remainingTasks + " task khác";
                        views.setTextViewText(R.id.widget_view_more_text, viewMoreText);
                        views.setViewVisibility(R.id.widget_view_more, View.VISIBLE);
                        android.util.Log.d("TaskWidget", "Showing view more: " + viewMoreText);
                    } else {
                        views.setViewVisibility(R.id.widget_view_more, View.GONE);
                        android.util.Log.d("TaskWidget", "Hiding view more - only " + taskCount + " tasks");
                    }
                } else {
                    views.setTextViewText(R.id.widget_title, "Tuần này");
                    views.setTextViewText(R.id.widget_subtitle, "Tasks sắp đến hạn");
                    views.setViewVisibility(R.id.widget_task_text, View.GONE);
                    views.setViewVisibility(R.id.widget_view_more, View.GONE);
                    views.setViewVisibility(R.id.widget_empty_container, View.VISIBLE);
                    views.setTextViewText(R.id.widget_empty_text, "Không có task nào sắp đến hạn");
                    views.setViewVisibility(R.id.widget_count, View.GONE);
                    android.util.Log.d("TaskWidget", "Set empty state content");
                }

            } catch (Exception e) {
                android.util.Log.e("TaskWidget", "Error loading tasks", e);
                views.setTextViewText(R.id.widget_title, "Tasks sắp đến hạn");
                views.setTextViewText(R.id.widget_task_text, "❌ Lỗi khi tải dữ liệu\n\nTap để thử lại");
                views.setViewVisibility(R.id.widget_task_text, View.VISIBLE);
                views.setViewVisibility(R.id.widget_empty_container, View.GONE);
                views.setViewVisibility(R.id.widget_count, View.GONE);
            }

            // Set click listener to open app and refresh widget
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);
            
            // Add refresh intent for widget title (double tap to refresh)
            Intent refreshIntent = new Intent(context, TaskWidgetProvider.class);
            refreshIntent.setAction(ACTION_UPDATE_WIDGET);
            PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(
                context, 
                0, 
                refreshIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            // Set refresh action on title tap
            views.setOnClickPendingIntent(R.id.widget_title, refreshPendingIntent);

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views);

        } catch (Exception e) {
            android.util.Log.e("TaskWidget", "Error updating widget", e);
        }
    }

    // Static method to update all widgets from anywhere in the app
    public static void updateAllWidgets(Context context) {
        try {
            // Method 1: Broadcast approach
            Intent intent = new Intent(context, TaskWidgetProvider.class);
            intent.setAction(ACTION_UPDATE_WIDGET);
            context.sendBroadcast(intent);
            android.util.Log.d("TaskWidget", "Widget update broadcast sent");
            
            // Method 2: Direct update approach (more reliable)
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName componentName = new ComponentName(context, TaskWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
            
            if (appWidgetIds.length > 0) {
                android.util.Log.d("TaskWidget", "Found " + appWidgetIds.length + " widgets to update");
                for (int appWidgetId : appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId);
                }
                android.util.Log.d("TaskWidget", "Direct widget update completed for " + appWidgetIds.length + " widgets");
            } else {
                android.util.Log.d("TaskWidget", "No widgets found to update");
            }
        } catch (Exception e) {
            android.util.Log.e("TaskWidget", "Error updating widgets", e);
        }
    }

    private static void applyWidgetTheme(Context context, RemoteViews views) {
        try {
            android.content.SharedPreferences prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE);
            int theme = prefs.getInt("theme", 0); // Default: Dark Premium
            
            // Apply different background based on theme
            switch (theme) {
                case 1: // Ocean Blue
                    views.setInt(R.id.widget_container, "setBackgroundResource", R.drawable.widget_background_ocean);
                    break;
                case 2: // Purple Gradient
                    views.setInt(R.id.widget_container, "setBackgroundResource", R.drawable.widget_background_purple);
                    break;
                default: // Dark Premium
                    views.setInt(R.id.widget_container, "setBackgroundResource", R.drawable.widget_background_premium);
                    break;
            }
        } catch (Exception e) {
            android.util.Log.e("TaskWidget", "Error applying theme", e);
        }
    }
}
