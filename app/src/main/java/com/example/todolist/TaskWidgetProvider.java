package com.example.todolist;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
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

            // Construct the RemoteViews object first with default content
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_task_list);

            try {
                // Get tasks due this week
                DatabaseHelper dbHelper = new DatabaseHelper(context);
                List<Task> allTasks = dbHelper.getAllTasks();

                // Filter tasks due this week
                StringBuilder taskText = new StringBuilder();
                int taskCount = 0;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat displaySdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
                Calendar calendar = Calendar.getInstance();
                Date today = calendar.getTime();
                calendar.add(Calendar.DAY_OF_YEAR, 7);
                Date nextWeek = calendar.getTime();

                for (Task task : allTasks) {
                    if (task.getDeadline() != null && !task.isCompleted()) {
                        try {
                            Date deadlineDate = sdf.parse(task.getDeadline().split(" ")[0]); // Get date part only
                            if (deadlineDate != null && deadlineDate.after(today) && deadlineDate.before(nextWeek)) {
                                taskCount++;
                                if (taskCount <= 5) { // Show first 5 tasks
                                    taskText.append("🔹 ").append(task.getTitle());
                                    if (task.getDeadline() != null) {
                                        taskText.append("\n   📅 ").append(displaySdf.format(deadlineDate));

                                        // Add urgency indicator
                                        long daysLeft = (deadlineDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24);
                                        if (daysLeft <= 1) {
                                            taskText.append(" ⚠️");
                                        } else if (daysLeft <= 3) {
                                            taskText.append(" ⏰");
                                        }
                                    }
                                    taskText.append("\n\n");
                                }
                            }
                        } catch (Exception e) {
                            android.util.Log.e("TaskWidget", "Error parsing task date: " + e.getMessage());
                        }
                    }
                }

                // Set update time
                SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                views.setTextViewText(R.id.widget_update_time, "Cập nhật: " + timeSdf.format(new Date()));

                // Set task list or empty message
                if (taskCount > 0) {
                    // Format tasks với emoji và layout đẹp hơn
                    StringBuilder formattedTasks = new StringBuilder();
                    int displayCount = 0;

                    for (Task task : allTasks) {
                        if (task.getDeadline() != null && !task.isCompleted()) {
                            try {
                                Date deadlineDate = sdf.parse(task.getDeadline().split(" ")[0]);
                                if (deadlineDate != null && deadlineDate.after(today) && deadlineDate.before(nextWeek)) {
                                    displayCount++;
                                    if (displayCount <= 3) { // Hiển thị tối đa 3 task
                                        formattedTasks.append("• ").append(task.getTitle());
                                        formattedTasks.append("\n  📅 ").append(displaySdf.format(deadlineDate));

                                        // Thêm indicator độ khẩn cấp
                                        long daysLeft = (deadlineDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24);
                                        if (daysLeft <= 1) {
                                            formattedTasks.append(" ⚠️ Gấp!");
                                        } else if (daysLeft <= 3) {
                                            formattedTasks.append(" ⏰ Sớm");
                                        }
                                        formattedTasks.append("\n\n");
                                    }
                                }
                            } catch (Exception e) {
                                android.util.Log.e("TaskWidget", "Error parsing task date: " + e.getMessage());
                            }
                        }
                    }

                    views.setTextViewText(R.id.widget_task_text, formattedTasks.toString().trim());
                    views.setViewVisibility(R.id.widget_task_text, android.view.View.VISIBLE);
                    views.setViewVisibility(R.id.widget_empty_container, android.view.View.GONE);
                    views.setViewVisibility(R.id.widget_empty_text, android.view.View.GONE);

                    // Hiển thị số lượng task
                    String countText = taskCount + " task" + (taskCount > 1 ? "s" : "");
                    if (taskCount > 3) {
                        countText += " (hiển thị 3)";
                    }
                    views.setTextViewText(R.id.widget_count, countText);
                    views.setViewVisibility(R.id.widget_count, android.view.View.VISIBLE);

                } else {
                    // Không có task
                    views.setViewVisibility(R.id.widget_task_text, android.view.View.GONE);
                    views.setViewVisibility(R.id.widget_empty_container, android.view.View.VISIBLE);
                    views.setViewVisibility(R.id.widget_count, android.view.View.GONE);
                    views.setViewVisibility(R.id.widget_empty_text, android.view.View.GONE);
                }

            } catch (Exception e) {
                android.util.Log.e("TaskWidget", "Error loading tasks: " + e.getMessage());
                // Show error state
                views.setTextViewText(R.id.widget_title, "Lỗi tải dữ liệu");
                views.setViewVisibility(R.id.widget_task_text, android.view.View.GONE);
                views.setViewVisibility(R.id.widget_empty_container, android.view.View.VISIBLE);
                views.setTextViewText(R.id.widget_empty_text, "Không thể tải tasks");
                views.setViewVisibility(R.id.widget_count, android.view.View.GONE);
            }

            // Set click listener to open the app
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);

        } catch (Exception e) {
            android.util.Log.e("TaskWidget", "Fatal error in updateAppWidget: " + e.getMessage());
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}
