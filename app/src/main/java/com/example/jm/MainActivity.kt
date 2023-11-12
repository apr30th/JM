package com.example.jm

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.jm.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var binding : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.run {
            imageCamera.setOnClickListener {
                finish()
                startActivity(Intent(this@MainActivity, CameraActivity::class.java))
            }
            imageGallery.setOnClickListener {
                finish()
                startActivity(Intent(this@MainActivity, GalleryActivity::class.java))
            }
        }
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)

    }
}

// 디코에 올렸던 원래 있던 코드에서 바인딩만 리니어 레이아웃에 맞게 수정함