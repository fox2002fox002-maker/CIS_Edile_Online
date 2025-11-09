package com.csi.edile
import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
class MainActivity : ComponentActivity() { override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { AppScreen() } } }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen() {
  val db = remember { FirebaseFirestore.getInstance() }
  var date by remember { mutableStateOf(today()) }
  var worker by remember { mutableStateOf("") }
  var hours by remember { mutableStateOf("9") }
  var expense by remember { mutableStateOf("0") }
  var notes by remember { mutableStateOf("") }
  var entries by remember { mutableStateOf(listOf<Entry>()) }
  LaunchedEffect(Unit) {
    entries = db.collection("entries").orderBy("date").get().await().documents.map { d ->
      Entry(id=d.id, date=d.getString("date")?:"", worker=d.getString("worker")?:"", hours=(d.getLong("hours")?:0L).toInt(), expense=(d.getDouble("expense")?:0.0), notes=d.getString("notes")?:"")
    }
  }
  Scaffold(topBar = { TopAppBar(title = { Text("C.I.S. Edile") }, actions = {
    TextButton(onClick = { exportPdf(LocalContext.current, entries) }) { Text("تصدير PDF") }
    TextButton(onClick = { FirebaseAuth.getInstance().signOut() }) { Text("خروج") }
  })}) { padding ->
    Column(Modifier.padding(padding).padding(16.dp)) {
      OutlinedTextField(value=date, onValueChange={date=it}, label={ Text("التاريخ (dd/MM/yyyy)") }, modifier=Modifier.fillMaxWidth())
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(value=worker, onValueChange={worker=it}, label={ Text("اسم العامل") }, modifier=Modifier.fillMaxWidth())
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(value=hours, onValueChange={hours=it}, label={ Text("عدد الساعات") }, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), modifier=Modifier.fillMaxWidth())
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(value=expense, onValueChange={expense=it}, label={ Text("المصاريف") }, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal), modifier=Modifier.fillMaxWidth())
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(value=notes, onValueChange={notes=it}, label={ Text("ملاحظات") }, modifier=Modifier.fillMaxWidth())
      Spacer(Modifier.height(12.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
          val entry = hashMapOf("date" to date, "worker" to worker, "hours" to (hours.toIntOrNull()?:0), "expense" to (expense.toDoubleOrNull()?:0.0), "notes" to notes)
          db.collection("entries").add(entry).addOnSuccessListener {
            db.collection("entries").orderBy("date").get().addOnSuccessListener { qs ->
              entries = qs.documents.map { d -> Entry(id=d.id, date=d.getString("date")?:"", worker=d.getString("worker")?:"", hours=(d.getLong("hours")?:0L).toInt(), expense=(d.getDouble("expense")?:0.0), notes=d.getString("notes")?:"") }
            }
          }
        }) { Text("إضافة") }
      }
      Spacer(Modifier.height(16.dp))
      Text("السجلات", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp))
      LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(entries) { e -> ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
          Text("${e.date} — ${e.worker}"); Text("ساعات: ${e.hours} | مصاريف: ${e.expense}"); if (e.notes.isNotBlank()) Text("ملاحظات: ${e.notes}")
        } } }
      }
    }
  }
}
fun today(): String { val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()); return sdf.format(Date()) }
fun exportPdf(context: Context, entries: List<Entry>) {
  val doc = PdfDocument(); val pageInfo = PdfDocument.PageInfo.Builder(595,842,1).create(); val page = doc.startPage(pageInfo); val c = page.canvas
  val paint = android.graphics.Paint().apply { textSize = 14f; isAntiAlias = true }
  var y = 40f; c.drawText("C.I.S. Edile - تقرير", 40f, y, paint); y+=20f; c.drawText("التاريخ: "+today(), 40f, y, paint); y+=30f
  c.drawText("Data | Worker | Hours | Expense | Notes", 40f, y, paint); y+=18f
  entries.forEach { val line = "${it.date} | ${it.worker} | ${it.hours} | ${it.expense} | ${it.notes}"; c.drawText(line.take(95), 40f, y, paint); y+=16f }
  doc.finishPage(page); val out = File(context.getExternalFilesDir(null), "backup_"+today().replace("/","-")+".pdf"); doc.writeTo(java.io.FileOutputStream(out)); doc.close()
}