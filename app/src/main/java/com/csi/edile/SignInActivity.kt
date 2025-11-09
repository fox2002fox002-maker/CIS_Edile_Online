package com.csi.edile
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
class SignInActivity : ComponentActivity() {
  private val auth by lazy { FirebaseAuth.getInstance() }
  private val db by lazy { FirebaseFirestore.getInstance() }
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().requestIdToken(getString(R.string.default_web_client_id)).build()
    val client = GoogleSignIn.getClient(this, gso)
    val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
      val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(res.data)
      try {
        val account = task.getResult(ApiException::class.java)
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnSuccessListener { r ->
          val user = r.user!!
          val doc = db.collection("users").document(user.uid)
          doc.get().addOnSuccessListener { snap ->
            if (!snap.exists()) { doc.set(com.csi.edile.UserProfile(uid=user.uid, email=user.email?:"", displayName=user.displayName?:"", role="admin")) }
            startActivity(Intent(this, MainActivity::class.java)); finish()
          }
        }
      } catch (e: Exception) { e.printStackTrace() }
    }
    setContent {
      MaterialTheme { Surface { Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("C.I.S. Edile", style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(16.dp))
        Button(onClick = { launcher.launch(client.signInIntent) }) { Text("تسجيل الدخول بـ Google") }
      } } }
    }
    auth.currentUser?.let { startActivity(Intent(this, MainActivity::class.java)); finish() }
  }
}