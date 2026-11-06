package ovh.gabrielhuav.flasklogin.data.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response
import ovh.gabrielhuav.flasklogin.data.session.SessionManager

class AuthInterceptor(context: Context) : Interceptor {
    private val sessionManager = SessionManager(context)

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = sessionManager.getToken()

        return if (!token.isNullOrBlank()) {
            val authenticatedRequest = original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(authenticatedRequest)
        } else {
            chain.proceed(original)
        }
    }
}
