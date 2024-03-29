package com.azercosmos.businesscarddatabase.utils

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.error.VolleyError
import com.azercosmos.businesscarddatabase.BuildConfig
import com.azercosmos.businesscarddatabase.activities.LoginActivity



class RequestManager {
    companion object {
        private var requestQueue: RequestQueue? = null
        private var sharedPreferences: SharedPreferences? = null

        private val DEFAULT_ADDRESS = "https://blog.azercosmos.dev/azercosmos-business-card-database/api/public"
        private val DEFAULT_LOGIN_ADDRESS = "https://blog.azercosmos.dev/azercosmos-auth/public"
        private val LOGIN_ADDRESS = "https://blog.azercosmos.dev/azercosmos-auth/public"
        private val API_ADDRESS = "https://blog.azercosmos.dev/azercosmos-business-card-database/api/public"

        fun setSharedPreferences (sharedPreferences: SharedPreferences) {
            this.sharedPreferences = sharedPreferences
        }

        fun setRequestQueue (requestQueue: RequestQueue) {
            this.requestQueue = requestQueue
        }

        fun <T> fillHeaders (request: Request<T>): Request<T> {
            // val headers = request.headers
            val headers: MutableMap<String, String> = mutableMapOf()

            headers.put("Authorization", sharedPreferences?.getString("token", "") as String)
            request.headers = headers
            return request
        }

        fun <T> sendRequest(request: Request<T>) {
            requestQueue?.add(fillHeaders(request))
        }

        fun getServerUrl(): String {
            return if(BuildConfig.DEBUG) sharedPreferences?.getString("api_address", DEFAULT_ADDRESS) ?: DEFAULT_ADDRESS
                    else API_ADDRESS
        }
        fun getLoginUrl(): String {
            return if(BuildConfig.DEBUG) sharedPreferences?.getString("login_address", DEFAULT_LOGIN_ADDRESS) ?: DEFAULT_LOGIN_ADDRESS
                    else LOGIN_ADDRESS
        }

        fun handleError (error: VolleyError, activity: Activity) {
            if (error.networkResponse.statusCode == 466) {
                val intent = Intent(activity, LoginActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                intent.putExtra("force", true)
                activity.startActivity(intent)
                activity.finish()
            }
        }

    }
}