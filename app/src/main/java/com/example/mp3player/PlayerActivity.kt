package com.example.simplemp3player

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import com.example.mp3player.R

class PlayerActivity : AppCompatActivity() {

    private lateinit var trackTitle: TextView
    private lateinit var trackArtist: TextView
    private lateinit var albumArt: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var btnPlay: Button
    private lateinit var btnPause: Button
    private lateinit var btnPrevious: Button
    private lateinit var btnNext: Button
    private lateinit var seekBar: SeekBar
    private lateinit var currentTimeText: TextView
    private lateinit var durationTimeText: TextView

    private var mediaPlayer: MediaPlayer? = null
    private var currentPosition = 0
    private var isPlaying = false
    private var songList: ArrayList<String> = ArrayList()
    private var songUris: ArrayList<Uri> = ArrayList()

    private val handler = Handler(Looper.getMainLooper())
    private var updateSeekBarRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        initViews()
        getDataFromIntent()
        setupClickListeners()
        playSong(currentPosition)
    }

    private fun initViews() {
        trackTitle = findViewById(R.id.trackTitle)
        trackArtist = findViewById(R.id.trackArtist)
        albumArt = findViewById(R.id.albumArt)
        btnBack = findViewById(R.id.btnBack)
        btnPlay = findViewById(R.id.btnPlay)
        btnPause = findViewById(R.id.btnPause)
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)
        seekBar = findViewById(R.id.seekBar)
        currentTimeText = findViewById(R.id.currentTime)
        durationTimeText = findViewById(R.id.durationTime)
    }

    private fun getDataFromIntent() {
        currentPosition = intent.getIntExtra("song_position", 0)
        trackTitle.text = intent.getStringExtra("song_title") ?: "Неизвестно"
        songList = intent.getStringArrayListExtra("song_list") ?: ArrayList()
        songUris = intent.getParcelableArrayListExtra("song_uris") ?: ArrayList()

        trackArtist.text = "Песня ${currentPosition + 1} из ${songList.size}"
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnPlay.setOnClickListener {
            if (mediaPlayer != null && !isPlaying) {
                mediaPlayer?.start()
                isPlaying = true
                startUpdatingSeekBar()
                Toast.makeText(this, "Воспроизведение", Toast.LENGTH_SHORT).show()
            }
        }

        btnPause.setOnClickListener {
            if (mediaPlayer != null && isPlaying) {
                mediaPlayer?.pause()
                isPlaying = false
                Toast.makeText(this, "Пауза", Toast.LENGTH_SHORT).show()
            }
        }

        btnPrevious.setOnClickListener {
            if (songList.isNotEmpty()) {
                currentPosition = if (currentPosition > 0) currentPosition - 1 else songList.size - 1
                playSong(currentPosition)
            }
        }

        btnNext.setOnClickListener {
            if (songList.isNotEmpty()) {
                currentPosition = if (currentPosition < songList.size - 1) currentPosition + 1 else 0
                playSong(currentPosition)
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun loadAlbumArt(uri: Uri) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, uri)

            val artBytes = retriever.embeddedPicture

            if (artBytes != null) {
                val bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                albumArt.setImageBitmap(bitmap)
                albumArt.setPadding(0, 0, 0, 0)
                albumArt.scaleType = ImageView.ScaleType.CENTER_CROP
            } else {
                setDefaultAlbumArt()
            }

            retriever.release()
        } catch (e: Exception) {
            e.printStackTrace()
            setDefaultAlbumArt()
        }
    }

    private fun setDefaultAlbumArt() {
        albumArt.setImageResource(android.R.drawable.ic_dialog_info)
        albumArt.setPadding(40, 40, 40, 40)
        albumArt.scaleType = ImageView.ScaleType.FIT_CENTER
    }

    private fun playSong(position: Int) {
        try {
            mediaPlayer?.release()
            mediaPlayer = null

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@PlayerActivity, songUris[position])
                prepare()
                start()


                setOnCompletionListener {
                    runOnUiThread {

                        Toast.makeText(this@PlayerActivity, "Следующий трек", Toast.LENGTH_SHORT).show()

                        this@PlayerActivity.currentPosition = if (currentPosition < songList.size - 1) {
                            currentPosition + 1
                        } else {
                            0
                        }


                        playSong(currentPosition)
                    }
                }
            }

            isPlaying = true
            trackTitle.text = songList[position]
            trackArtist.text = "Песня ${position + 1} из ${songList.size}"

            loadAlbumArt(songUris[position])

            val duration = mediaPlayer?.duration ?: 0
            seekBar.max = duration
            durationTimeText.text = formatTime(duration.toLong())
            seekBar.progress = 0
            currentTimeText.text = formatTime(0)

            startUpdatingSeekBar()

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка воспроизведения: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startUpdatingSeekBar() {
        updateSeekBarRunnable?.let { handler.removeCallbacks(it) }

        updateSeekBarRunnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        val currentPos = it.currentPosition
                        seekBar.progress = currentPos
                        currentTimeText.text = formatTime(currentPos.toLong())
                        handler.postDelayed(this, 1000)
                    } else {
                        handler.postDelayed(this, 1000)
                    }
                }
            }
        }
        handler.post(updateSeekBarRunnable!!)
    }

    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateSeekBarRunnable!!)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}