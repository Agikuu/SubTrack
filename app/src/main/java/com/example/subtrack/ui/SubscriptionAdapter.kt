package com.example.subtrack.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.subtrack.data.Konstanta
import com.example.subtrack.data.SubCalc
import com.example.subtrack.data.Subscription
import com.example.subtrack.databinding.ItemSubscriptionBinding

class SubscriptionAdapter(
    private val onClick: (Subscription) -> Unit
) : RecyclerView.Adapter<SubscriptionAdapter.VH>() {

    private var data: List<Subscription> = emptyList()

    fun submit(list: List<Subscription>) {
        data = list
        notifyDataSetChanged()
    }

    class VH(val b: ItemSubscriptionBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemSubscriptionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = data[position]
        val satuan = if (s.periode == Konstanta.PERIODE_TAHUNAN) "tahun" else "bulan"

        holder.b.tvNama.text = s.namaLayanan
        holder.b.tvKategori.text = s.kategori
        holder.b.tvHarga.text = "${SubCalc.rupiah(s.harga.toDouble())} / $satuan"
        holder.b.tvJatuhTempo.text =
            "Jatuh tempo: ${SubCalc.tanggal(SubCalc.jatuhTempoBerikutnya(s))}"
        holder.b.tvStatus.text =
            if (s.status == Konstanta.STATUS_AKTIF) "Aktif" else "Tidak aktif"
        holder.itemView.setOnClickListener { onClick(s) }
    }
}
