package com.loregate.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.loregate.app.domain.model.AuthUser
import com.loregate.app.domain.repository.AuthRepository
import com.loregate.app.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository
) : AuthRepository {

    override val currentUser: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toDomain())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    override fun getCurrentUser(): AuthUser? = firebaseAuth.currentUser?.toDomain()

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> =
        runCatching {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user?.toDomain()
                ?: throw IllegalStateException("Sign in failed: user not found.")
            // Sync user profile in Firestore (best effort)
            userRepository.createOrUpdateUserProfile(user)
            user
        }

    override suspend fun signUpWithEmail(email: String, password: String): Result<AuthUser> =
        runCatching {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user?.toDomain()
                ?: throw IllegalStateException("Sign up gagal: user tidak ditemukan.")
            userRepository.createOrUpdateUserProfile(user).getOrThrow()
            user
        }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthUser> =
        runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user?.toDomain()
                ?: throw IllegalStateException("Google sign in failed: user not found.")
            userRepository.createOrUpdateUserProfile(user)
            user
        }

    override fun signOut() {
        firebaseAuth.signOut()
    }

    private fun FirebaseUser.toDomain(): AuthUser = AuthUser(
        uid = uid,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl?.toString()
    )
}
