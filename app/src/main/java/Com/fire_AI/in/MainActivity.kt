package Com.fire_AI.`in`

import android.app.UiModeManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import Com.fire_AI.`in`.ui.AuthScreen
import Com.fire_AI.`in`.ui.ChatViewModel
import Com.fire_AI.`in`.ui.SettingsViewModel
import Com.fire_AI.`in`.ui.TvAiApp
import Com.fire_AI.`in`.ui.theme.TvAiTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    private val chatViewModel: ChatViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val credentialManager by lazy { CredentialManager.create(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uiMode = getSystemService(UI_MODE_SERVICE) as UiModeManager
        val isTv = uiMode.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        setContent {
            TvAiTheme(isTv = isTv) {
                val user = auth.currentUser
                if (user != null || offlineMode) {
                    TvAiApp(chatVm = chatViewModel, settingsVm = settingsViewModel, isTv = isTv, signedInEmail = user?.email)
                } else {
                    AuthScreen(isTv, authLoading, authError, ::signInWithGoogle, { offlineMode = true; setContent { recreateUi() } })
                }
            }
        }
    }

    private var offlineMode = false
    private var authLoading = false
    private var authError: String? = null

    private fun signInWithGoogle() {
        authLoading = true
        authError = null
        setContent { recreateUi() }
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
                auth.signInWithCredential(GoogleAuthProvider.getCredential(credential.idToken, null)).await()
            } catch (e: GetCredentialException) {
                authError = e.message ?: "Google sign-in was cancelled or unavailable on this TV."
            } catch (e: Exception) {
                authError = e.message ?: "Could not complete Google sign-in."
            } finally {
                authLoading = false
                setContent { recreateUi() }
            }
        }
    }

    @Composable
    private fun recreateUi() {
        val uiMode = getSystemService(UI_MODE_SERVICE) as UiModeManager
        val isTv = uiMode.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        TvAiTheme(isTv = isTv) {
            val user = auth.currentUser
            if (user != null || offlineMode) TvAiApp(chatViewModel, settingsViewModel, isTv, user?.email)
            else AuthScreen(isTv, authLoading, authError, ::signInWithGoogle, { offlineMode = true; setContent { recreateUi() } })
        }
    }
}
