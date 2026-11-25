package com.example.subtrack.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.subtrack.MainActivity
import com.example.subtrack.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnDaftar.setOnClickListener { daftar() }
        binding.tvKeLogin.setOnClickListener { finish() }
    }

    private fun daftar() {
        val nama = binding.etNama.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val konfirmasi = binding.etKonfirmasi.text.toString()

        binding.tilNama.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilKonfirmasi.error = null

        if (nama.isEmpty()) {
            binding.tilNama.error = "Nama wajib diisi"
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Masukkan email yang valid"
            return
        }
        if (password.length < 6) {
            binding.tilPassword.error = "Password minimal 6 karakter"
            return
        }
        if (password != konfirmasi) {
            binding.tilKonfirmasi.error = "Konfirmasi password tidak sama"
            return
        }

        tampilkanLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { hasil ->
                val uid = hasil.user!!.uid // akun baru pasti punya user
                val dataUser = hashMapOf(
                    "nama" to nama,
                    "email" to email,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .set(dataUser)
                    .addOnCompleteListener { keMain() }
            }
            .addOnFailureListener { e ->
                tampilkanLoading(false)
                Toast.makeText(this, pesanErrorAuth(e), Toast.LENGTH_LONG).show()
            }
    }

    private fun keMain() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    private fun tampilkanLoading(loading: Boolean) {
        binding.btnDaftar.isEnabled = !loading
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
    }
}