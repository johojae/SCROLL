/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kaist.dd

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.kaist.dd.databinding.ActivityDetectBinding
import com.kaist.dd.fragment.CameraFragment
import com.kaist.dd.fragment.LogFragment

class DetectActivity : AppCompatActivity() {
    private lateinit var activityDetectBinding: ActivityDetectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityDetectBinding = ActivityDetectBinding.inflate(layoutInflater)
        setContentView(activityDetectBinding.root)

        supportFragmentManager.beginTransaction().replace(R.id.fragment_container,
            CameraFragment()).commitAllowingStateLoss()

        activityDetectBinding.navigation.selectedItemId = R.id.tab_camera
        activityDetectBinding.navigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    finishAffinity()
                    startActivity(intent)
                    return@setOnItemSelectedListener true
                }
                R.id.tab_camera -> {
                    supportFragmentManager.beginTransaction().replace(R.id.fragment_container,
                        CameraFragment()).commitAllowingStateLoss()
                    return@setOnItemSelectedListener true
                }
                R.id.tab_log -> {
                    supportFragmentManager.beginTransaction().replace(R.id.fragment_container,
                        LogFragment()).commitAllowingStateLoss()
                    return@setOnItemSelectedListener true
                }
                else -> {
                    false
                }
            }
        }
        activityDetectBinding.navigation.setOnItemReselectedListener { item ->
            when (item.itemId) {
                R.id.tab_home -> {}
                R.id.tab_camera -> {}
                R.id.tab_log -> {}
            }
        }
    }

    override fun onBackPressed() {
        finish()
    }
}
