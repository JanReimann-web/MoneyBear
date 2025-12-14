package com.jan.moneybear

import android.app.Application
import com.google.firebase.FirebaseApp
import com.jan.moneybear.billing.BillingManager
import com.jan.moneybear.data.local.AppDb
import com.jan.moneybear.data.local.provideDb
import com.jan.moneybear.data.store.SettingsStore
import com.jan.moneybear.data.repository.AuthRepository
import com.jan.moneybear.domain.SyncRepository
import com.jan.moneybear.domain.TransactionRepository

class MoneyBearApp : Application() {

    val database: AppDb by lazy { provideDb(this) }

    val settingsStore: SettingsStore by lazy { SettingsStore(this, database.settings()) }

    val authRepository: AuthRepository by lazy { AuthRepository(this) }

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(database.tx())
    }

    val syncRepository: SyncRepository by lazy {
        SyncRepository(
            remote = com.jan.moneybear.data.remote.NoOpRemoteDataSource(),
            dao = database.tx(),
            settings = settingsStore
        )
    }

    val billingManager: BillingManager by lazy { BillingManager(this) }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        billingManager.connect()
    }
}




















