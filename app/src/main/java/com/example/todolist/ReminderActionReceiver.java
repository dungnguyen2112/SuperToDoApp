package com.example.todolist;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import java.util.Calendar;

public class ReminderActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int taskId = intent.getIntExtra("task_id", -1);
        
        if (taskId == -1) return;
        
        NotificationManager notificationManager = (NotificationManager) 
            context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        switch (action) {
            case "MARK_COMPLETE":
                markTaskComplete(context, taskId);
                // Cancel the notification
                if (notificationManager != null) {
                    notificationManager.cancel(taskId);
                }
                break;
                
            case "SNOOZE":
                snoozeReminder(context, intent, taskId);
                // Cancel the current notification
                if (notificationManager != null) {
                    notificationManager.cancel(taskId);
                }
                break;
        }
    }
    
    private void markTaskComplete(Context context, int taskId) {
        try {
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            Task task = dbHelper.getTaskById(taskId);
            
            if (task != null) {
                task.setCompleted(true);
                int result = dbHelper.updateTask(task);
                
                if (result > 0) {
                    // Cancel all future reminders for this task
                    ReminderAlarmManager reminderManager = new ReminderAlarmManager(context);
                    reminderManager.cancelReminder(taskId);
                    
                    // Update widget immediately after task completion
                    TaskWidgetProvider.updateAllWidgets(context);
                    
                    Toast.makeText(context, "✅ Task completed: " + task.getTitle(), 
                        Toast.LENGTH_LONG).show();
                    android.util.Log.d("ReminderAction", "Task marked complete: " + task.getTitle());
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ReminderAction", "Error marking task complete", e);
        }
    }
    
    private void snoozeReminder(Context context, Intent intent, int taskId) {
        try {
            String taskTitle = intent.getStringExtra("task_title");
            String taskDescription = intent.getStringExtra("task_description");
            
            if (taskTitle != null) {
                // Create a snooze reminder for 1 hour later
                Calendar snoozeTime = Calendar.getInstance();
                snoozeTime.add(Calendar.HOUR, 1);
                
                Task snoozeTask = new Task(taskTitle, taskDescription, "REMINDER");
                snoozeTask.setId(taskId);
                snoozeTask.setReminderEnabled(true);
                snoozeTask.setCompleted(false);
                
                ReminderAlarmManager reminderManager = new ReminderAlarmManager(context);
                reminderManager.setTestReminder(snoozeTask, 
                    snoozeTime.get(Calendar.HOUR_OF_DAY), 
                    snoozeTime.get(Calendar.MINUTE));
                
                java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", 
                    java.util.Locale.getDefault());
                String snoozeTimeStr = timeFormat.format(snoozeTime.getTime());
                
                Toast.makeText(context, "⏰ Reminder snoozed until " + snoozeTimeStr, 
                    Toast.LENGTH_LONG).show();
                android.util.Log.d("ReminderAction", "Task snoozed until: " + snoozeTimeStr);
            }
        } catch (Exception e) {
            android.util.Log.e("ReminderAction", "Error snoozing reminder", e);
        }
    }
} 