package com.example.subtrack.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.subtrack.data.Konstanta
import com.example.subtrack.data.SubCalc
import com.example.subtrack.data.Subscription
import com.example.subtrack.data.SubscriptionRepository
import com.example.subtrack.databinding.ActivityAddEditSubscriptionBinding
import com.google.firebase.Timestamp
import java.util.Calendar

class AddEditSubscriptionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "extra_id"
    }

    private lateinit var binding: ActivityAddEditSubscriptionBinding
    private var idEdit: String? = null
    private var tanggalDipilih: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditSubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.spKategori.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, Konstanta.KATEGORI
        )

        idEdit = intent.getStringExtra(EXTRA_ID)
        idEdit?.let { id ->
            binding.tvJudul.text = "Edit Langganan"
            muatDataLama(id)
        }

        binding.btnTanggal.setOnClickListener { tampilkanDatePicker() }
        binding.btnSimpan.setOnClickListener { simpan() }
    }

    private fun muatDataLama(id: String) {
        SubscriptionRepository.ambil(
            id,
            onData = { s ->
                if (s == null) {
                    toast("Data tidak ditemukan")
                    finish()
                } else {
                    isiForm(s)
                }
            },
            onError = { pesan -> toast(pesan) }
        )
    }

    private fun isiForm(s: Subscription) {
        binding.etNama.setText(s.namaLayanan)
        binding.spKategori.setSelection(Konstanta.KATEGORI.indexOf(s.kategori).coerceAtLeast(0))
        binding.etHarga.setText(s.harga.toString())

        if (s.periode == Konstanta.PERIODE_TAHUNAN) {
            binding.rbTahunan.isChecked = true
        } else {
            binding.rbBulanan.isChecked = true
        }

        s.tanggalPembayaran?.toDate()?.let { tgl ->
            tanggalDipilih = Calendar.getInstance().apply { time = tgl }
            binding.btnTanggal.text = SubCalc.tanggal(tgl)
        }

        if (s.status == Konstanta.STATUS_AKTIF) {
            binding.rbAktif.isChecked = true
        } else {
            binding.rbTidakAktif.isChecked = true
        }
    }

    private fun tampilkanDatePicker() {
        val acuan = tanggalDipilih ?: Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, tahun, bulan, hari ->
                val kal = Calendar.getInstance().apply {
                    clear()
                    set(tahun, bulan, hari, 0, 0, 0)
                }
                tanggalDipilih = kal
                binding.btnTanggal.text = SubCalc.tanggal(kal.time)
            },
            acuan.get(Calendar.YEAR),
            acuan.get(Calendar.MONTH),
            acuan.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun simpan() {
        val nama = binding.etNama.text.toString().trim()
        val harga = binding.etHarga.text.toString().trim().toLongOrNull()
        binding.tilNama.error = null
        binding.tilHarga.error = null

        var valid = true
        if (nama.isEmpty()) {
            binding.tilNama.error = "Nama layanan wajib diisi"
            valid = false
        }
        if (harga == null || harga <= 0) {
            binding.tilHarga.error = "Harga harus berupa angka lebih dari 0"
            valid = false
        }
        if (binding.rgPeriode.checkedRadioButtonId == -1) {
            toast("Pilih periode pembayaran")
            valid = false
        }
        if (tanggalDipilih == null) {
            toast("Pilih tanggal pembayaran")
            valid = false
        }
        if (!valid) return

        // harga dan tanggalDipilih tidak mungkin null di sini karena sudah divalidasi
        val langganan = Subscription(
            namaLayanan = nama,
            kategori = binding.spKategori.selectedItem.toString(),
            harga = harga!!,
            periode = if (binding.rbTahunan.isChecked) {
                Konstanta.PERIODE_TAHUNAN
            } else {
                Konstanta.PERIODE_BULANAN
            },
            tanggalPembayaran = Timestamp(tanggalDipilih!!.time),
            status = if (binding.rbAktif.isChecked) {
                Konstanta.STATUS_AKTIF
            } else {
                Konstanta.STATUS_TIDAK_AKTIF
            }
        )

        binding.btnSimpan.isEnabled = false
        val sukses = {
            toast("Tersimpan")
            finish()
        }
        val gagal = { pesan: String ->
            binding.btnSimpan.isEnabled = true
            toast(pesan)
        }

        val id = idEdit
        if (id == null) {
            SubscriptionRepository.tambah(langganan, sukses, gagal)
        } else {
            SubscriptionRepository.perbarui(id, langganan, sukses, gagal)
        }
    }

    private fun toast(pesan: String) {
        Toast.makeText(this, pesan, Toast.LENGTH_SHORT).show()
    }
}
