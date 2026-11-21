package com.jksalcedo.librefind.di

import android.content.Context
import com.jksalcedo.librefind.data.repository.SupabaseAppRepository
import com.jksalcedo.librefind.data.repository.SupabaseAuthRepository
import com.jksalcedo.librefind.domain.repository.AppRepository
import com.jksalcedo.librefind.domain.repository.AuthRepository
import com.russhwolf.settings.SharedPreferencesSettings
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import java.util.Properties

val supabaseModule = module {
    single {
        val context: Context = get()
        val sharedPrefs = context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)
        val settings = SharedPreferencesSettings(sharedPrefs)
        val properties = Properties()
        context.assets.open("supabase.properties").use { inputStream ->
            properties.load(inputStream)
        }

        createSupabaseClient(
            supabaseUrl = properties.getProperty("SUPABASE_URL"),
            supabaseKey = properties.getProperty("SUPABASE_KEY")
        ) {
            install(Auth) {
                scheme = "librefind"
                host = "login-callback"
                sessionManager = SettingsSessionManager(settings)
            }
            install(Postgrest)
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
                encodeDefaults = false
            })
        }
    }

    single<AuthRepository> { SupabaseAuthRepository(get()) }
    single<AppRepository> { SupabaseAppRepository(get(), get()) }
}
