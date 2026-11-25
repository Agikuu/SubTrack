package com.example.subtrack.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

fun pesanErrorAuth(e: Exception): String = when (e) {
    is FirebaseAuthWeakPasswordException -> "Password terlalu lemah, gunakan minimal 6 karakter"
    is FirebaseAuthInvalidUserException -> "Akun dengan email ini tidak ditemukan"
    is FirebaseAuthInvalidCredentialsException -> "Email atau password salah"
    is FirebaseAuthUserCollisionException -> "Email ini sudah terdaftar"
    is FirebaseNetworkException -> "Tidak ada koneksi internet"
    else -> e.localizedMessage ?: "Terjadi kesalahan, coba lagi"
}