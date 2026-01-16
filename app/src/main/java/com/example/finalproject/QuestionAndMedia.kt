package com.example.finalproject

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore

class QuestionAndMedia : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_question_and_media)

        db = FirebaseFirestore.getInstance()
        val prefs = getSharedPreferences("docIdPref", MODE_PRIVATE)
        val docId = prefs.getString("docId", null)

        // Spinner
        val questionSpinner = findViewById<Spinner>(R.id.questionSpinner)
        val questions = listOf("What is my name?", "What is my family name?", "What is my full name?", "What year was I born?",
                                "What city am I living in?", "What country am I living in?")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, questions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        questionSpinner.adapter = adapter

        val answer = findViewById<TextView>(R.id.answer)

        // Listen for spinner selection changes
        questionSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val selectedQuestion = parent.getItemAtPosition(position).toString()
                if (!docId.isNullOrBlank()) {
                    fetchAnswerFromFirestore(selectedQuestion, answer, docId)
                }else {
                    val answer = findViewById<TextView>(R.id.answer)
                    answer.text = "No data found"
                }

            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {
                answer.text = ""
            }
        })


        // Map Button
        val mapBtn = findViewById<Button>(R.id.mapBtn)
        mapBtn.setOnClickListener {
            startActivity(Intent(this, Map::class.java))
        }
        // Recording Button
        val recordingBtn = findViewById<Button>(R.id.recordBtn)
        recordingBtn.setOnClickListener {
            startActivity(Intent(this, VideoRecording::class.java))
        }
        // Next Button
        val nextBtn = findViewById<Button>(R.id.nextBtn)
        nextBtn.setOnClickListener {
            startActivity(Intent(this, Map::class.java))
        }
        // Back Button
        val backBtn = findViewById<Button>(R.id.backBtn)
        backBtn.setOnClickListener {
            startActivity(Intent(this, PersonalInformation::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun fetchAnswerFromFirestore(question: String, answerTextView: TextView, docId: String) {
        // Assuming you have only one user doc, or you have a way to identify it (replace "USER_DOC_ID" accordingly)
        val docRef = db.collection("users").document(docId)

        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val answer = when (question) {
                        "What is my name?" -> document.getString("first name") ?: "Not found"
                        "What is my family name?" -> document.getString("family name") ?: "Not found"
                        "What is my full name?" -> {
                            val first = document.getString("first name") ?: ""
                            val last = document.getString("family name") ?: ""
                            "$first $last".trim()
                        }
                        "What year was I born?" -> {
                            val age = document.getLong("age")?.toInt()
                            if (age != null) {
                                val currentYear =
                                    java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                                (currentYear - age).toString()
                            } else {
                                "Age not found"
                            }
                        }
                        "What city am I living in?" -> document.getString("city") ?: "Not found"
                        "What country am I living in?" -> document.getString("country") ?: "Not found"
                        else -> "Unknown question"
                    }
                    answerTextView.text = answer
                } else {
                    answerTextView.text = "No data found"
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching data: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

}