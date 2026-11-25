package com.example.subtrack.data

import com.google.firebase.Timestamp

/** Nilai tetap yang dipakai di seluruh aplikasi. */
object Konstanta {
    val KATEGORI = listOf("Hiburan", "Musik", "Produktivitas", "Penyimpanan", "Pendidikan", "Lainnya")

    const val PERIODE_BULANAN = "bulanan"
    const val PERIODE_TAHUNAN = "tahunan"

    const val STATUS_AKTIF = "aktif"
    const val STATUS_TIDAK_AKTIF = "tidak_aktif"
}

/** Satu dokumen di koleksi "subscriptions". */
data class Subscription(
    val id: String = "",
    val userId: String = "",
    val namaLayanan: String = "",
    val kategori: String = "",
    val harga: Long = 0L,
    val periode: String = Konstanta.PERIODE_BULANAN,
    val tanggalPembayaran: Timestamp? = null,
    val status: String = Konstanta.STATUS_AKTIF,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
