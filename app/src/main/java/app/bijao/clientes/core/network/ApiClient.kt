package app.bijao.clientes.core.network

import app.bijao.clientes.core.AppConfig
import app.bijao.clientes.core.auth.AuthManager
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Cliente HTTP contra el backend FastAPI compartido (rutas bajo `/api/app`) --
 * equivalente de `APIClient.swift` en bijao-clientes-ios. El token de la
 * sesión de Supabase se agrega acá mismo vía interceptor, no en cada
 * llamada: ningún endpoint de `ApiService` necesita saberlo.
 */
object ApiClient {
    private val json = Json { ignoreUnknownKeys = true }

    private val authInterceptor = Interceptor { chain ->
        val token = AuthManager.accessToken
        val request = chain.request().newBuilder().apply {
            if (token != null) addHeader("Authorization", "Bearer $token")
        }.build()
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
        .build()

    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(AppConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }
}
