package Com.fire_AI.`in`.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthScreen(isTv: Boolean, isLoading: Boolean, error: String?, onGoogleSignIn: () -> Unit, onContinueOffline: () -> Unit) {
    val outer = if (isTv) 56.dp else 24.dp
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF101A2C), Color(0xFF090C14), Color(0xFF030508)))), contentAlignment = Alignment.Center) {
        Surface(Modifier.fillMaxWidth(if (isTv) 0.58f else 0.94f), RoundedCornerShape(30.dp), color = Color(0xFF111827), tonalElevation = 8.dp) {
            Column(Modifier.padding(outer), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("FIRE AI", color = Color(0xFFFF7043), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Text("Your AI, ready for the big screen.", color = MaterialTheme.colorScheme.onSurface, fontSize = if (isTv) 34.sp else 28.sp, fontWeight = FontWeight.Bold)
                Text("Sign in to keep your Fire AI experience connected across devices. Your Gemini key stays protected on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 22.sp)
                Spacer(Modifier.height(10.dp))
                Button(onClick = onGoogleSignIn, enabled = !isLoading, modifier = Modifier.fillMaxWidth().height(if (isTv) 58.dp else 52.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF8FAFC), contentColor = Color(0xFF101828))) {
                    if (isLoading) CircularProgressIndicator(color = Color(0xFF101828), strokeWidth = 2.dp)
                    else { Text("G", fontWeight = FontWeight.Bold, fontSize = 20.sp); Spacer(Modifier.width(12.dp)); Text("Continue with Google", fontWeight = FontWeight.SemiBold) }
                }
                if (!error.isNullOrBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, lineHeight = 19.sp)
                OutlinedButton(onClick = onContinueOffline, enabled = !isLoading, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(18.dp)) { Text("Continue without an account") }
                Text("Firebase Authentication secures the account session. Offline mode keeps local chat and settings available.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f), fontSize = 12.sp)
            }
        }
    }
}
