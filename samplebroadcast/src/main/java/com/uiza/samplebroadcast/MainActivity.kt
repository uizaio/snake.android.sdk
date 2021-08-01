package com.uiza.samplebroadcast

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val SERVER = "rtmps://live-api-s.facebook.com:443/rtmp/"
        const val STREAM_KEY = "FB-4111254245662108-0-Abw4b9YFhJT8AgpF"
    }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        setContentView(R.layout.activity_main)

        setupViews()
    }

    private fun setupViews() {
        edtServer.setText(SERVER)
        edtStreamKey.setText(STREAM_KEY)

        txtInfo.text = String.format(
            Locale.getDefault(),
            "%s - %s",
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE
        )

        btnStart.setOnClickListener {
            val intent = Intent(this@MainActivity, UZBroadCastActivity::class.java)
            intent.putExtra(
                SampleLiveApplication.EXTRA_STREAM_ENDPOINT,
                String.format(
                    "%s/%s",
                    edtServer.text.toString(),
                    edtStreamKey.text.toString()
                )
            )
            startActivity(intent)
        }
        btnStartDisplay.setOnClickListener { v: View? ->
            val intent = Intent(this@MainActivity, UZDisplayActivity::class.java)
            intent.putExtra(
                SampleLiveApplication.EXTRA_STREAM_ENDPOINT,
                String.format(
                    "%s/%s",
                    edtServer.text.toString(),
                    edtStreamKey.text.toString()
                )
            )
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

}
