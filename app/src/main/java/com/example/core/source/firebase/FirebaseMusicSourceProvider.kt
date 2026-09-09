package com.example.core.source.firebase

import android.util.Log
import com.example.core.model.MusicSource
import com.example.core.model.Song
import com.example.core.source.MusicSourceProvider
import com.example.core.source.PlaybackIntegrationType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Cloud Music Provider using Firebase (v1.0).
 * Allows users to stream their own library from Firestore and Storage.
 */
class FirebaseMusicSourceProvider : MusicSourceProvider {

    private val firestore by lazy { 
        try { com.google.firebase.firestore.FirebaseFirestore.getInstance() } 
        catch (e: Exception) { null } 
    }
    private val storage by lazy { 
        try { com.google.firebase.storage.FirebaseStorage.getInstance() } 
        catch (e: Exception) { null } 
    }

    override val source: MusicSource = MusicSource.FIREBASE
    override val displayName: String = "Firebase Cloud"
    override val providerDescription: String = "Accede a tu propia biblioteca de música almacenada en la nube de Firebase."
    override val integrationType: PlaybackIntegrationType = PlaybackIntegrationType.NATIVE_EXOPLAYER

    private val _isConfigured = MutableStateFlow(true)
    override val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()

    private val _isConnected = MutableStateFlow(true)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    override suspend fun authenticate(credentials: Map<String, String>): Result<Unit> {
        return if (firestore != null) Result.success(Unit) else Result.failure(Exception("Firebase not initialized"))
    }

    override suspend fun disconnect() {
        _isConnected.value = false
    }

    override suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext emptyList()
        try {
            val collection = fs.collection("music")
            val snapshot = collection.get().await()

            val songs = snapshot.documents.mapNotNull { doc ->
                val title = doc.getString("title") ?: return@mapNotNull null
                val artist = doc.getString("artist") ?: "Artista Desconocido"
                val album = doc.getString("album") ?: "Álbum Desconocido"
                val fileName = doc.getString("file_name") ?: return@mapNotNull null
                val duration = doc.getLong("duration_ms") ?: 0L
                val artwork = doc.getString("artwork_url")

                if (query.isNotBlank() && 
                    !title.contains(query, ignoreCase = true) && 
                    !artist.contains(query, ignoreCase = true)) {
                    return@mapNotNull null
                }

                Song(
                    id = "fb_${doc.id}",
                    title = title,
                    artist = artist,
                    album = album,
                    durationMs = duration,
                    mediaUri = fileName, // We store fileName here and resolve it later
                    artworkUri = artwork,
                    source = MusicSource.FIREBASE,
                    path = fileName
                )
            }
            songs
        } catch (e: Exception) {
            Log.e("FirebaseProvider", "Fetch failed: ${e.message}")
            emptyList()
        }
    }

    override suspend fun resolveMediaUri(songId: String): String? = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext null
        val st = storage ?: return@withContext null
        try {
            val docId = songId.removePrefix("fb_")
            val doc = fs.collection("music").document(docId).get().await()
            val fileName = doc.getString("file_name") ?: return@withContext null
            
            // Get download URL from Storage
            val storageRef = st.reference.child("music/$fileName")
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("FirebaseProvider", "Resolve failed: ${e.message}")
            null
        }
    }

    /**
     * Scans Storage folder 'music/' and creates Firestore entries for missing files.
     */
    suspend fun syncCloudLibrary(): Result<Int> = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext Result.failure(Exception("Firestore not available"))
        val st = storage ?: return@withContext Result.failure(Exception("Storage not available"))
        try {
            // 1. List all .mp3 files in Storage folder 'music'
            val storageRef = st.reference.child("music")
            val listResult = storageRef.listAll().await()
            val storageFiles = listResult.items.filter { it.name.endsWith(".mp3", ignoreCase = true) }
            
            // 2. Get all current documents in Firestore 'music' collection
            val firestoreDocs = fs.collection("music").get().await()
            val existingFileNames = firestoreDocs.documents.mapNotNull { it.getString("file_name") }.toSet()
            
            // 3. Find files in Storage that are NOT in Firestore
            val missingFiles = storageFiles.filterNot { existingFileNames.contains(it.name) }
            
            if (missingFiles.isEmpty()) return@withContext Result.success(0)
            
            // 4. Create Firestore entries using Batch Write for efficiency
            val batch = fs.batch()
            missingFiles.forEach { file ->
                val newDocRef = fs.collection("music").document()
                val data = mapOf(
                    "title" to file.name.removeSuffix(".mp3").replace("_", " "),
                    "artist" to "Nube Personal",
                    "album" to "Firebase Cloud",
                    "file_name" to file.name,
                    "duration_ms" to 0L, // To be filled later or by metadata reader if possible
                    "artwork_url" to null
                )
                batch.set(newDocRef, data)
            }
            
            batch.commit().await()
            Result.success(missingFiles.size)
        } catch (e: Exception) {
            Log.e("FirebaseProvider", "Sync failed: ${e.message}")
            Result.failure(e)
        }
    }
}
