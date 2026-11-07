package Com.fire_AI.`in`

import android.app.UiModeManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import Com.fire_AI.`in`.ui.AuthScreen
import Com.fire_AI.`in`.ui.ChatViewModel
import Com.fire_AI.`in`.ui.FireAiApp
import Com.fire_AI.`in`.ui.SettingsViewModel
import Com.fire_AI.`in`.ui.theme.FireAiTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    private val chatViewModel: ChatViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val credentialManager by lazy { CredentialManager.create(this) }
    private var isTvDevice = false
    private var offlineMode = false
    private var authLoading = false
    private var authError: String? = null

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            authLoading = false
            authError = "Google sign-in was cancelled. Choose an account to continue."
            renderUi()
            return@registerForActivityResult
        }
        lifecycleScope.launch {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                finishFirebaseSignIn(account)
            } catch (e: Exception) {
                authLoading = false
                authError = e.message ?: "Google account selection failed."
                renderUi()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uiMode = getSystemService(UI_MODE_SERVICE) as UiModeManager
        isTvDevice = uiMode.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        renderUi()
    }

    private fun signInWithGoogle() {
        authLoading = true
        authError = null
        renderUi()
        if (isTvDevice) {
            launchLegacyGooglePicker()
            return
        }
        lifecycleScope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setAutoSelectEnabled(false)
                    .build()
                val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
                val result = credentialManager.getCredential(this@MainActivity, request)
                val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
                finishFirebaseSignIn(credential.idToken)
            } catch (_: GetCredentialException) {
                launchLegacyGooglePicker()
            } catch (e: Exception) {
                authLoading = false
                authError = e.message ?: "Could not complete Google sign-in."
                renderUi()
            }
        }
    }

    private fun launchLegacyGooglePicker() {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(this, options).signOut().addOnCompleteListener {
            googleSignInLauncher.launch(GoogleSignIn.getClient(this, options).signInIntent)
        }
    }

    private suspend fun finishFirebaseSignIn(idToken: String?) {
        if (idToken.isNullOrBlank()) {
            authLoading = false
            authError = "Google did not return an ID token. Check Firebase OAuth and SHA-1 registration."
            renderUi()
            return
        }
        try {
            auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
            authError = null
        } catch (e: Exception) {
            authError = e.message ?: "Firebase rejected the Google account."
        } finally {
            authLoading = false
            renderUi()
        }
    }

    private suspend fun finishFirebaseSignIn(account: GoogleSignInAccount) {
        finishFirebaseSignIn(account.idToken)
    }

    private fun renderUi() { setContent { renderUiContent() } }

    @Composable
    private fun renderUiContent() {
        val user = auth.currentUser
        FireAiTheme(isTv = isTvDevice) {
            if (user != null || offlineMode) {
                FireAiApp(chatViewModel, settingsViewModel, isTvDevice)
            } else {
                AuthScreen(
                    isTv = isTvDevice,
                    isLoading = authLoading,
                    error = authError,
                    onGoogleSignIn = ::signInWithGoogle,
                    onContinueOffline = { offlineMode = true; renderUi() },
                )
            }
        }
    }
}
