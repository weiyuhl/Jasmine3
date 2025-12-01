package com.lhzkml.jasmine.di

import androidx.room.Room
import io.ktor.http.HttpHeaders
import io.pebbletemplates.pebble.PebbleEngine
import kotlinx.serialization.json.Json
import com.lhzkmlai.provider.ProviderManager
import com.lhzkmlcommon.http.AcceptLanguageBuilder
import com.lhzkml.jasmine.BuildConfig
import com.lhzkml.jasmine.data.ai.AIRequestInterceptor
import com.lhzkml.jasmine.data.ai.transformers.AssistantTemplateLoader
import com.lhzkml.jasmine.data.ai.GenerationHandler
import com.lhzkml.jasmine.data.ai.transformers.TemplateTransformer
import com.lhzkml.jasmine.data.api.jasmineAPI
import com.lhzkml.jasmine.data.datastore.SettingsStore
import com.lhzkml.jasmine.data.db.AppDatabase
import com.lhzkml.jasmine.data.db.Migration_6_7
import com.lhzkml.jasmine.data.ai.mcp.McpManager
import com.lhzkml.jasmine.data.sync.WebdavSync
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.Locale
import java.util.concurrent.TimeUnit

val dataSourceModule = module {
    single {
        SettingsStore(context = get(), scope = get())
    }

    single {
        Room.databaseBuilder(get(), AppDatabase::class.java, "jasmine")
            .addMigrations(Migration_6_7)
            .build()
    }

    single {
        AssistantTemplateLoader(settingsStore = get())
    }

    single {
        PebbleEngine.Builder()
            .loader(get<AssistantTemplateLoader>())
            .defaultLocale(Locale.getDefault())
            .autoEscaping(false)
            .build()
    }

    single { TemplateTransformer(engine = get(), settingsStore = get()) }

    single {
        get<AppDatabase>().conversationDao()
    }

    single {
        get<AppDatabase>().memoryDao()
    }

    single {
        get<AppDatabase>().genMediaDao()
    }

    single { McpManager(settingsStore = get(), appScope = get()) }

    single {
        GenerationHandler(
            context = get(),
            providerManager = get(),
            json = get(),
            memoryRepo = get(),
            conversationRepo = get(),
            aiLoggingManager = get()
        )
    }

    single<OkHttpClient> {
        val acceptLang = AcceptLanguageBuilder.fromAndroid(get())
            .build()
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(120, TimeUnit.SECONDS)
            .followSslRedirects(true)
            .followRedirects(true)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader(HttpHeaders.AcceptLanguage, acceptLang)
                    .addHeader(HttpHeaders.UserAgent, "jasmine-Android/${BuildConfig.VERSION_NAME}")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(AIRequestInterceptor(remoteConfig = get()))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            })
            .build()
    }

    single {
        ProviderManager(client = get())
    }

    single {
        WebdavSync(settingsStore = get(), json = get(), context = get())
    }

    single<Retrofit> {
        Retrofit.Builder()
            .baseUrl("https://api.rikka-ai.com")
            .addConverterFactory(get<Json>().asConverterFactory("application/json; charset=UTF8".toMediaType()))
            .build()
    }

    single<jasmineAPI> {
        get<Retrofit>().create(jasmineAPI::class.java)
    }
}
