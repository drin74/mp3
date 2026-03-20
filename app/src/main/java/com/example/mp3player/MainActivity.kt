package com.example.simplemp3player

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mp3player.R

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private var songList: ArrayList<String> = ArrayList()
    private var songUris: ArrayList<Uri> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.songListView)
        checkPermissions()
    }

    private fun checkPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 100)
        } else {
            loadSongs()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadSongs()
        } else {
            Toast.makeText(this, "Нужно разрешение для доступа к музыке", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun loadSongs() {
        songList.clear()
        songUris.clear()

        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            MediaStore.Audio.Media.TITLE + " ASC"
        )

        if (cursor != null) {
            val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val uriColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val title = cursor.getString(titleColumn)
                val path = cursor.getString(uriColumn)

                if (title != null && path != null) {
                    songList.add(title)
                    songUris.add(Uri.parse(path))
                }
            }
            cursor.close()
        }

        if (songList.isEmpty()) {
            Toast.makeText(this, "Музыка не найдена", Toast.LENGTH_SHORT).show()
        } else {
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, songList)
            listView.adapter = adapter


            listView.setOnItemClickListener { _, _, position, _ ->
                openPlayer(position)
            }

            Toast.makeText(this, "Найдено песен: ${songList.size}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPlayer(position: Int) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("song_position", position)
            putExtra("song_title", songList[position])
            putStringArrayListExtra("song_list", songList)
            putParcelableArrayListExtra("song_uris", songUris)
        }
        startActivity(intent)
    }
}