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
        if (!task.isReminderEnabled() || task.getDeadline() == null || task.isCompleted()) {
            Log.d(TAG, "Reminder not enabled, no deadline, or task completed for task: " + task.getId());
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar deadlineCalendar = Calendar.getInstance();
            deadlineCalendar.setTime(sdf.parse(task.getDeadline()));
            
            // Set deadline to end of day (23:59:59)
            deadlineCalendar.set(Calendar.HOUR_OF_DAY, 23);
            deadlineCalendar.set(Calendar.MINUTE, 59);
            deadlineCalendar.set(Calendar.SECOND, 59);

            Calendar now = Calendar.getInstance();
            
            // Calculate one week before deadline
            Calendar oneWeekBefore = (Calendar) deadlineCalendar.clone();
            oneWeekBefore.add(Calendar.DAY_OF_YEAR, -7);

            // Daily reminder times: 9:00 AM, 2:00 PM, 6:00 PM
            int[] reminderHours = {9, 14, 18};
            
            // Cancel any existing reminders first
            cancelReminder(task.getId());
            
            int alarmId = task.getId() * 100; // Base ID for this task
            int alarmsSet = 0;

            // Set reminders for each day in the week before deadline
            for (Calendar currentDay = (Calendar) oneWeekBefore.clone(); 
                 !currentDay.after(deadlineCalendar); 
                 currentDay.add(Calendar.DAY_OF_YEAR, 1)) {
                
                // Set 3 reminders per day (9 AM, 2 PM, 6 PM)
                for (int hour : reminderHours) {
                    Calendar reminderTime = (Calendar) currentDay.clone();
                    reminderTime.set(Calendar.HOUR_OF_DAY, hour);
                    reminderTime.set(Calendar.MINUTE, 43);
                    reminderTime.set(Calendar.SECOND, 0);
                    reminderTime.set(Calendar.MILLISECOND, 0);

                    // Only set reminder if it's in the future
                    if (reminderTime.after(now)) {
                        Intent intent = new Intent(context, ReminderReceiver.class);
                        intent.putExtra("task_id", task.getId());
                        intent.putExtra("task_title", task.getTitle());
                        intent.putExtra("task_description", task.getDescription());
                        intent.putExtra("reminder_type", "daily_reminder");

                        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                            context,
                            alarmId++, // Unique ID for each reminder
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        );

                        long triggerTime = reminderTime.getTimeInMillis();

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                        } else {
                            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                        }
                        
                        alarmsSet++;
                    }
                }
            }

            Log.d(TAG, "Set " + alarmsSet + " daily reminders for task " + task.getId() + " until deadline: " + task.getDeadline());
        } catch (Exception e) {
            Log.e(TAG, "Error setting reminders for task " + task.getId(), e);
        }
    }

    public void cancelReminder(int taskId) {
        // Cancel all possible reminders for this task
        // We use up to 21 reminders per task (7 days * 3 times per day = 21 max reminders)
        int baseAlarmId = taskId * 100;
        
        for (int i = 0; i < 25; i++) { // Extra buffer to ensure all are cancelled
            Intent intent = new Intent(context, ReminderReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                baseAlarmId + i,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
            }
        }
        
        Log.d(TAG, "All reminders cancelled for task: " + taskId);
    }

    public void updateReminder(Task task) {
        // Cancel existing reminder
        cancelReminder(task.getId());
        
        // Set new reminder if enabled
        if (task.isReminderEnabled()) {
            setReminder(task);
        }
    }
    
    // Test method - set immediate reminder for testing
    public void setTestReminder(Task task, int hour, int minute) {
        Log.d(TAG, "Setting test reminder for task " + task.getId() + " at " + hour + ":" + minute);
        
        try {
            Calendar testTime = Calendar.getInstance();
            testTime.set(Calendar.HOUR_OF_DAY, hour);
            testTime.set(Calendar.MINUTE, minute);
            testTime.set(Calendar.SECOND, 0);
            testTime.set(Calendar.MILLISECOND, 0);
            
            // If the time has already passed today, set for tomorrow
            Calendar now = Calendar.getInstance();
//            if (testTime.before(now)) {
//                testTime.add(Calendar.DAY_OF_YEAR, 1);
//            }

            Intent intent = new Intent(context, ReminderReceiver.class);
            intent.putExtra("task_id", task.getId());
            intent.putExtra("task_title", task.getTitle());
            intent.putExtra("task_description", task.getDescription());
            intent.putExtra("reminder_type", "test_reminder");

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                task.getId() * 1000, // Different ID for test reminders
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            long triggerTime = testTime.getTimeInMillis();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Log.d(TAG, "Test reminder set for " + sdf.format(testTime.getTime()));
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting test reminder", e);
        }
    }
} 