package com.tthih.yu.campuscard

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.io.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

/**
 * A persistent CookieJar implementation that saves cookies to SharedPreferences.
 * Cookies are stored per host.
 * Note: This is a simplified implementation. For production, consider edge cases like cookie expiration handling.
 */
class PersistentCookieJar(context: Context) : CookieJar {

    private val TAG = "PersistentCookieJar"
    private val COOKIE_PREFS = "OkHttp_Cookie_Prefs"
    private val prefs: SharedPreferences = context.getSharedPreferences(COOKIE_PREFS, Context.MODE_PRIVATE)

    // In-memory cache for faster access, backed by SharedPreferences
    private val cookieCache: HashMap<String, MutableSet<Cookie>> = HashMap()

    init {
        loadAllFromPrefs()
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val mutableCookies = cookieCache.getOrPut(host) { HashSet() }

        // Update or add new cookies, remove expired ones implicitly by overwriting
        val newCookies = cookies.filter { !isExpired(it) }
        
        // Create a map of new cookies for efficient lookup
        val newCookiesMap = newCookies.associateBy { cookieKey(it) }

        // Remove old versions of the new cookies and any expired cookies from the current set
        val iterator = mutableCookies.iterator()
        while (iterator.hasNext()) {
            val oldCookie = iterator.next()
            if (isExpired(oldCookie) || newCookiesMap.containsKey(cookieKey(oldCookie))) {
                iterator.remove()
            }
        }
        
        // Add the new, valid cookies
        mutableCookies.addAll(newCookies)

        // Persist the updated set for the host
        saveToPrefs(host, mutableCookies)
        Log.d(TAG, "Saved ${newCookies.size} cookies for host: $host")
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val cookiesForHost = cookieCache[host] ?: return emptyList()

        // Filter out expired cookies before returning
        val validCookies = cookiesForHost.filter { cookie ->
            !isExpired(cookie) && cookie.matches(url)
        }.toMutableList() // Make mutable for potential removal

        // Optional: Remove expired cookies found during load from cache and prefs
        val expired = cookiesForHost.filter { isExpired(it) }
        if (expired.isNotEmpty()) {
            Log.d(TAG, "Removing ${expired.size} expired cookies for host: $host during load")
            cookiesForHost.removeAll(expired)
            saveToPrefs(host, cookiesForHost)
        }
        
        Log.d(TAG, "Loaded ${validCookies.size} cookies for host: $host")
        return validCookies
    }

    @Synchronized
    fun clear() {
        cookieCache.clear()
        prefs.edit().clear().apply()
        Log.i(TAG, "Cleared all persistent cookies.")
    }
    
    @Synchronized
    fun clearSession() {
        // Clears non-persistent cookies (implementation depends on how persistence is marked)
        // For this simple implementation, we might assume all saved cookies are persistent
        // or add logic based on cookie.persistent flag if needed.
        // A simpler approach for now might be the same as clear().
        clear() 
        Log.i(TAG, "Cleared session cookies (currently clears all).")
    }


    private fun saveToPrefs(host: String, cookies: Set<Cookie>) {
        val editor = prefs.edit()
        // Store cookies as a Set of serialized strings
        val serializedCookies = cookies.mapNotNull { serializeCookie(it) }.toSet()
        editor.putStringSet(host, serializedCookies)
        editor.apply()
    }

    private fun loadAllFromPrefs() {
        val allEntries = prefs.all
        for ((host, value) in allEntries) {
            if (value is Set<*>) {
                val cookies = HashSet<Cookie>()
                (value as? Set<String>)?.forEach { serializedCookie ->
                    deserializeCookie(serializedCookie)?.let { cookie ->
                        // Basic check for expiration on load
                        if (!isExpired(cookie)) {
                            cookies.add(cookie)
                        } else {
                            Log.d(TAG, "Ignoring expired cookie on load: ${cookie.name}")
                        }
                    }
                }
                 if (cookies.isNotEmpty()) {
                    cookieCache[host] = cookies
                    Log.d(TAG, "Loaded ${cookies.size} cookies from prefs for host: $host")
                }
            }
        }
    }
    
     private fun cookieKey(cookie: Cookie): String {
        return "${cookie.name};${cookie.domain}" // Simple key based on name and domain
    }

    private fun isExpired(cookie: Cookie): Boolean {
        return cookie.expiresAt < System.currentTimeMillis()
    }

    // --- Serialization/Deserialization ---

    private fun serializeCookie(cookie: Cookie): String? {
        val bos = ByteArrayOutputStream()
        try {
            ObjectOutputStream(bos).use { oos ->
                oos.writeObject(SerializableOkHttpCookie(cookie))
            }
            return Base64.getEncoder().encodeToString(bos.toByteArray())
        } catch (e: IOException) {
            Log.e(TAG, "Error serializing cookie: ${cookie.name}", e)
            return null
        }
    }

    private fun deserializeCookie(serialized: String): Cookie? {
        val bytes = try {
             Base64.getDecoder().decode(serialized)
        } catch (e: IllegalArgumentException) {
             Log.e(TAG, "Base64 decoding failed for cookie string.", e)
             return null // Return null if Base64 decoding fails
        }
        val bis = ByteArrayInputStream(bytes)
        try {
            ObjectInputStream(bis).use { ois ->
                val serializableCookie = ois.readObject() as? SerializableOkHttpCookie
                return serializableCookie?.getCookie()
            }
        } catch (e: Exception) { // Catch broader exceptions during deserialization
            Log.e(TAG, "Error deserializing cookie", e)
            return null
        }
    }

    /**
     * A wrapper class to make OkHttp's Cookie serializable.
     * Needed because OkHttp's Cookie class itself is not Serializable.
     */
    private class SerializableOkHttpCookie(
        @Transient private var cookie: Cookie? = null
    ) : Serializable {

        companion object {
            private const val serialVersionUID: Long = -8594045714036645534L
        }

        // Fields to store cookie data
        private var name: String? = null
        private var value: String? = null
        private var expiresAt: Long = 0
        private var domain: String? = null
        private var path: String? = null
        private var secure: Boolean = false
        private var httpOnly: Boolean = false
        private var hostOnly: Boolean = false
        // Note: We are not serializing 'persistent' flag as its meaning can be ambiguous
        // in this context. We treat all saved cookies as potentially persistent.

        init {
            this.cookie?.let {
                this.name = it.name
                this.value = it.value
                this.expiresAt = it.expiresAt
                this.domain = it.domain
                this.path = it.path
                this.secure = it.secure
                this.httpOnly = it.httpOnly
                this.hostOnly = it.hostOnly
            }
        }

        fun getCookie(): Cookie? {
            val builder = Cookie.Builder()
            name?.let { builder.name(it) }
            value?.let { builder.value(it) }
            builder.expiresAt(expiresAt)
            domain?.let { dom ->
                 if (hostOnly) {
                    builder.hostOnlyDomain(dom)
                } else {
                    builder.domain(dom)
                }
            }
            path?.let { builder.path(it) }
            if (secure) builder.secure()
            if (httpOnly) builder.httpOnly()

            // Basic validation before building
             if (name.isNullOrEmpty() || domain.isNullOrEmpty()) {
                 Log.w("PersistentCookieJar", "Deserialized cookie missing name or domain, skipping build.")
                 return null
            }

            return try {
                 builder.build()
            } catch (e: Exception) {
                 Log.e("PersistentCookieJar", "Failed to build cookie from deserialized data", e)
                 null // Return null if building fails
            }
        }

        // Custom serialization logic
        @Throws(IOException::class)
        private fun writeObject(out: ObjectOutputStream) {
             out.writeObject(name)
             out.writeObject(value)
             out.writeLong(expiresAt)
             out.writeObject(domain)
             out.writeObject(path)
             out.writeBoolean(secure)
             out.writeBoolean(httpOnly)
             out.writeBoolean(hostOnly)
        }

        // Custom deserialization logic
        @Throws(IOException::class, ClassNotFoundException::class)
        private fun readObject(ois: ObjectInputStream) {
            name = ois.readObject() as? String
            value = ois.readObject() as? String
            expiresAt = ois.readLong()
            domain = ois.readObject() as? String
            path = ois.readObject() as? String
            secure = ois.readBoolean()
            httpOnly = ois.readBoolean()
            hostOnly = ois.readBoolean()
        }
    }
} 