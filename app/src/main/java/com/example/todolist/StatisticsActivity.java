package com.example.todolist;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private TextView totalTasksText, completedTasksText, avgCompletionTimeText, onTimeCompletionText;
    private LinearLayout weeklyChartContainer, monthlyChartContainer;
    private ProgressBar completionRateProgress;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.getInstance(this).initializeTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        setupToolbar();
        initViews();
        setupDatabase();
        loadStatistics();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Thống kê hiệu suất");
        }
    }

    private void initViews() {
        totalTasksText = findViewById(R.id.text_total_tasks);
        completedTasksText = findViewById(R.id.text_completed_tasks);
        avgCompletionTimeText = findViewById(R.id.text_avg_completion_time);
        onTimeCompletionText = findViewById(R.id.text_ontime_completion);
        weeklyChartContainer = findViewById(R.id.weekly_chart_container);
        monthlyChartContainer = findViewById(R.id.monthly_chart_container);
        completionRateProgress = findViewById(R.id.completion_rate_progress);
    }

    private void setupDatabase() {
        databaseHelper = new DatabaseHelper(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void loadStatistics() {
        loadBasicStats();
        loadWeeklyChart();
        loadMonthlyChart();
        loadCompletionRateChart();
    }

    private void loadBasicStats() {
        List<Task> allTasks = databaseHelper.getAllTasks();
        List<Task> completedTasks = databaseHelper.getCompletedTasks();
        
        int totalTasks = allTasks.size();
        int completedCount = completedTasks.size();
        
        totalTasksText.setText("Tổng số task: " + totalTasks);
        completedTasksText.setText("Task đã hoàn thành: " + completedCount);
        
        // Tính thời gian trung bình hoàn thành
        double avgDays = calculateAverageCompletionTime(completedTasks);
        avgCompletionTimeText.setText("Thời gian trung bình: " + String.format("%.1f ngày", avgDays));
        
        // Tính tỷ lệ hoàn thành đúng hạn
        double onTimeRate = calculateOnTimeCompletionRate(completedTasks);
        onTimeCompletionText.setText("Hoàn thành đúng hạn: " + String.format("%.1f%%", onTimeRate));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void loadWeeklyChart() {
        Map<String, Integer> weeklyData = getWeeklyCompletionData();
        
        weeklyChartContainer.removeAllViews();
        
        int maxValue = 1;
        for (int value : weeklyData.values()) {
            if (value > maxValue) {
                maxValue = value;
            }
        }
        
        for (Map.Entry<String, Integer> entry : weeklyData.entrySet()) {
            // Tạo view cho mỗi tuần
            LinearLayout weekItem = new LinearLayout(this);
            weekItem.setOrientation(LinearLayout.HORIZONTAL);
            weekItem.setPadding(0, 8, 0, 8);
            
            // Label tuần
            TextView weekLabel = new TextView(this);
            weekLabel.setText(entry.getKey());
            weekLabel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            weekLabel.setTextColor(getResources().getColor(R.color.text_primary));
            
            // Progress bar
            ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progressBar.setMax(maxValue);
            progressBar.setProgress(entry.getValue());
            progressBar.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));
            
            // Giá trị
            TextView valueText = new TextView(this);
            valueText.setText(String.valueOf(entry.getValue()));
            valueText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            valueText.setPadding(16, 0, 0, 0);
            valueText.setTextColor(getResources().getColor(R.color.text_primary));
            
            weekItem.addView(weekLabel);
            weekItem.addView(progressBar);
            weekItem.addView(valueText);
            
            weeklyChartContainer.addView(weekItem);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void loadMonthlyChart() {
        Map<String, Integer> monthlyData = getMonthlyCompletionData();
        
        monthlyChartContainer.removeAllViews();
        
        int maxValue = 1;
        for (int value : monthlyData.values()) {
            if (value > maxValue) {
                maxValue = value;
            }
        }
        
        for (Map.Entry<String, Integer> entry : monthlyData.entrySet()) {
            // Tạo view cho mỗi tháng
            LinearLayout monthItem = new LinearLayout(this);
            monthItem.setOrientation(LinearLayout.HORIZONTAL);
            monthItem.setPadding(0, 8, 0, 8);
            
            // Label tháng
            TextView monthLabel = new TextView(this);
            monthLabel.setText(entry.getKey());
            monthLabel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            monthLabel.setTextColor(getResources().getColor(R.color.text_primary));
            
            // Progress bar
            ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progressBar.setMax(maxValue);
            progressBar.setProgress(entry.getValue());
            progressBar.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));
            
            // Giá trị
            TextView valueText = new TextView(this);
            valueText.setText(String.valueOf(entry.getValue()));
            valueText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            valueText.setPadding(16, 0, 0, 0);
            valueText.setTextColor(getResources().getColor(R.color.text_primary));
            
            monthItem.addView(monthLabel);
            monthItem.addView(progressBar);
            monthItem.addView(valueText);
            
            monthlyChartContainer.addView(monthItem);
        }
    }

    private void loadCompletionRateChart() {
        List<Task> allTasks = databaseHelper.getAllTasks();
        List<Task> completedTasks = databaseHelper.getCompletedTasks();
        
        int completed = completedTasks.size();
        int total = allTasks.size();
        
        if (total > 0) {
            int completionPercentage = (completed * 100) / total;
            completionRateProgress.setMax(100);
            completionRateProgress.setProgress(completionPercentage);
            
            // Thêm text hiển thị phần trăm
            TextView completionText = findViewById(R.id.completion_rate_text);
            if (completionText != null) {
                completionText.setText(completionPercentage + "% (" + completed + "/" + total + ")");
            }
        } else {
            completionRateProgress.setProgress(0);
        }
    }

    private double calculateAverageCompletionTime(List<Task> completedTasks) {
        if (completedTasks.isEmpty()) return 0;
        
        long totalDays = 0;
        int validTasks = 0;
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.ENGLISH);
        
        for (Task task : completedTasks) {
            try {
                Date createdDate = dateFormat.parse(task.getCreatedDate());
                Date completedDate = task.getCompletedDate() != null ? 
                    dateFormat.parse(task.getCompletedDate()) : new Date();
                
                if (createdDate != null && completedDate != null) {
                    long diffInMillies = Math.abs(completedDate.getTime() - createdDate.getTime());
                    long diffInDays = diffInMillies / (1000 * 60 * 60 * 24);
                    totalDays += diffInDays;
                    validTasks++;
                }
            } catch (ParseException e) {
                // Bỏ qua task có format ngày không hợp lệ
            }
        }
        
        return validTasks > 0 ? (double) totalDays / validTasks : 0;
    }

    private double calculateOnTimeCompletionRate(List<Task> completedTasks) {
        if (completedTasks.isEmpty()) return 0;
        
        int onTimeTasks = 0;
        int tasksWithDeadline = 0;
        
        SimpleDateFormat deadlineFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat completedDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.ENGLISH);
        
        for (Task task : completedTasks) {
            if (task.getDeadline() != null && !task.getDeadline().isEmpty() 
                && task.getCompletedDate() != null && !task.getCompletedDate().isEmpty()) {
                tasksWithDeadline++;
                try {
                    Date deadline = deadlineFormat.parse(task.getDeadline());
                    Date completedDate = completedDateFormat.parse(task.getCompletedDate());
                    
                    if (deadline != null && completedDate != null) {
                        // Chỉ so sánh ngày, bỏ qua giờ
                        Calendar deadlineCal = Calendar.getInstance();
                        deadlineCal.setTime(deadline);
                        deadlineCal.set(Calendar.HOUR_OF_DAY, 23);
                        deadlineCal.set(Calendar.MINUTE, 59);
                        deadlineCal.set(Calendar.SECOND, 59);
                        
                        Calendar completedCal = Calendar.getInstance();
                        completedCal.setTime(completedDate);
                        
                        // Hoàn thành trước hoặc trong ngày deadline đều tính là đúng hạn
                        if (!completedCal.getTime().after(deadlineCal.getTime())) {
                            onTimeTasks++;
                        }
                    }
                } catch (ParseException e) {
                    // Bỏ qua task có format ngày không hợp lệ
                    System.out.println("ParseException: " + e.getMessage() + " for task deadline: " + task.getDeadline() + " completed: " + task.getCompletedDate());
                }
            }
        }
        
        return tasksWithDeadline > 0 ? (double) onTimeTasks / tasksWithDeadline * 100 : 0;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private Map<String, Integer> getWeeklyCompletionData() {
        Map<String, Integer> weeklyData = new HashMap<>();
        List<Task> completedTasks = databaseHelper.getCompletedTasks();
        
        LocalDate now = LocalDate.now();
        
        // Tạo 4 tuần gần nhất
        for (int i = 3; i >= 0; i--) {
            LocalDate weekStart = now.minusWeeks(i);
            String weekLabel = "Tuần " + weekStart.format(DateTimeFormatter.ofPattern("dd/MM"));
            weeklyData.put(weekLabel, 0);
        }
        
        // Đếm số task hoàn thành trong mỗi tuần
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.ENGLISH);
        
        for (Task task : completedTasks) {
            try {
                String dateToUse = task.getCompletedDate() != null ? task.getCompletedDate() : task.getCreatedDate();
                Date completedDate = dateFormat.parse(dateToUse);
                if (completedDate != null) {
                    LocalDate taskDate = new Date(completedDate.getTime()).toInstant()
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    
                    // Kiểm tra task thuộc tuần nào
                    for (int i = 0; i < 4; i++) {
                        LocalDate weekStart = now.minusWeeks(3 - i);
                        LocalDate weekEnd = weekStart.plusDays(6);
                        
                        if ((taskDate.isAfter(weekStart) || taskDate.isEqual(weekStart)) &&
                            (taskDate.isBefore(weekEnd) || taskDate.isEqual(weekEnd))) {
                            String weekLabel = "Tuần " + weekStart.format(DateTimeFormatter.ofPattern("dd/MM"));
                            weeklyData.put(weekLabel, weeklyData.get(weekLabel) + 1);
                            break;
                        }
                    }
                }
            } catch (ParseException e) {
                // Bỏ qua task có format ngày không hợp lệ
            }
        }
        
        return weeklyData;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private Map<String, Integer> getMonthlyCompletionData() {
        Map<String, Integer> monthlyData = new HashMap<>();
        List<Task> completedTasks = databaseHelper.getCompletedTasks();
        
        LocalDate now = LocalDate.now();
        
        // Tạo 6 tháng gần nhất
        for (int i = 5; i >= 0; i--) {
            LocalDate monthDate = now.minusMonths(i);
            String monthLabel = monthDate.format(DateTimeFormatter.ofPattern("MM/yyyy"));
            monthlyData.put(monthLabel, 0);
        }
        
        // Đếm số task hoàn thành trong mỗi tháng
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.ENGLISH);
        
        for (Task task : completedTasks) {
            try {
                String dateToUse = task.getCompletedDate() != null ? task.getCompletedDate() : task.getCreatedDate();
                Date completedDate = dateFormat.parse(dateToUse);
                if (completedDate != null) {
                    LocalDate taskDate = new Date(completedDate.getTime()).toInstant()
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    
                    String taskMonth = taskDate.format(DateTimeFormatter.ofPattern("MM/yyyy"));
                    if (monthlyData.containsKey(taskMonth)) {
                        monthlyData.put(taskMonth, monthlyData.get(taskMonth) + 1);
                    }
                }
            } catch (ParseException e) {
                // Bỏ qua task có format ngày không hợp lệ
            }
        }
        
        return monthlyData;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 