package com.example.finalproject

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.Spinner
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

class PersonalInformation : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_personal_information)

        db = FirebaseFirestore.getInstance()
        var docId: String? = null

        // Spinner Info
        val ageSpinner = findViewById<Spinner>(R.id.ageSpinner)
        val countrySpinner = findViewById<Spinner>(R.id.countrySpinner)
        val citySpinner = findViewById<Spinner>(R.id.citySpinner)

        val numbers = (1..100).map { it.toString() }
        val countries = listOf("USA", "Canada", "Nepal", "Australia")
        val cities = listOf("New York", "Toronto", "Kathmandu", "Sydney")

        // Adapter for numbers
        val ageAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            numbers
        )
        // Adapter for countries
        val countryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            countries
        )
        // Adapter for cities
        val cityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            cities
        )
        countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        countrySpinner.adapter = countryAdapter

        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        citySpinner.adapter = cityAdapter

        ageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ageSpinner.adapter = ageAdapter



        // Submit Button
        val submitBtn = findViewById<Button>(R.id.submitBtn)
        submitBtn.setOnClickListener {
            val selectedAge = ageSpinner.selectedItem.toString().toIntOrNull() ?: 0
            val selectedCountry = countrySpinner.selectedItem.toString()
            val selectedCity = citySpinner.selectedItem.toString()
            val firstName = findViewById<EditText>(R.id.firstName).text.toString()
            val familyName = findViewById<EditText>(R.id.familyName).text.toString()

            if (firstName.isEmpty() || familyName.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show confirmation dialog
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Confirm Submission")
            builder.setMessage("Do you want to save this data?")
            builder.setPositiveButton("Save") { dialog, which ->
                // Save data to Firestore
                val userData = hashMapOf(
                    "first name" to firstName,
                    "family name" to familyName,
                    "age" to selectedAge,
                    "country" to selectedCountry,
                    "city" to selectedCity
                )

                db.collection("users")
                    .add(userData)
                    .addOnSuccessListener { documentRef ->
                        docId = documentRef.id
                        val prefs = getSharedPreferences("docIdPref", MODE_PRIVATE)
                        prefs.edit().putString("docId", docId).apply()

                        Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error saving data: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
            }
            builder.setNegativeButton("Cancel") { dialog, which ->
                Toast.makeText(this, "Data was not saved", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }

            builder.create().show()
        }

        // Next Button
        val nextBtn = findViewById<Button>(R.id.nextBtn)
        nextBtn.setOnClickListener {
            val intent = Intent(this, QuestionAndMedia::class.java)
            startActivity(intent)
        }
        // Back Button
        val backBtn = findViewById<Button>(R.id.backBtn)
        backBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}