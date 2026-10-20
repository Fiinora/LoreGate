package com.loregate.app.data.repository

import com.loregate.app.domain.model.AuthUser
import com.loregate.app.domain.repository.AuthRepository
import com.loregate.app.domain.repository.UserRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository
) : AuthRepository {

    override val currentUser: Flow<AuthUser?> = firebaseAuth.authStateChanged.map { it?.toDomain() }

    override fun getCurrentUser(): AuthUser? = firebaseAuth.currentUser?.toDomain()

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> =
        runCatching {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password)
            val user = result.user?.toDomain()
                ?: throw IllegalStateException("Sign in failed: user not found.")
            userRepository.createOrUpdateUserProfile(user)
            user
        }

    override suspend fun signUpWithEmail(email: String, password: String): Result<AuthUser> =
        runCatching {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password)
            val user = result.user?.toDomain()
                ?: throw IllegalStateException("Sign up failed: user not found.")
            userRepository.createOrUpdateUserProfile(user).getOrThrow()
            user
        }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthUser> =
        runCatching {
            val credential = GoogleAuthProvider.credential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential)
            val user = result.user?.toDomain()
                ?: throw IllegalStateException("Google sign in failed: user not found.")
            userRepository.createOrUpdateUserProfile(user)
            user
        }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    private fun FirebaseUser.toDomain(): AuthUser = AuthUser(
        uid = uid,
        displayName = displayName,
        email = email,
        photoUrl = photoURL
    )
}
