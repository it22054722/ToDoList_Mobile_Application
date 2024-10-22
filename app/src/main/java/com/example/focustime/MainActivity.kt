package com.example.focustime

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var tasksListView: ListView
    private lateinit var addTaskButton: Button
    private lateinit var tasksAdapter: ArrayAdapter<String>
    private val tasks = mutableListOf<String>()
    private val taskDetails = mutableListOf<TaskDetail>()
    private val handler = Handler(Looper.getMainLooper())
    private val countdownRunnable = object : Runnable {
        override fun run() {
            updateCountdowns()
            handler.postDelayed(this, 1000) // Update every second
        }
    }
    private val alertedTasks = mutableSetOf<String>() // Track tasks that have already alerted

    data class TaskDetail(val name: String, val description: String, val date: String, val time: String, val deadline: Long)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("FocusTimePrefs", Context.MODE_PRIVATE)
        tasksListView = findViewById(R.id.list_view_tasks)
        addTaskButton = findViewById(R.id.button_add_task)

        loadTasks()

        tasksAdapter = object : ArrayAdapter<String>(this, R.layout.task_item, R.id.text_view_task, tasks) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.task_item, parent, false)
                val taskTextView = view.findViewById<TextView>(R.id.text_view_task)
                val countdownTextView = view.findViewById<TextView>(R.id.text_view_countdown)

                taskTextView.text = tasks[position]
                val countdown = getCountdown(taskDetails[position].deadline)
                countdownTextView.text = countdown

                return view
            }
        }

        tasksListView.adapter = tasksAdapter

        addTaskButton.setOnClickListener {
            showAddTaskDialog()
        }

        tasksListView.setOnItemClickListener { _, _, position, _ ->
            showEditTaskDialog(position)
        }

        tasksListView.setOnItemLongClickListener { _, _, position, _ ->
            deleteTask(position)
            true
        }

        handler.post(countdownRunnable) // Start countdown updates
    }

    private fun showAddTaskDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.edit_text_task_name)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.edit_text_task_description)
        val dateTextView = dialogView.findViewById<TextView>(R.id.text_view_task_date)
        val timeTextView = dialogView.findViewById<TextView>(R.id.text_view_task_time)
        val saveButton = dialogView.findViewById<Button>(R.id.button_save_task)
        val dateButton = dialogView.findViewById<Button>(R.id.button_select_date)
        val timeButton = dialogView.findViewById<Button>(R.id.button_select_time)

        var selectedDate: String? = null
        var selectedTime: String? = null
        var deadline: Long? = null

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val date = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }.time
                    selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                    dateTextView.text = selectedDate
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        timeButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val timePickerDialog = TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                    timeTextView.text = selectedTime
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            )
            timePickerDialog.show()
        }

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val description = descriptionEditText.text.toString()
            if (name.isNotBlank() && selectedDate != null && selectedTime != null) {
                deadline = getDeadlineInMillis(selectedDate!!, selectedTime!!)
                addTask(name, description, selectedDate!!, selectedTime!!, deadline!!)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showEditTaskDialog(position: Int) {
        val taskDetail = taskDetails[position]
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.edit_text_task_name)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.edit_text_task_description)
        val dateTextView = dialogView.findViewById<TextView>(R.id.text_view_task_date)
        val timeTextView = dialogView.findViewById<TextView>(R.id.text_view_task_time)
        val saveButton = dialogView.findViewById<Button>(R.id.button_save_task)
        val dateButton = dialogView.findViewById<Button>(R.id.button_select_date)
        val timeButton = dialogView.findViewById<Button>(R.id.button_select_time)

        nameEditText.setText(taskDetail.name)
        descriptionEditText.setText(taskDetail.description)
        dateTextView.text = taskDetail.date
        timeTextView.text = taskDetail.time

        var updatedDate = taskDetail.date
        var updatedTime = taskDetail.time
        var updatedDeadline = taskDetail.deadline

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val date = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }.time
                    updatedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                    dateTextView.text = updatedDate
                    updatedDeadline = getDeadlineInMillis(updatedDate!!, updatedTime!!)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        timeButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val timePickerDialog = TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    updatedTime = String.format("%02d:%02d", hourOfDay, minute)
                    timeTextView.text = updatedTime
                    updatedDeadline = getDeadlineInMillis(updatedDate!!, updatedTime!!)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            )
            timePickerDialog.show()
        }

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val description = descriptionEditText.text.toString()
            if (name.isNotBlank() && updatedDate != null && updatedTime != null) {
                updateTask(position, name, description, updatedDate!!, updatedTime!!, updatedDeadline!!)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun addTask(name: String, description: String, date: String, time: String, deadline: Long) {
        val task = "$name: $description\nDate: $date\nTime: $time"
        tasks.add(task)
        taskDetails.add(TaskDetail(name, description, date, time, deadline))
        tasksAdapter.notifyDataSetChanged()
        saveTasks()
    }

    private fun updateTask(position: Int, name: String, description: String, date: String, time: String, deadline: Long) {
        val task = "$name: $description\nDate: $date\nTime: $time"
        tasks[position] = task
        taskDetails[position] = TaskDetail(name, description, date, time, deadline)
        tasksAdapter.notifyDataSetChanged()
        saveTasks()
    }

    private fun deleteTask(position: Int) {
        tasks.removeAt(position)
        taskDetails.removeAt(position)
        tasksAdapter.notifyDataSetChanged()
        saveTasks()
    }

    private fun loadTasks() {
        tasks.clear()
        taskDetails.clear()
        val savedTasks = sharedPreferences.getStringSet("tasks", emptySet()) ?: emptySet()
        for (task in savedTasks) {
            tasks.add(task)
            // Assuming saved task details are split into the following format:
            // taskName: taskDescription\nDate: taskDate\nTime: taskTime
            val parts = task.split("\n")
            if (parts.size == 3) {
                val nameAndDescription = parts[0].split(": ")
                val name = nameAndDescription[0]
                val description = nameAndDescription[1]
                val date = parts[1].replace("Date: ", "")
                val time = parts[2].replace("Time: ", "")
                val deadline = getDeadlineInMillis(date, time)
                taskDetails.add(TaskDetail(name, description, date, time, deadline))
            }
        }
    }

    private fun saveTasks() {
        val editor = sharedPreferences.edit()
        val taskSet = tasks.toSet()
        editor.putStringSet("tasks", taskSet)
        editor.apply()
    }

    private fun getCountdown(deadline: Long): String {
        val currentTime = System.currentTimeMillis()
        val timeLeft = deadline - currentTime
        return if (timeLeft > 0) {
            val hours = (timeLeft / (1000 * 60 * 60)).toInt()
            val minutes = ((timeLeft / (1000 * 60)) % 60).toInt()
            val seconds = ((timeLeft / 1000) % 60).toInt()
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            "00:00:00"
        }
    }

    private fun getDeadlineInMillis(date: String, time: String): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.parse("$date $time")?.time ?: 0L
    }

    private fun updateCountdowns() {
        for (i in tasks.indices) {
            val taskDetail = taskDetails[i]
            val countdown = getCountdown(taskDetail.deadline)
            val listItem = tasksListView.getChildAt(i)
            val countdownTextView = listItem?.findViewById<TextView>(R.id.text_view_countdown)
            countdownTextView?.text = countdown

            // Show alert when the countdown is up and task has not been alerted yet
            if (taskDetail.deadline <= System.currentTimeMillis() && taskDetail.name !in alertedTasks) {
                showAlert(taskDetail.name)
                alertedTasks.add(taskDetail.name) // Mark task as alerted
            }
        }
    }

    private fun showAlert(taskName: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Task Reminder")
        builder.setMessage("Time's up for task: $taskName")
        builder.setPositiveButton("OK", null)
        builder.show()

        // Play notification sound
        val mediaPlayer = MediaPlayer.create(this, R.raw.ganymede)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
        }
    }
}
