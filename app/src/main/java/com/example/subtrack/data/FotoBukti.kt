package com.example.subtrack.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object FotoBukti {

    private const val SISI_MAKS = 800
    private const val KUALITAS_JPEG = 60

    /** Batas aman panjang teks Base64. Dokumen Firestore maksimal sekitar 1 MB. */
    const val BATAS_PANJANG_TEKS = 700_000

    /** Membaca gambar dari Uri, memperkecilnya, lalu mengubahnya menjadi teks Base64. Null jika gagal. */
    fun ubahKeBase64(context: Context, uri: Uri): String? {
        return try {
            // Langkah 1: baca ukuran gambar saja, tanpa memuat isinya
            val opsiUkuran = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { masuk ->
                BitmapFactory.decodeStream(masuk, null, opsiUkuran)
            }

            // Langkah 2: tentukan pengecilan kasar (kelipatan 2) agar memori hemat
            var sampel = 1
            while (opsiUkuran.outWidth / sampel > SISI_MAKS * 2 ||
                opsiUkuran.outHeight / sampel > SISI_MAKS * 2
            ) {
                sampel *= 2
            }

            // Langkah 3: baca gambar sungguhan
            val opsiBaca = BitmapFactory.Options().apply { inSampleSize = sampel }
            val asli = context.contentResolver.openInputStream(uri)?.use { masuk ->
                BitmapFactory.decodeStream(masuk, null, opsiBaca)
            } ?: return null

            // Langkah 4: skalakan sampai sisi terpanjang 800 piksel (tidak pernah diperbesar)
            val skala = minOf(1f, SISI_MAKS.toFloat() / maxOf(asli.width, asli.height))
            val hasil = if (skala < 1f) {
                Bitmap.createScaledBitmap(
                    asli,
                    (asli.width * skala).toInt(),
                    (asli.height * skala).toInt(),
                    true
                )
            } else {
                asli
            }

            // Langkah 5: kompres ke JPEG lalu ubah menjadi teks
            val keluar = ByteArrayOutputStream()
            hasil.compress(Bitmap.CompressFormat.JPEG, KUALITAS_JPEG, keluar)
            Base64.encodeToString(keluar.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /** Mengubah teks Base64 kembali menjadi gambar. Null jika teksnya rusak. */
    fun ubahKeBitmap(base64: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}