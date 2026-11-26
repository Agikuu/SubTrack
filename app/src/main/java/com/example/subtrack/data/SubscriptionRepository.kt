package com.example.subtrack.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object SubscriptionRepository {

    private val koleksi
        get() = FirebaseFirestore.getInstance().collection("subscriptions")

    private fun uidSaatIni(): String? = FirebaseAuth.getInstance().currentUser?.uid

    /** Memantau semua langganan milik pengguna yang login. Panggil remove() pada hasilnya saat layar berhenti. */
    fun dengarkanSemua(
        onData: (List<Subscription>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        val uid = uidSaatIni() ?: return null
        return koleksi.whereEqualTo("userId", uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error.localizedMessage ?: "Gagal memuat data")
            } else {
                val daftar = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Subscription::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                onData(daftar)
            }
        }
    }

    /** Memantau satu langganan. onData menerima null jika dokumennya sudah dihapus. */
    fun dengarkanSatu(
        id: String,
        onData: (Subscription?) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return koleksi.document(id).addSnapshotListener { doc, error ->
            if (error != null) {
                onError(error.localizedMessage ?: "Gagal memuat data")
            } else if (doc != null && doc.exists()) {
                onData(doc.toObject(Subscription::class.java)?.copy(id = doc.id))
            } else {
                onData(null)
            }
        }
    }

    /** Mengambil satu langganan satu kali, dipakai untuk mengisi form edit. */
    fun ambil(id: String, onData: (Subscription?) -> Unit, onError: (String) -> Unit) {
        koleksi.document(id).get()
            .addOnSuccessListener { doc ->
                onData(if (doc.exists()) doc.toObject(Subscription::class.java)?.copy(id = doc.id) else null)
            }
            .addOnFailureListener { onError(it.localizedMessage ?: "Gagal memuat data") }
    }

    fun tambah(s: Subscription, onSukses: () -> Unit, onGagal: (String) -> Unit) {
        val uid = uidSaatIni()
        if (uid == null) {
            onGagal("Sesi login berakhir, silakan login ulang")
            return
        }
        val data = isiForm(s) + mapOf(
            "userId" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        koleksi.add(data)
            .addOnSuccessListener { onSukses() }
            .addOnFailureListener { onGagal(it.localizedMessage ?: "Gagal menyimpan") }
    }

    /**
     * Memperbarui data langganan dari form.
     * resetSiklus bernilai true jika tanggal atau periode diubah, sehingga jadwal tagihan
     * dimulai dari awal dan siklusDibayar kembali menjadi 0.
     */
    fun perbarui(
        id: String,
        s: Subscription,
        onSukses: () -> Unit,
        onGagal: (String) -> Unit,
        resetSiklus: Boolean = false
    ) {
        var data = isiForm(s) + mapOf("updatedAt" to FieldValue.serverTimestamp())
        if (resetSiklus) data = data + mapOf("siklusDibayar" to 0L)
        koleksi.document(id).update(data)
            .addOnSuccessListener { onSukses() }
            .addOnFailureListener { onGagal(it.localizedMessage ?: "Gagal memperbarui") }
    }

    fun hapus(id: String, onSukses: () -> Unit, onGagal: (String) -> Unit) {
        koleksi.document(id).delete()
            .addOnSuccessListener { onSukses() }
            .addOnFailureListener { onGagal(it.localizedMessage ?: "Gagal menghapus") }
    }

    /** Field yang diisi pengguna lewat form. userId dan waktu ditambahkan terpisah di tambah() dan perbarui(). */
    private fun isiForm(s: Subscription): Map<String, Any> = mapOf(
        "namaLayanan" to s.namaLayanan,
        "kategori" to s.kategori,
        "harga" to s.harga,
        "periode" to s.periode,
        // form sudah memastikan tanggal terisi sebelum fungsi ini dipanggil
        "tanggalPembayaran" to (s.tanggalPembayaran ?: Timestamp.now()),
        "status" to s.status
    )
}