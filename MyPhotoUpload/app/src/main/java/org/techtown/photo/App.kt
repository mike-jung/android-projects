package org.techtown.photo

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import net.gotev.uploadservice.BuildConfig
import net.gotev.uploadservice.UploadServiceConfig
import net.gotev.uploadservice.UploadServiceConfig.retryPolicy
import net.gotev.uploadservice.data.RetryPolicyConfig
import net.gotev.uploadservice.network.HttpRequest
import net.gotev.uploadservice.network.HttpStack
import net.gotev.uploadservice.observer.request.GlobalRequestObserver
import net.gotev.uploadservice.okhttp.OkHttpStackRequest
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*


/**
 * Base Application class
 *
 * 1. 파일 업로드 라이브러리를 위한 초기화
 *
 */
class App : Application() {

    companion object {
        val TAG = "App"
        const val notificationChannelID = "UploadServiceChannel"

        val serverUrl = "http://172.168.10.95:8001"

        fun getHttpClient(): OkHttpClient {

            val x509TrustManager: X509TrustManager = object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate?> {
                    return arrayOfNulls(0)
                }

                @Throws(CertificateException::class)
                override fun checkServerTrusted(
                    chain: Array<X509Certificate>,
                    authType: String
                ) {
                    Log.d(TAG, ": authType: $authType")
                }

                @Throws(CertificateException::class)
                override fun checkClientTrusted(
                    chain: Array<X509Certificate>,
                    authType: String
                ) {
                    Log.d(TAG, ": authType: $authType")
                }
            }

            val okHttpClientBuilder = OkHttpClient.Builder()
            try {
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, SecureRandom())
                val sslSocketFactory = sslContext.socketFactory
                okHttpClientBuilder.sslSocketFactory(sslSocketFactory, x509TrustManager)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            okHttpClientBuilder.hostnameVerifier(RelaxedHostNameVerifier())
            return okHttpClientBuilder.build()
        }

        private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }

            @Throws(CertificateException::class)
            override fun checkClientTrusted(
                chain: Array<X509Certificate>,
                authType: String
            ) {
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(
                chain: Array<X509Certificate>,
                authType: String
            ) {
            }
        })

        class RelaxedHostNameVerifier : HostnameVerifier {
            override fun verify(hostname: String, session: SSLSession): Boolean {
                return true
            }
        }

    }

    // Customize the notification channel as you wish. This is only for a bare minimum example
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                notificationChannelID,
                "Upload Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Config 초기화
        UploadServiceConfig.initialize(
            context = this,
            defaultNotificationChannel = notificationChannelID,
            debug = BuildConfig.DEBUG
        )


        UploadServiceConfig.httpStack = MyHttpStack(getHttpClient())
        //UploadServiceConfig.httpStack = getOkHttpClient() as HttpStack

        // 알림 채널 설정
        createNotificationChannel()

        // 재시도 정책
        retryPolicy = RetryPolicyConfig(1, 10, 2, 3)

        // 옵저버 설정
        GlobalRequestObserver(this, GlobalUploadObserver())

    }

    inner class MyHttpStack(
        private val client: OkHttpClient =
            OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .cache(null)
                .addInterceptor(object : Interceptor {
                    override fun intercept(chain: Interceptor.Chain): Response {
                        val request = chain.request().newBuilder()
                            .header("User-Agent", UploadServiceConfig.defaultUserAgent)
                            .build()
                        return chain.proceed(request)
                    }
                })
                .build()
    ) : HttpStack {
        @Throws(IOException::class)
        override fun newRequest(uploadId: String, method: String, url: String): HttpRequest {
            return OkHttpStackRequest(uploadId, client, method, url)
        }
    }


}