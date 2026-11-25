package com.example.subtrack.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.subtrack.MainActivity
import com.example.subtrack.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { login() }
        binding.tvKeRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun login() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        binding.tilEmail.error = null
        binding.tilPassword.error = null

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Masukkan email yang valid"
            return
        }
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password wajib diisi"
            return
        }

        tampilkanLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
            }
            .addOnFailureListener { e ->
                tampilkanLoading(false)
                Toast.makeText(this, pesanErrorAuth(e), Toast.LENGTH_LONG).show()
            }
    }

    private fun tampilkanLoading(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
    }
}