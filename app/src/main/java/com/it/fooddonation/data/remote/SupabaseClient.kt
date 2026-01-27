package com.it.fooddonation.data.remote

import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.cio.CIO

object SupabaseClient {
    private const val TAG = "SupabaseClient"
    private const val SUPABASE_URL = "https://xrbjlvqjsumpbygswqtz.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_rxszxpbFEkZ9QN0kjkNO1Q_m71jdLvL"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        // Use CIO engine for WebSocket support (required for Realtime)
        httpEngine = CIO.create()

        install(Auth) {
            Log.d(TAG, "Installing Auth plugin with autoLoadFromStorage=true, autoSaveToStorage=true, alwaysAutoRefresh=true")
            // Enable automatic session refresh
            autoLoadFromStorage = true
            autoSaveToStorage = true
            alwaysAutoRefresh = true
        }
        install(Postgrest)
        install(Storage)
        install(Realtime)
        Log.d(TAG, "Supabase client initialized with CIO engine, Auth, Postgrest, Storage, and Realtime")
    }
}
