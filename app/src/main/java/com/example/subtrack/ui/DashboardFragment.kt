package com.example.subtrack.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.subtrack.data.SubCalc
import com.example.subtrack.data.Subscription
import com.example.subtrack.data.SubscriptionRepository
import com.example.subtrack.databinding.FragmentDashboardBinding
import com.google.firebase.firestore.ListenerRegistration

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val adapter = SubscriptionAdapter { s ->
        startActivity(
            Intent(requireContext(), SubscriptionDetailActivity::class.java)
                .putExtra(SubscriptionDetailActivity.EXTRA_ID, s.id)
        )
    }
    private var listener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvMendatang.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMendatang.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        listener = SubscriptionRepository.dengarkanSemua(
            onData = { daftar -> tampilkan(daftar) },
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

    private fun tampilkan(daftar: List<Subscription>) {
        val b = _binding ?: return
        val aktif = SubCalc.aktif(daftar)

        b.tvJumlahAktif.text = "${aktif.size} layanan aktif"
        b.tvBulanan.text = SubCalc.rupiah(SubCalc.totalBulanan(daftar))
        b.tvTahunan.text = SubCalc.rupiah(SubCalc.totalTahunan(daftar))

        val mendatang = aktif
            .mapNotNull { s -> SubCalc.jatuhTempoBerikutnya(s)?.let { tanggal -> s to tanggal } }
            .sortedBy { pasangan -> pasangan.second }
            .take(5)
            .map { pasangan -> pasangan.first }

        adapter.submit(mendatang)
        b.tvKosong.visibility = if (mendatang.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}