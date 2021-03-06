package com.uiza.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.uiza.common.Constant
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
            val intent = Intent(this, UZBroadCastActivity::class.java)
            intent.putExtra(
                Constant.EXTRA_STREAM_ENDPOINT,
                String.format("%s/%s", edtServer.text.toString(), edtStreamKey.text.toString())
            )
            startActivity(intent)
        }
        btnStartDisplay.setOnClickListener {
            val intent = Intent(this, UZDisplayActivity::class.java)
            intent.putExtra(
                Constant.EXTRA_STREAM_ENDPOINT,
                String.format("%s/%s", edtServer.text.toString(), edtStreamKey.text.toString())
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
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

}
