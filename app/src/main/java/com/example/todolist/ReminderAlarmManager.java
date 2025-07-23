package com.example.todolist;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ReminderAlarmManager {
    private static final String TAG = "ReminderAlarmManager";
    private Context context;
    private AlarmManager alarmManager;

    public ReminderAlarmManager(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public void setReminder(Task task) {
        if (!task.isReminderEnabled() || task.getReminderTime() == null) {
            Log.d(TAG, "Reminder not enabled or time not set for task: " + task.getId());
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Calendar reminderTime = Calendar.getInstance();
            reminderTime.setTime(sdf.parse(task.getReminderTime()));

            // Check if reminder time is in the future
            Calendar now = Calendar.getInstance();
            if (reminderTime.before(now)) {
                Log.d(TAG, "Reminder time is in the past, skipping: " + task.getReminderTime());
                return;
            }

            Intent intent = new Intent(context, ReminderReceiver.class);
            intent.putExtra("task_id", task.getId());
            intent.putExtra("task_title", task.getTitle());
            intent.putExtra("task_description", task.getDescription());

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                task.getId(), // Use task ID as request code to make it unique
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            long triggerTime = reminderTime.getTimeInMillis();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }

            Log.d(TAG, "Reminder set for task " + task.getId() + " at " + task.getReminderTime());
        } catch (Exception e) {
            Log.e(TAG, "Error setting reminder for task " + task.getId(), e);
        }
    }

    public void cancelReminder(int taskId) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "Reminder cancelled for task: " + taskId);
    }

    public void updateReminder(Task task) {
        // Cancel existing reminder
        cancelReminder(task.getId());
        
        // Set new reminder if enabled
        if (task.isReminderEnabled()) {
            setReminder(task);
        }
    }
} 