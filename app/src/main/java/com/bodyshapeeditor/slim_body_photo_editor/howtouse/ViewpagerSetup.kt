package com.bodyshapeeditor.slim_body_photo_editor.howtouse

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.bodyshapeeditor.slim_body_photo_editor.MainActivity
import com.bodyshapeeditor.slim_body_photo_editor.Preferences
import com.bodyshapeeditor.slim_body_photo_editor.R
import com.bodyshapeeditor.slim_body_photo_editor.databinding.ActivityViewpagerSetupBinding

class ViewpagerSetup : AppCompatActivity() {
    lateinit var binding: ActivityViewpagerSetupBinding
    private var preferences: Preferences? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewpagerSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferences = Preferences(this)
        val isAppOpen = preferences!!.getAppOpen()
        val viewPager: ViewPager2 = findViewById(R.id.onboardingViewpager)
        val indicator = binding.circleIndicator
        val nextBtn = binding.nextBtn
        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter
        indicator.setViewPager(viewPager);
        nextBtn.setOnClickListener {
            if (viewPager.currentItem < adapter.itemCount - 1) {
                viewPager.currentItem += 1
            } else {

                startActivity(Intent(this, MainActivity::class.java))
              /*  if (isAppOpen) {
                    startActivity(Intent(this, MainActivity::class.java))
                } else {
                    startActivity(Intent(this, PermissionScreen::class.java))
                }*/

            }
        }
        binding.skipTvBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
           /* if (isAppOpen) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                startActivity(Intent(this, PermissionScreen::class.java))
            }*/
        }
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.nextBtn.text = if (position == 2) "Get Started" else "Next"
            }
        })


    }
}
