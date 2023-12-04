/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kaist.dd.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kaist.dd.DatabaseHelper
import com.kaist.dd.LogAdapter
import com.kaist.dd.LogData
import com.kaist.dd.databinding.FragmentLogBinding

class LogFragment : Fragment() {
    private var _fragmentLogBinding: FragmentLogBinding? = null
    private val fragmentLogBinding get() = _fragmentLogBinding!!
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var logAdapter: LogAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentLogBinding =
            FragmentLogBinding.inflate(inflater, container, false)
        databaseHelper = DatabaseHelper(
            context = requireContext()
        )
        initRecycler()

        return fragmentLogBinding.root
    }

    private fun initRecycler() {
        var logData = databaseHelper.getAllLogs() as MutableList<LogData>
        logAdapter = LogAdapter(requireContext())
        logAdapter.datas = logData
        logAdapter.notifyDataSetChanged()
        _fragmentLogBinding!!.logRecyclerView.adapter = logAdapter
    }
}