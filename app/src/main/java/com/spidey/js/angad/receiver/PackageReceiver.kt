package com.spidey.js.angad.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PackageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val packageName = intent.data?.schemeSpecificPart
        
        Log.d("PackageReceiver", "Action: $action | Package: $packageName")
        
        // In a more complex app, we would trigger a cache refresh here.
        // For Angad, the UI fetches fresh on each entry, and VpnService 
        // resolves UIDs live.
    }
}
