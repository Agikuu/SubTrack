package com.example.subtrack.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.subtrack.data.Konstanta
import com.example.subtrack.data.SubCalc
import com.example.subtrack.data.Subscription
import com.example.subtrack.data.SubscriptionRepository
import com.example.subtrack.databinding.ActivitySubscriptionDetailBinding
import com.google.firebase.firestore.ListenerRegistration

class SubscriptionDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "extra_id"
    }

    private lateinit var binding: ActivitySubscriptionDetailBinding
    private var id: String = ""
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val idDariIntent = intent.getStringExtra(EXTRA_ID)
        if (idDariIntent == null) {
            finish()
            return
        }
        id = idDariIntent

        binding.btnEdit.setOnClickListener {
            startActivity(
                Intent(this, AddEditSubscriptionActivity::class.java)
                    .putExtra(AddEditSubscriptionActivity.EXTRA_ID, id)
            )
        }
        binding.btnHapus.setOnClickListener { konfirmasiHapus() }
    }

    override fun onStart() {
        super.onStart()
        listener = SubscriptionRepository.dengarkanSatu(
            id,
            onData = { s -> if (s == null) finish() else tampilkan(s) },
            onError = { pesan -> toast(pesan) }
        )
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
        listener = null
    }

    private fun tampilkan(s: Subscription) {
        val satuan = if (s.periode == Konstanta.PERIODE_TAHUNAN) "tahun" else "bulan"
        val status = if (s.status == Konstanta.STATUS_AKTIF) "Aktif" else "Tidak aktif"

        binding.tvNama.text = s.namaLayanan
        binding.tvInfo.text = listOf(
            "Kategori: ${s.kategori}",
            "Harga: ${SubCalc.rupiah(s.harga.toDouble())} / $satuan",
            "Tanggal pembayaran tersimpan: ${SubCalc.tanggal(s.tanggalPembayaran?.toDate())}",
            "Jatuh tempo berikutnya: ${SubCalc.tanggal(SubCalc.jatuhTempoBerikutnya(s))}",
            "Status: $status",
            "Dibuat: ${SubCalc.tanggal(s.createdAt?.toDate())}",
            "Diperbarui: ${SubCalc.tanggal(s.updatedAt?.toDate())}"
        ).joinToString(System.lineSeparator())
    }

    private fun konfirmasiHapus() {
        AlertDialog.Builder(this)
            .setTitle("Hapus langganan?")
            .setMessage("Data langganan ini akan dihapus permanen.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                SubscriptionRepository.hapus(
                    id,
                    onSukses = {
                        toast("Langganan dihapus")
                        finish()
                    },
                    onGagal = { pesan -> toast(pesan) }
                )
            }
            .show()
    }

    private fun toast(pesan: String) {
        Toast.makeText(this, pesan, Toast.LENGTH_SHORT).show()
    }
}
