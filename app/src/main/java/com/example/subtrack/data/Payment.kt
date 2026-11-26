package com.example.subtrack.data

import com.google.firebase.Timestamp

/** Satu dokumen di koleksi payments: satu kali pembayaran untuk satu jatuh tempo. */
data class Payment(
    val id: String = "",
    val userId: String = "",
    val subscriptionId: String = "",
    val nominal: Long = 0L,
    val catatan: String = "",
    val adaFoto: Boolean = false,
    val untukJatuhTempo: Timestamp? = null,
    val tanggalBayar: Timestamp? = null
)
