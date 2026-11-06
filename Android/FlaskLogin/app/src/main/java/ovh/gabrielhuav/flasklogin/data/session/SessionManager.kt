package ovh.gabrielhuav.flasklogin.data.session

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "FlaskLoginPrefs"
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_ROLE = "user_role"
    }

    fun saveAuthSession(token: String, userId: Int, username: String, role: String) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putInt(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putString(KEY_ROLE, role)
            apply()
        }
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrBlank()
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun getUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }

    fun getUserId(): Int {
        return prefs.getInt(KEY_USER_ID, 0)
    }

    fun getRole(): String? {
        return prefs.getString(KEY_ROLE, "user")
    }

    fun isAdmin(): Boolean {
        return getRole() == "admin"
    }

    fun updateUsernameAndToken(newToken: String, newUsername: String) {
        prefs.edit().apply {
            putString(KEY_TOKEN, newToken)
            putString(KEY_USERNAME, newUsername)
            apply()
        }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
