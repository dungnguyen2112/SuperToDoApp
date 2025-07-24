package com.example.todolist;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "task_reminder_channel";
    private static final String CHANNEL_NAME = "Task Reminders";
    private static final String CHANNEL_DESCRIPTION = "Notifications for task reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        int taskId = intent.getIntExtra("task_id", -1);
        String taskTitle = intent.getStringExtra("task_title");
        String taskDescription = intent.getStringExtra("task_description");

        if (taskId == -1 || taskTitle == null) {
            return;
        }

        // Check if this is a test reminder
        String reminderType = intent.getStringExtra("reminder_type");
        boolean isTestReminder = "test_reminder".equals(reminderType);
        
        if (isTestReminder) {
            // For test reminders, always show notification
            createNotificationChannel(context);
            showNotification(context, taskId, taskTitle + " [TEST]", taskDescription, null);
            android.util.Log.d("ReminderReceiver", "Showing test reminder: " + taskTitle);
        } else {
            // Check if task is still incomplete before showing notification
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            Task task = dbHelper.getTaskById(taskId);
            
            if (task != null && !task.isCompleted() && task.isReminderEnabled()) {
                createNotificationChannel(context);
                showNotification(context, taskId, taskTitle, taskDescription, task);
            } else {
                android.util.Log.d("ReminderReceiver", "Skipping reminder - task completed or reminder disabled: " + taskTitle);
                
                // Cancel all future reminders for this completed task
                if (task != null && task.isCompleted()) {
                    ReminderAlarmManager reminderManager = new ReminderAlarmManager(context);
                    reminderManager.cancelReminder(taskId);
                }
            }
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 300, 200, 300});
            channel.enableLights(true);
            channel.setLightColor(0xFF2196F3); // Beautiful blue light
            channel.setShowBadge(true);
            
            // Set custom sound (optional - uses default for now)
            // channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification(Context context, int taskId, String title, String description, Task task) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        // Intent to open the app when notification is clicked
        Intent appIntent = new Intent(context, MainActivity.class);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            taskId,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create "Mark Complete" action intent
        Intent completeIntent = new Intent(context, ReminderActionReceiver.class);
        completeIntent.setAction("MARK_COMPLETE");
        completeIntent.putExtra("task_id", taskId);
        PendingIntent completePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId + 10000, // Different ID
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create "Snooze" action intent  
        Intent snoozeIntent = new Intent(context, ReminderActionReceiver.class);
        snoozeIntent.setAction("SNOOZE");
        snoozeIntent.putExtra("task_id", taskId);
        snoozeIntent.putExtra("task_title", title);
        snoozeIntent.putExtra("task_description", description);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId + 20000, // Different ID
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Get current time for subtitle
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        String currentTime = timeFormat.format(new java.util.Date());

        // Build deadline info
        String deadlineInfo = "";
        String urgencyEmoji = "⏰";
        int notificationColor = 0xFF2196F3; // Default blue
        
        if (task != null && task.getDeadline() != null && !task.getDeadline().isEmpty()) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                java.text.SimpleDateFormat displaySdf = new java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault());
                java.util.Date deadlineDate = sdf.parse(task.getDeadline());
                
                long timeDiff = deadlineDate.getTime() - System.currentTimeMillis();
                long daysDiff = timeDiff / (1000 * 60 * 60 * 24);
                
                if (daysDiff < 0) {
                    urgencyEmoji = "🚨";
                    deadlineInfo = " • Overdue!";
                    notificationColor = 0xFFE53935; // Red for overdue
                } else if (daysDiff == 0) {
                    urgencyEmoji = "⚠️";
                    deadlineInfo = " • Due today!";
                    notificationColor = 0xFFFF9800; // Orange for today
                } else if (daysDiff <= 3) {
                    urgencyEmoji = "⏳";
                    deadlineInfo = " • Due " + displaySdf.format(deadlineDate);
                    notificationColor = 0xFFFF9800; // Orange for soon
                } else {
                    deadlineInfo = " • Due " + displaySdf.format(deadlineDate);
                }
            } catch (Exception e) {
                android.util.Log.e("ReminderReceiver", "Error parsing deadline", e);
            }
        }

        // Build topic info
        String topicInfo = "";
        if (task != null && task.getTopic() != null && !task.getTopic().isEmpty()) {
            topicInfo = " | 🏷️ " + task.getTopic().toUpperCase();
        }

        // Build rich content
        String richDescription = description != null && !description.isEmpty() ? description : "Don't forget to complete this task!";
        String bigTextContent = urgencyEmoji + " " + richDescription;
        if (!deadlineInfo.isEmpty()) {
            bigTextContent += "\n📅" + deadlineInfo;
        }
        if (!topicInfo.isEmpty()) {
            bigTextContent += "\n" + topicInfo;
        }
        bigTextContent += "\n\n💡 Tap to open app or use quick actions below";

        // Build beautiful notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_task) // Use app's task icon
            .setContentTitle(urgencyEmoji + " " + title)
            .setContentText(richDescription + deadlineInfo)
            .setSubText("Reminder at " + currentTime + topicInfo)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(bigTextContent)
                .setBigContentTitle("📋 Task Reminder")
                .setSummaryText("Swipe for actions"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(notificationColor) // Dynamic color based on urgency
            .setColorized(true)
            .setVibrate(new long[]{0, 300, 200, 300})
            .setLights(notificationColor, 1000, 1000)
            .addAction(R.drawable.ic_lock, "✅ Complete", completePendingIntent) // Complete action
            .addAction(R.drawable.ic_lock, "⏰ Snooze 1h", snoozePendingIntent) // Snooze action
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setOnlyAlertOnce(false)
            .setNumber(1); // Show badge number

        // Show notification with unique ID based on task ID
        manager.notify(taskId, builder.build());
    }
}
