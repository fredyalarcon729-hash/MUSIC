package com.example.core.auth

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import java.math.BigInteger

/**
 * Handles Google Authentication using the Android Credential Manager (v6.4).
 * Includes diagnostic tools to retrieve SHA-1 and package name.
 */
class AuthManager(private val context: Context) {

    private val TAG = "AuthManager"
    private val credentialManager = CredentialManager.create(context)

    private val _userState = MutableStateFlow<UserSession?>(null)
    val userState: StateFlow<UserSession?> = _userState.asStateFlow()

    data class UserSession(
        val displayName: String?,
        val email: String?,
        val idToken: String,
        val photoUrl: String?
    )

    data class DiagnosticInfo(
        val packageName: String,
        val sha1: String
    )

    /**
     * Retrieves the SHA-1 fingerprint and package name for diagnostic purposes.
     */
    fun getDiagnosticInfo(): DiagnosticInfo {
        val packageName = context.packageName
        var sha1 = "No disponible"
        try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }

            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.signingCertificateHistory
            } else {
                packageInfo.signatures
            }

            if (signatures != null) {
                for (signature in signatures) {
                    val md = MessageDigest.getInstance("SHA-1")
                    val digest = md.digest(signature.toByteArray())
                    sha1 = digest.joinToString(":") { "%02X".format(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting SHA-1", e)
        }
        return DiagnosticInfo(packageName, sha1)
    }

    private fun generateNonce(): String {
        return BigInteger(130, SecureRandom()).toString(32)
    }

    suspend fun signIn(activityContext: Context, clientId: String?): Result<UserSession> {
        val cleanClientId = clientId?.trim() ?: ""
        
        if (cleanClientId.isEmpty() || !cleanClientId.endsWith(".apps.googleusercontent.com")) {
            return Result.failure(Exception("Client ID inválido. Asegúrate de que termine en .apps.googleusercontent.com"))
        }

        return try {
            val nonce = generateNonce()
            Log.d(TAG, "Starting sign-in with ClientID: $cleanClientId")

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(cleanClientId)
                .setAutoSelectEnabled(true)
                .setNonce(nonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = activityContext,
                request = request
            )

            val credential = result.credential
            if (credential is GoogleIdTokenCredential) {
                val session = UserSession(
                    displayName = credential.displayName,
                    email = credential.id,
                    idToken = credential.idToken,
                    photoUrl = credential.profilePictureUri?.toString()
                )
                _userState.value = session
                Log.d(TAG, "Sign-in successful for: ${session.email}")
                Result.success(session)
            } else {
                Result.failure(Exception("Error: Recibida credencial no compatible (${credential.type})"))
            }
        } catch (e: androidx.credentials.exceptions.GetCredentialException) {
            Log.e(TAG, "Credential Manager Error", e)
            val info = getDiagnosticInfo()
            val msg = when(e) {
                is androidx.credentials.exceptions.NoCredentialException -> 
                    "Error 28444: No hay cuentas vinculadas. Verifica en Google Cloud:\n" +
                    "1. ID Android con Paquete: ${info.packageName}\n" +
                    "2. SHA-1: ${info.sha1}\n" +
                    "3. Que el ID Web en la app sea del mismo proyecto."
                is androidx.credentials.exceptions.GetCredentialCancellationException -> "Operación cancelada."
                else -> "Error de Google (${e.type}): ${e.message}"
            }
            Result.failure(Exception(msg))
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in unexpected failure", e)
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            _userState.value = null
            Log.d(TAG, "User signed out")
        } catch (e: Exception) {
            Log.e(TAG, "Sign-out error", e)
        }
    }
}
