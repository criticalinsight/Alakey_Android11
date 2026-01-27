package com.example.alakey.repl

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.alakey.data.UniversalRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReplReceiver : BroadcastReceiver() {

    @Inject lateinit var repo: UniversalRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.alakey.FACT") {
            val entityId = intent.getStringExtra("entity_id")
            val attr = intent.getStringExtra("attr")
            val value = intent.getStringExtra("val")

            if (entityId != null && attr != null && value != null) {
                scope.launch {
                    repo.assertFact(entityId, attr, value)
                    Log.d("ReplReceiver", "Fact asserted: $entityId -> $attr = $value")
                }
            } else {
                Log.w("ReplReceiver", "Malformed FACT broadcast. Need entity_id, attr, val.")
            }
        }
    }
}
