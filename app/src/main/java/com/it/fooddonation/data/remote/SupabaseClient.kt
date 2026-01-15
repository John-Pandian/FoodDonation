package com.it.fooddonation.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {
    private const val SUPABASE_URL = "https://xrbjlvqjsumpbygswqtz.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_rxszxpbFEkZ9QN0kjkNO1Q_m71jdLvL"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Auth)
        install(Postgrest)
    }
}
