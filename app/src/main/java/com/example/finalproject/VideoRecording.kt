package com.example.finalproject

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.firestore.FirebaseFirestore


class VideoRecording : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var startRecordingButton: Button
    private lateinit var saveButton: Button
    private lateinit var backButton: Button
    private lateinit var videoTitleEditText: EditText

    private var videoCapture: VideoCapture<Recorder>? = null
    private var currentRecording: Recording? = null
    private var videoFile: File? = null

    private val CAMERA_PERMISSION_CODE = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_recording)

        previewView = findViewById(R.id.previewView)
        startRecordingButton = findViewById(R.id.startRecordingButton)
        saveButton = findViewById(R.id.saveButton)
        backButton = findViewById(R.id.backButton)
        videoTitleEditText = findViewById(R.id.videoTitleEditText)
        val feedbackEditText = findViewById<EditText>(R.id.feedbackEditText)


        saveButton.isEnabled = false

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                CAMERA_PERMISSION_CODE
            )
        } else {
            startCamera()
        }

        startRecordingButton.setOnClickListener {
            startRecordingButton.isEnabled = false
            saveButton.isEnabled = true
            startVideoRecording()
        }

        saveButton.setOnClickListener {
            stopRecordingAndUpload()
        }

        backButton.setOnClickListener {
            startActivity(Intent(this, Map::class.java))
        }

        // Exit button
        findViewById<Button>(R.id.exitBtn).setOnClickListener {
            val feedback = feedbackEditText.text.toString().trim()
            if (feedback.isEmpty()) {
                Toast.makeText(this, "Please provide your feedback before exiting", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "Unknown User"

            // 1. Save feedback to Firestore
            val db = FirebaseFirestore.getInstance()
            val feedbackData = hashMapOf(
                "userId" to userId,
                "feedback" to feedback,
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("feedback")
                .add(feedbackData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Feedback saved", Toast.LENGTH_SHORT).show()

                    // 2. Sentiment analysis
                    val sentiment = analyzeSentiment(feedback)

                    // 3. Send email to manager
                    sendEmailToManager(userId, feedback, sentiment)

                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to save feedback: ${e.message}", Toast.LENGTH_LONG).show()
                }

        }
    }

    private fun analyzeSentiment(feedback: String): String {
        val positiveWords = listOf("good", "great", "excellent", "happy", "love", "satisfied")
        val negativeWords = listOf("bad", "poor", "terrible", "hate", "angry", "unsatisfied")

        val lowerCaseFeedback = feedback.lowercase()

        return when {
            positiveWords.any { it in lowerCaseFeedback } -> "Satisfied"
            negativeWords.any { it in lowerCaseFeedback } -> "Not Satisfied"
            else -> "Somewhat Satisfied"
        }
    }

    private fun sendEmailToManager(userId: String, feedback: String, sentiment: String) {
        val managerEmail = "aryalpravesh111@gmail.com"
        val subject = "User Feedback Report"
        val message =
            """
                User ID: $userId
                Sentiment: $sentiment
                Feedback: $feedback
            """.trimIndent()

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(managerEmail))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, message)
        }

        try {
            startActivity(Intent.createChooser(emailIntent, "Send email..."))
        } catch (e: Exception) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }



    private fun allPermissionsGranted() = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    ).all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = androidx.camera.core.Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    videoCapture
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to start camera: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startVideoRecording() {
        val videoCapture = this.videoCapture ?: return

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        videoFile = File(externalCacheDir, "VID_$timestamp.mp4")

        val outputOptions = FileOutputOptions.Builder(videoFile!!).build()

        currentRecording = videoCapture.output
            .prepareRecording(this, outputOptions)
            .apply {
                if (ActivityCompat.checkSelfPermission(
                        this@VideoRecording,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            Toast.makeText(this, "Recording saved: ${videoFile?.absolutePath}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Recording error: ${recordEvent.error}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
    }

    private fun stopRecordingAndUpload() {
        currentRecording?.stop()
        currentRecording = null

        startRecordingButton.isEnabled = true
        saveButton.isEnabled = false

        val title = videoTitleEditText.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a video title", Toast.LENGTH_SHORT).show()
            return
        }

        if (videoFile == null || !videoFile!!.exists()) {
            Toast.makeText(this, "No video recorded", Toast.LENGTH_SHORT).show()
            return
        }

        uploadVideoToFirebase(videoFile!!, title)
    }

    private fun uploadVideoToFirebase(file: File, title: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val storageRef = FirebaseStorage.getInstance().reference.child("videos/$userId/$title.mp4")

        storageRef.putFile(Uri.fromFile(file))
            .addOnSuccessListener {
                Toast.makeText(this, "Video uploaded successfully", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera and audio permissions are required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}