package com.example.subtrack.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.subtrack.data.Konstanta
import com.example.subtrack.data.Subscription
import com.example.subtrack.data.SubscriptionRepository
import com.example.subtrack.databinding.FragmentSubscriptionsBinding
import com.google.firebase.firestore.ListenerRegistration

class SubscriptionsFragment : Fragment() {

    private var _binding: FragmentSubscriptionsBinding? = null
    private val binding get() = _binding!!

    private val adapter = SubscriptionAdapter { s ->
        startActivity(
            Intent(requireContext(), SubscriptionDetailActivity::class.java)
                .putExtra(SubscriptionDetailActivity.EXTRA_ID, s.id)
        )
    }
    private var semua: List<Subscription> = emptyList()
    private var listener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscriptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvLangganan.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLangganan.adapter = adapter

        binding.spKategoriFilter.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Semua kategori") + Konstanta.KATEGORI
        )
        binding.spStatusFilter.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Semua status", "Aktif", "Tidak aktif")
        )

        val saatFilterBerubah = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                terapkanFilter()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        binding.spKategoriFilter.onItemSelectedListener = saatFilterBerubah
        binding.spStatusFilter.onItemSelectedListener = saatFilterBerubah
        binding.etCari.doAfterTextChanged { terapkanFilter() }

        binding.fabTambah.setOnClickListener {
            startActivity(Intent(requireContext(), AddEditSubscriptionActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        listener = SubscriptionRepository.dengarkanSemua(
            onData = { daftar ->
                semua = daftar
                terapkanFilter()
            },
            onError = { pesan ->
                Toast.makeText(requireContext(), pesan, Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
        listener = null
    }

    private fun terapkanFilter() {
        val b = _binding ?: return
        val kata = b.etCari.text.toString().trim()
        val posisiKategori = b.spKategoriFilter.selectedItemPosition
        val posisiStatus = b.spStatusFilter.selectedItemPosition

        val hasil = semua
            .filter { s -> s.namaLayanan.contains(kata, ignoreCase = true) }
            .filter { s ->
                posisiKategori == 0 || s.kategori == Konstanta.KATEGORI[posisiKategori - 1]
            }
            .filter { s ->
                when (posisiStatus) {
                    1 -> s.status == Konstanta.STATUS_AKTIF
                    2 -> s.status == Konstanta.STATUS_TIDAK_AKTIF
                    else -> true
                }
            }
            .sortedBy { it.namaLayanan.lowercase() }

        adapter.submit(hasil)
        b.tvKosong.visibility = if (hasil.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
