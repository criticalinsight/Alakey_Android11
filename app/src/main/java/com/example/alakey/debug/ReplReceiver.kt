package com.example.alakey.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.alakey.data.UniversalRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * REPL-Driven Development Receiver.
 * Example ADB Command:
 * adb shell am broadcast -a com.example.alakey.REPL_EVAL -e cmd "refresh"
 * adb shell am broadcast -a com.example.alakey.REPL_EVAL -e cmd "subscribe https://feed.rss"
 */
@AndroidEntryPoint
class ReplReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: UniversalRepository
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.alakey.REPL_EVAL") {
            val cmd = intent.getStringExtra("cmd") ?: return
            Log.d("REPL", "Received command: $cmd")
            Toast.makeText(context, "REPL: $cmd", Toast.LENGTH_SHORT).show()
            
            evaluate(cmd, context)
        }
    }

    private fun evaluate(cmd: String, context: Context) {
        scope.launch {
            try {
                when {
                    cmd == "refresh" -> {
                        repository.syncAll()
                        Toast.makeText(context, "Refreshed all feeds", Toast.LENGTH_SHORT).show()
                    }
                    cmd.startsWith("subscribe ") -> {
                        val url = cmd.removePrefix("subscribe ").trim()
                        repository.subscribe(url)
                            .onSuccess { Toast.makeText(context, "Subscribed!", Toast.LENGTH_SHORT).show() }
                            .onFailure { Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_LONG).show() }
                    }
                    cmd == "nuke" -> {
                        // repository.nuke() // Dangerous, implement later if needed
                        Toast.makeText(context, "Nuke not implemented", Toast.LENGTH_SHORT).show()
                    }
                    cmd.startsWith("query-logs ") -> {
                        val type = cmd.removePrefix("query-logs ").trim()
                        val logs = repository.getLogsByType(type)
                        Log.i("REPL_RESULT", "--- Logs for $type ---")
                        logs.forEach { Log.i("REPL_RESULT", "[${it.status}] ${it.payload}") }
                        Toast.makeText(context, "Dumped ${logs.size} logs to Logcat (tag: REPL_RESULT)", Toast.LENGTH_LONG).show()
                    }
                    cmd.startsWith("grep-logs ") -> {
                        val q = cmd.removePrefix("grep-logs ").trim()
                        val logs = repository.grepLogs(q)
                        Log.i("REPL_RESULT", "--- Grep for '$q' ---")
                        logs.forEach { Log.i("REPL_RESULT", "[${it.type}] ${it.payload}") }
                        Toast.makeText(context, "Found ${logs.size} matches (tag: REPL_RESULT)", Toast.LENGTH_LONG).show()
                    }
                    cmd.startsWith("assert-fact ") -> {
                        val parts = cmd.removePrefix("assert-fact ").split(" ")
                        if (parts.size >= 3) {
                            val entityId = parts[0]
                            val attr = parts[1]
                            val value = parts.drop(2).joinToString(" ")
                            repository.assertFact(entityId, attr, value)
                            Toast.makeText(context, "Fact asserted: $attr", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.w("REPL", "Assert-fact usage: assert-fact <id> <attr> <val>")
                        }
                    }
                    cmd == "inspect-state" -> {
                        // This needs to be called on a ViewModel usually, 
                        // but we can dump the 'Facts' which is the source of truth.
                        val facts = repository.getAllFacts()
                        Log.i("REPL_RESULT", "--- Current Facts (Information Model) ---")
                        facts.forEach { Log.i("REPL_RESULT", "[:${it.attribute} ${it.entityId}] -> \"${it.value}\"") }
                        Toast.makeText(context, "Dumped Facts to Logcat", Toast.LENGTH_SHORT).show()
                    }
                    cmd.startsWith("exec-sql ") -> {
                         val sql = cmd.removePrefix("exec-sql ").trim()
                         try {
                             val results = repository.rawQuery(sql)
                             Log.i("REPL_RESULT", "--- SQL Result for: $sql ---")
                             results.forEach { Log.i("REPL_RESULT", it.toString()) }
                             Toast.makeText(context, "SQL Executed. Check Logcat.", Toast.LENGTH_LONG).show()
                         } catch (e: Exception) {
                             Log.e("REPL", "SQL Failed", e)
                             Toast.makeText(context, "SQL Error: ${e.message}", Toast.LENGTH_LONG).show()
                         }
                    }
                    else -> Log.w("REPL", "Unknown command: $cmd")
                }
            } catch (e: Exception) {
                Log.e("REPL", "Eval failed", e)
            }
        }
    }
}
