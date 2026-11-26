package com.example.subtrack.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Date

object PaymentRepository {

    private val db
        get() = FirebaseFirestore.getInstance()

    private val koleksiBayar
        get() = db.collection("payments")

    private val koleksiFoto
        get() = db.collection("bukti_foto")

    private fun uidSaatIni(): String? = FirebaseAuth.getInstance().currentUser?.uid

    /** Memantau riwayat pembayaran satu langganan, yang terbaru di atas. Panggil remove() pada hasilnya saat layar berhenti. */
    fun dengarkanRiwayat(
        subscriptionId: String,
        onData: (List<Payment>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        val uid = uidSaatIni() ?: return null
        return koleksiBayar
            .whereEqualTo("userId", uid)
            .whereEqualTo("subscriptionId", subscriptionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Gagal memuat riwayat")
                } else {
                    val daftar = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Payment::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    // tanggalBayar bisa null sesaat setelah disimpan (menunggu server), anggap yang terbaru
                    onData(daftar.sortedByDescending { it.tanggalBayar?.seconds ?: Long.MAX_VALUE })
                }
            }
    }

    /**
     * Mencatat satu pembayaran. Tiga penulisan dikirim sebagai satu batch:
     * dokumen payments, dokumen bukti_foto (jika ada foto), dan kenaikan siklusDibayar.
     * Hanya perlu dipanggil jika SubCalc.bisaDicatat(langganan) bernilai true.
     */
    fun catat(
        langganan: Subscription,
        untukJatuhTempo: Date,
        catatan: String,
        fotoBase64: String?,
        onSukses: () -> Unit,
        onGagal: (String) -> Unit
    ) {
        val uid = uidSaatIni()
        if (uid == null) {
            onGagal("Sesi login berakhir, silakan login ulang")
            return
        }

        val refBayar = koleksiBayar.document()
        val refLangganan = db.collection("subscriptions").document(langganan.id)
        val batch = db.batch()

        batch.set(
            refBayar,
            mapOf(
                "userId" to uid,
                "subscriptionId" to langganan.id,
                "nominal" to langganan.harga,
                "catatan" to catatan,
                "adaFoto" to (fotoBase64 != null),
                "untukJatuhTempo" to Timestamp(untukJatuhTempo),
                "tanggalBayar" to FieldValue.serverTimestamp()
            )
        )

        if (fotoBase64 != null) {
            batch.set(
                koleksiFoto.document(refBayar.id),
                mapOf(
                    "userId" to uid,
                    "paymentId" to refBayar.id,
                    "data" to fotoBase64,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }

        batch.update(
            refLangganan,
            mapOf(
                "siklusDibayar" to FieldValue.increment(1),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        batch.commit()
            .addOnSuccessListener { onSukses() }
            .addOnFailureListener { onGagal(it.localizedMessage ?: "Gagal menyimpan pembayaran") }
    }

    /** Mengambil teks Base64 foto bukti satu pembayaran. onData menerima null jika tidak ada foto. */
    fun ambilFoto(paymentId: String, onData: (String?) -> Unit, onError: (String) -> Unit) {
        koleksiFoto.document(paymentId).get()
            .addOnSuccessListener { doc -> onData(doc.getString("data")) }
            .addOnFailureListener { onError(it.localizedMessage ?: "Gagal memuat foto") }
    }
}