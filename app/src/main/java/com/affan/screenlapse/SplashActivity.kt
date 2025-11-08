package com.affan.screenlapse

import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val nameText = findViewById<TextView>(R.id.creator_name)

        // Apply gradient shader after layout is drawn
        nameText.post {
            val paint = nameText.paint
            val width = paint.measureText(nameText.text.toString())

            val textShader = LinearGradient(
                0f, 0f, width, nameText.textSize,
                intArrayOf(
                    Color.parseColor("#B8005E"), // Deep Pink
                    Color.parseColor("#FF5722"), // Orange
                    Color.parseColor("#964B00")  // Dark Beige/Brown
                ),
                null,
                Shader.TileMode.CLAMP
            )

            nameText.paint.shader = textShader
            nameText.invalidate()
        }

        // Delay to show splash screen
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, ProfileSelectionActivity::class.java))
            finish()
        }, 2000)
    }
}
