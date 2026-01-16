package com.example.finalproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager
    private val intentToNextPage by lazy { Intent(this, PersonalInformation::class.java) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        FacebookSdk.sdkInitialize(applicationContext)
        callbackManager = CallbackManager.Factory.create()

        // Login
        findViewById<Button>(R.id.loginBtn).setOnClickListener {
            val email = findViewById<EditText>(R.id.email).text.toString().trim()
            val password = findViewById<EditText>(R.id.password).text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        Toast.makeText(this, "Login Successful! Welcome ${user?.email}", Toast.LENGTH_SHORT).show()
                        startActivity(intentToNextPage)
                    } else {
                        val exception = task.exception
                        when {
                            exception is FirebaseAuthInvalidUserException -> {
                                Toast.makeText(this, "No account found. Please sign up first.", Toast.LENGTH_LONG).show()
                            }
                            exception is FirebaseAuthInvalidCredentialsException -> {
                                Toast.makeText(this, "Invalid email or password. Please try again.", Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                Toast.makeText(this, "Login Failed: ${exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
        }



        // Google Sign In setup
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        findViewById<Button>(R.id.googleSignupBtn).setOnClickListener {
            signInWithGoogle()
        }

        // Register Facebook callback once
        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    handleFacebookAccessToken(loginResult.accessToken)
                }

                override fun onCancel() {
                    Toast.makeText(applicationContext, "Facebook Login Cancelled", Toast.LENGTH_SHORT).show()
                }

                override fun onError(error: FacebookException) {
                    Toast.makeText(applicationContext, "Facebook Login Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })

        // Facebook button click
        findViewById<Button>(R.id.facebookSignupBtn).setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(
                this, listOf("email", "public_profile")
            )
        }

        // Sign in with email and password
        findViewById<Button>(R.id.emailSignupBtn).setOnClickListener {
            val signupIntent = Intent(this, Signup::class.java)
            startActivity(signupIntent)
        }


        // anonymous login
        val anonymousBtn = findViewById<Button>(R.id.anonymousSignInBtn)
        anonymousBtn.setOnClickListener {
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        Toast.makeText(this, "Signed in anonymously! UID: ${user?.uid}", Toast.LENGTH_SHORT).show()
                        startActivity(intentToNextPage)
                    } else {
                        Toast.makeText(this, "Anonymous sign-in failed: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
        }

    }


    // Google Sign-In result launcher
    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account: GoogleSignInAccount = task.getResult(Exception::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: Exception) {
                Log.w("GoogleSignIn", "Google sign in failed", e)
                Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
            }
        }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "Google login successful! Welcome ${user?.displayName}", Toast.LENGTH_SHORT).show()
                    startActivity(intentToNextPage)
                } else {
                    Toast.makeText(this, "Google authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "Facebook login successful! Welcome ${user?.displayName}", Toast.LENGTH_LONG).show()
                    startActivity(intentToNextPage)
                } else {
                    Toast.makeText(this, "Facebook authentication failed", Toast.LENGTH_SHORT).show()
                    if (!task.isSuccessful) {
                        val error = task.exception
                        Log.e("FacebookAuth", "signInWithCredential failed", error)
                        Toast.makeText(this, "Facebook authentication failed: ${error?.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    // Required for Facebook login
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
}
