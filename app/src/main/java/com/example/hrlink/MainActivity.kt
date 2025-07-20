package com.example.hrlink

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var urlEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var webView: WebView
    private val client = OkHttpClient()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        urlEditText = findViewById(R.id.urlEditText)
        submitButton = findViewById(R.id.submitButton)
        webView = findViewById(R.id.webView)

        setupWebView()

        submitButton.setOnClickListener {
            val apiUrl = urlEditText.text.toString().trim()
            if (apiUrl.isNotEmpty()) {
                fetchAndSetCookies(apiUrl)
            } else {
                Toast.makeText(this, "لطفا لینک را وارد کنید", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
    }

    private fun fetchAndSetCookies(apiUrl: String) {
        val requestUrl = apiUrl.replace("/view/raw/", "/view/")
        val request = Request.Builder().url(requestUrl).build()

        submitButton.isEnabled = false
        Toast.makeText(this, "در حال بارگذاری...", Toast.LENGTH_SHORT).show()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "خطای شبکه: اتصال را بررسی کنید", Toast.LENGTH_LONG).show()
                    submitButton.isEnabled = true
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "لینک وارد شده نادرست است", Toast.LENGTH_LONG).show()
                        submitButton.isEnabled = true
                    }
                    return
                }

                val body = response.body?.string()
                try {
                    val mainResponse = gson.fromJson(body, MainResponse::class.java)
                    val jwtPayload = gson.fromJson(mainResponse.jwt, JwtPayload::class.java)
                    setSnappfoodCookies(jwtPayload)
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "انجام شد! در حال بارگذاری اسنپ‌فود...", Toast.LENGTH_LONG).show()
                        webView.loadUrl("https://m.snappfood.ir")
                        submitButton.isEnabled = true
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "خطا در پردازش اطلاعات", Toast.LENGTH_LONG).show()
                        submitButton.isEnabled = true
                    }
                }
            }
        })
    }

    private fun setSnappfoodCookies(payload: JwtPayload) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        val domain = "m.snappfood.ir"
        cookieManager.setCookie(domain, "jwt-access_token=${'$'}{payload.accessToken}; path=/")
        cookieManager.setCookie(domain, "jwt-token_type=${'$'}{payload.tokenType}; path=/")
        cookieManager.setCookie(domain, "jwt-refresh_token=${'$'}{payload.refreshToken}; path=/")
        cookieManager.setCookie(domain, "jwt-expires_in=${'$'}{payload.expiresIn}; path=/")
        cookieManager.setCookie(domain, "UserMembership=0; path=/")
        cookieManager.flush()
    }
}