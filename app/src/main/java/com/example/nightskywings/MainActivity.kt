package com.example.nightskywings

import android.Manifest
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private var player: MediaPlayer? = null

    private lateinit var image: ImageView
    private lateinit var title: TextView
    private lateinit var fact: TextView
    private lateinit var play: Button

    // Load the card once the permission dialog is answered, so a granted location is used.
    private val askPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { loadCard() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Scheduler.scheduleAll(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        image = ImageView(this).apply { adjustViewBounds = true }
        title = TextView(this).apply { textSize = 22f; gravity = Gravity.CENTER }
        fact = TextView(this).apply { textSize = 16f; setPadding(0, 24, 0, 24) }
        play = Button(this).apply { text = "Play song"; isEnabled = false }
        root.addView(image); root.addView(title); root.addView(fact); root.addView(play)
        setContentView(ScrollView(this).apply { addView(root) })

        val wanted = buildList {
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (savedInstanceState == null) askPermissions.launch(wanted.toTypedArray()) else loadCard()
    }

    private fun loadCard() {
        if (Config.EBIRD_KEY.isBlank()) {
            title.text = "No eBird key set. Add ebird.key to local.properties and rebuild."
            return
        }
        title.text = "Loading bird of the day..."
        lifecycleScope.launch {
            val card = withContext(Dispatchers.IO) {
                val (lat, lon) = currentLatLon(this@MainActivity)
                val bird = BirdApi.nearby(this@MainActivity, lat, lon).randomOrNull()
                    ?: BirdApi.rareInLithuania(this@MainActivity).randomOrNull()
                bird?.let { BirdMedia.card(it) }
            }
            if (card == null) { title.text = "No birds found. Check your eBird key and connection."; return@launch }
            title.text = "${card.english}\n${card.lithuanian}"
            fact.text = card.fact ?: "No fact available."
            card.imageUrl?.let { image.load(it) }
            card.songUrl?.let { url ->
                play.isEnabled = true
                play.setOnClickListener {
                    player?.release()
                    player = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
                        )
                        setDataSource(url)
                        setOnPreparedListener { it.start() }
                        setOnErrorListener { _, _, _ -> play.text = "Song unavailable"; true }
                        prepareAsync()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}
