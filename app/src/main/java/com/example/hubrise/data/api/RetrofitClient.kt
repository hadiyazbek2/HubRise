package com.example.hubrise.data.api

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.hubrise.data.local.UserPreferences
import com.example.hubrise.data.model.RefreshTokenRequest
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // Emulator routes 10.0.2.2 → host machine localhost
    private const val EMULATOR_URL = "http://10.0.2.2:8000/"

    // Real device: replace with your computer's LAN IP
    private const val DEVICE_URL = "http://192.168.3.230:8000/"

    private val BASE_URL: String
        get() = if (isEmulator()) EMULATOR_URL else DEVICE_URL

    // Application context — set once via init() from Application class or MainActivity
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.FINGERPRINT.contains("test-keys")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MODEL.contains("sdk_gphone")
            || Build.MANUFACTURER.contains("Genymotion")
            || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
            || Build.PRODUCT == "google_sdk"
            || Build.PRODUCT.startsWith("sdk")
            || Build.PRODUCT.contains("emulator")
            || Build.HARDWARE == "goldfish"
            || Build.HARDWARE == "ranchu")
    }

    // ---- Interceptor: attach Bearer token to every request ----
    private val tokenInterceptor = Interceptor { chain ->
        val prefs = UserPreferences(appContext)
        val token = runBlocking { prefs.accessToken.first() }
        val request = if (!token.isNullOrEmpty()) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    // ---- Authenticator: refresh token on 401, redirect to login on failure ----
    private val tokenAuthenticator = object : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            // Avoid infinite loop: if refresh request itself 401s, give up
            if (response.request.url.encodedPath.contains("token/refresh")) return null

            val prefs = UserPreferences(appContext)
            val refreshToken = runBlocking { prefs.refreshToken.first() }
                ?: return navigateToLogin()

            // Synchronous refresh call using a separate no-auth Retrofit instance
            return try {
                val refreshService = noAuthRetrofit().create(AuthApiService::class.java)
                val refreshResponse = runBlocking {
                    refreshService.refreshToken(RefreshTokenRequest(refreshToken))
                }
                // Save new access token
                runBlocking { prefs.saveAccessToken(refreshResponse.access) }

                // Retry the original request with the new token
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${refreshResponse.access}")
                    .build()
            } catch (e: Exception) {
                navigateToLogin()
                null
            }
        }

        private fun navigateToLogin(): Request? {
            runBlocking { UserPreferences(appContext).clearAll() }
            // Post to main thread to start LoginActivity
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                val intent = Intent(appContext, com.example.hubrise.ui.auth.login.LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                appContext.startActivity(intent)
            }
            return null
        }
    }

    private fun getLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    // OkHttpClient WITH auth interceptor and authenticator (used for all authenticated calls)
    private fun getOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(getLoggingInterceptor())
            .addInterceptor(tokenInterceptor)
            .authenticator(tokenAuthenticator)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    // OkHttpClient WITHOUT auth — used only for the token refresh call to avoid infinite loop
    private fun noAuthOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(getLoggingInterceptor())
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private fun noAuthRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(noAuthOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Auth service uses no-auth client (login/signup/social should not have the token interceptor)
    fun getAuthApiService(): AuthApiService {
        return noAuthRetrofit().create(AuthApiService::class.java)
    }

    fun getFeedApiService(): FeedApiService {
        return getRetrofit().create(FeedApiService::class.java)
    }

    fun getHubApiService(): HubApiService {
        return getRetrofit().create(HubApiService::class.java)
    }

    /** Converts a relative Django media path (e.g. /media/img.jpg) to an absolute URL. */
    fun absoluteUrl(path: String?): String? {
        if (path.isNullOrEmpty()) return null
        return if (path.startsWith("http")) path else BASE_URL.trimEnd('/') + path
    }
}
