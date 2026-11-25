package com.example.subtrack.data

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Perhitungan biaya dan tanggal. Hasilnya estimasi, bukan tagihan resmi dari penyedia layanan. */
object SubCalc {

    private val lokalIndonesia = Locale("id", "ID")

    fun aktif(daftar: List<Subscription>): List<Subscription> =
        daftar.filter { it.status == Konstanta.STATUS_AKTIF }

    /** Biaya per bulan: langganan tahunan dibagi 12. */
    fun biayaBulanan(s: Subscription): Double =
        if (s.periode == Konstanta.PERIODE_TAHUNAN) s.harga / 12.0 else s.harga.toDouble()

    /** Biaya per tahun: langganan bulanan dikali 12. */
    fun biayaTahunan(s: Subscription): Double =
        if (s.periode == Konstanta.PERIODE_TAHUNAN) s.harga.toDouble() else s.harga * 12.0

    fun totalBulanan(daftar: List<Subscription>): Double =
        aktif(daftar).sumOf { biayaBulanan(it) }

    fun totalTahunan(daftar: List<Subscription>): Double =
        aktif(daftar).sumOf { biayaTahunan(it) }

    /** Tanggal pembayaran berikutnya yang belum lewat dari hari ini. */
    fun jatuhTempoBerikutnya(s: Subscription): Date? {
        val awal = s.tanggalPembayaran?.toDate() ?: return null
        val langkahBulan = if (s.periode == Konstanta.PERIODE_TAHUNAN) 12 else 1

        val hariIni = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        var kelipatan = 0
        while (true) {
            val kandidat = Calendar.getInstance().apply {
                time = awal
                add(Calendar.MONTH, kelipatan * langkahBulan)
            }.time
            // batas 1200 hanya pengaman agar perulangan tidak pernah tak berujung
            if (!kandidat.before(hariIni) || kelipatan > 1200) return kandidat
            kelipatan++
        }
    }

    fun rupiah(nilai: Double): String {
        val format = NumberFormat.getCurrencyInstance(lokalIndonesia)
        format.maximumFractionDigits = 0
        return format.format(nilai)
    }

    fun tanggal(date: Date?): String {
        if (date == null) return "-"
        return SimpleDateFormat("dd MMM yyyy", lokalIndonesia).format(date)
    }
}
