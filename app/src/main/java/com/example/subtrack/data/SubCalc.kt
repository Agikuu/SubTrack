package com.example.subtrack.data

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Perhitungan biaya dan tanggal. Hasilnya estimasi, bukan tagihan resmi dari penyedia layanan. */
enum class StatusBayar {
    TIDAK_AKTIF,
    TERLAMBAT,
    HARI_INI,
    SEGERA,
    BELUM_JATUH_TEMPO,
    SUDAH_DIBAYAR
}

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

    /** Jatuh tempo dalam jarak sekian hari dari sekarang dianggap segera. */
    private const val BATAS_HARI_SEGERA = 7

    /** Pembayaran boleh dicatat paling cepat sekian hari sebelum jatuh tempo. */
    private const val BATAS_HARI_MUKA = 14

    private fun awalHari(date: Date): Date = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    /** Selisih hari dari hari ini ke tanggal tujuan. Negatif berarti sudah lewat. */
    fun selisihHari(tujuan: Date): Long {
        val selisihMs = awalHari(tujuan).time - awalHari(Date()).time
        return Math.round(selisihMs / 86_400_000.0)
    }

    private fun langkahBulan(s: Subscription): Int =
        if (s.periode == Konstanta.PERIODE_TAHUNAN) 12 else 1

    /**
     * Jatuh tempo yang sedang ditunggu: tanggal awal ditambah (siklusDibayar x periode).
     * Dihitung langsung dari tanggal awal, jadi tanggal 31 tidak bergeser ke 28 untuk seterusnya.
     */
    fun jatuhTempoAktif(s: Subscription): Date? {
        val awal = s.tanggalPembayaran?.toDate() ?: return null
        val tambahBulan = s.siklusDibayar * langkahBulan(s)
        return Calendar.getInstance().apply {
            time = awal
            add(Calendar.MONTH, tambahBulan.toInt())
        }.time
    }

    fun statusBayar(s: Subscription): StatusBayar {
        if (s.status != Konstanta.STATUS_AKTIF) return StatusBayar.TIDAK_AKTIF
        val tempo = jatuhTempoAktif(s) ?: return StatusBayar.BELUM_JATUH_TEMPO
        val sisa = selisihHari(tempo)
        return when {
            sisa < 0 -> StatusBayar.TERLAMBAT
            sisa == 0L -> StatusBayar.HARI_INI
            sisa <= BATAS_HARI_SEGERA -> StatusBayar.SEGERA
            s.siklusDibayar > 0 -> StatusBayar.SUDAH_DIBAYAR
            else -> StatusBayar.BELUM_JATUH_TEMPO
        }
    }

    /** Teks status untuk ditampilkan. Kosong untuk langganan yang tidak aktif. */
    fun teksStatusBayar(s: Subscription): String {
        val tempo = jatuhTempoAktif(s) ?: return ""
        val sisa = selisihHari(tempo)
        return when (statusBayar(s)) {
            StatusBayar.TIDAK_AKTIF -> ""
            StatusBayar.TERLAMBAT -> "Terlambat ${-sisa} hari"
            StatusBayar.HARI_INI -> "Jatuh tempo hari ini"
            StatusBayar.SEGERA -> "Jatuh tempo $sisa hari lagi"
            StatusBayar.BELUM_JATUH_TEMPO -> "Belum jatuh tempo"
            StatusBayar.SUDAH_DIBAYAR -> "Periode ini sudah dibayar"
        }
    }

    /** Pembayaran hanya bisa dicatat untuk langganan aktif yang jatuh temponya sudah lewat atau tinggal 14 hari lagi. */
    fun bisaDicatat(s: Subscription): Boolean {
        if (s.status != Konstanta.STATUS_AKTIF) return false
        val tempo = jatuhTempoAktif(s) ?: return false
        return selisihHari(tempo) <= BATAS_HARI_MUKA
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
