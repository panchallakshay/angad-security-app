package com.spidey.js.angad.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.spidey.js.angad.util.PreferencesManager
import com.spidey.js.angad.vpn.AngadVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefManager = PreferencesManager(context)
            CoroutineScope(Dispatchers.IO).launch {
                val shouldRestart = prefManager.autoRestart.first()
                val wasEnabled = prefManager.vpnEnabledState.first()
                
                if (shouldRestart && wasEnabled) {
                    val prepareIntent = VpnService.prepare(context)
                    if (prepareIntent == null) {
                        val serviceIntent = Intent(context, AngadVpnService::class.java)
                        context.startForegroundService(serviceIntent)
                    }
                }
            }
        }
    }
}
