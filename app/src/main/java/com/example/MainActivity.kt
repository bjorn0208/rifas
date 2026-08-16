package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.RaffleRepository
import com.example.ui.RaffleApp
import com.example.ui.RaffleViewModel
import com.example.ui.RaffleViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private lateinit var viewModel: RaffleViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Room DB, Repository and ViewModel
    val database = AppDatabase.getDatabase(applicationContext)
    val repository = RaffleRepository(database.raffleDao())
    val factory = RaffleViewModelFactory(repository)
    viewModel = ViewModelProvider(this, factory)[RaffleViewModel::class.java]

    // Parse deep link if starting via deep link
    handleRaffleDeepLink(intent)

    setContent {
      MyApplicationTheme {
        RaffleApp(viewModel = viewModel)
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleRaffleDeepLink(intent)
  }

  private fun handleRaffleDeepLink(intent: Intent?) {
    val data: Uri? = intent?.data
    if (data != null) {
      Log.d("MainActivity", "Received deep link data: $data")
      val raffleIdStr = data.getQueryParameter("id")
      if (raffleIdStr != null) {
        try {
          val raffleId = raffleIdStr.toInt()
          Log.d("MainActivity", "Selecting raffle ID from deep link: $raffleId")
          viewModel.selectRaffle(raffleId)
          // Always switch to PUBLIC mode for external users clicking links
          viewModel.setAdminMode(false)
        } catch (e: NumberFormatException) {
          Log.e("MainActivity", "Error parsing raffle ID from link", e)
        }
      }
      val ref = data.getQueryParameter("ref")
      if (!ref.isNullOrBlank()) {
        Log.d("MainActivity", "Setting referrer from deep link: $ref")
        viewModel.setReferrer(ref)
      }
    }
  }
}
