package cn.wearbbs.music.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.wearbbs.music.R
import cn.wearbbs.music.adapter.LocalMusicAdapter
import cn.wearbbs.music.view.MessageView
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.Objects
import kotlin.coroutines.resume

/**
 * 本地音乐
 */
open class LocalMusicNewActivity : AppCompatActivity() {

    private var rv_main: RecyclerView? = null
    private var localMusicAdapter: LocalMusicAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_localmusic)
        rv_main = findViewById(R.id.rv_main)
        localMusicAdapter = LocalMusicAdapter(JSONArray(), this)
        rv_main?.setLayoutManager(LinearLayoutManager(this))
        rv_main?.setAdapter(localMusicAdapter)
        checkPermissionForInit()
    }

    @SuppressLint("NonConstantResourceId")
    fun onClick(view: View) {
        when (view.id) {
            R.id.main_title -> finish()
            R.id.tv_add -> startActivity(
                Intent(
                    this@LocalMusicNewActivity,
                    FtpActivity::class.java
                )
            )
        }
    }

    suspend fun initList(root: File): JSONArray? {
        return suspendCancellableCoroutine { continuation ->
            if (root.exists()) {
                val data = JSONArray()
                root.listFiles()?.forEach {
                    if (it.isDirectory) {
                        var musicDir = File(it.path + "/music")
                        if (!musicDir.exists()) {
                            musicDir = File(it.path)
                        }
                        val lrcDir = File(it.path + "/lrc")
                        val coverDir = File(it.path + "/cover")
                        val idDir = File(it.path + "/id")
                        val musicFiles = musicDir.listFiles { pathname: File ->
                            pathname.name.endsWith(".mp3") ||
                                    pathname.name.endsWith(".wav") ||
                                    pathname.name.endsWith(".aac") ||
                                    pathname.name.endsWith(".flac")
                        }
                        val lrcFiles = lrcDir.listFiles { pathname: File ->
                            pathname.name.endsWith(".lrc")
                        }
                        val coverFiles = coverDir.listFiles { pathname: File ->
                            pathname.name.endsWith(".jpg") ||
                                    pathname.name.endsWith(".png")
                        }
                        val idFiles = idDir.listFiles { pathname: File ->
                            pathname.name.endsWith(".txt")
                        }
                        if ((musicFiles?.size ?: 0) > 0) {
                            for (i in Objects.requireNonNull(musicFiles).indices) {
                                val musicInfo = JSONObject()
                                musicInfo["musicFile"] = musicFiles[i].path
                                val lrcIndex =
                                    searchFilesArrayForIndex(lrcFiles, getFileName(musicFiles[i]))
                                if (lrcIndex != -1) {
                                    assert(lrcFiles != null)
                                    musicInfo["lrcFile"] = lrcFiles!![lrcIndex].path
                                } else {
                                    musicInfo["lrcFile"] = null
                                }
                                val coverIndex =
                                    searchFilesArrayForIndex(coverFiles, getFileName(musicFiles[i]))
                                if (coverIndex != -1) {
                                    assert(coverFiles != null)
                                    musicInfo["coverFile"] = coverFiles!![coverIndex].path
                                } else {
                                    musicInfo["coverFile"] = null
                                }
                                val idIndex =
                                    searchFilesArrayForIndex(idFiles, getFileName(musicFiles[i]))
                                if (idIndex != -1) {
                                    var id: String? = null
                                    try {
                                        val `in` = BufferedReader(FileReader(idFiles[idIndex]))
                                        id = `in`.readLine()
                                    } catch (ignored: IOException) {
                                    }
                                    musicInfo["id"] = id
                                    musicInfo["idFile"] = idFiles[idIndex].path
                                } else {
                                    musicInfo["id"] = null
                                }
                                data.add(musicInfo)
                            }
                        }
                    }
                }
                if (continuation.isActive) {
                    continuation.resume(data)
                }
            } else {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }
    }

    private fun searchFilesArrayForIndex(array: Array<File>?, str: String): Int {
        if (array == null) {
            return -1
        }
        for (i in array.indices) {
            if (getFileName(array[i]) == str) {
                return i
            }
        }
        return -1
    }

    private fun getFileName(file: File): String {
        return file.name.substring(0, file.name.lastIndexOf("."))
    }

    fun checkPermissionForInit() {
        // 读取权限
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= 23) {
            // 检查权限是否已授权
            val hasPermission = checkSelfPermission(permission)
            // 如果没有授权
            if (hasPermission != PackageManager.PERMISSION_GRANTED) {
                // 请求权限
                requestPermissions(arrayOf(permission), 0)
            } else {
                // 已授权权限
                loadMusic()
            }
        } else {
            loadMusic()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size > 0) { //grantResults 数组中存放的是授权结果
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 同意授权
                loadMusic()
            } else {
                // 拒绝授权
                showPermissionDeniedMessage()
            }
        }
    }

    private fun loadMusic() {
        findViewById<View>(R.id.lv_loading).visibility = View.GONE
        lifecycleScope.launch(Dispatchers.IO) {
            val sdDownload =
                File(Environment.getExternalStorageDirectory().absolutePath + "/download")
            val sdMusic = File(Environment.getExternalStorageDirectory().absolutePath + "/Music")
            val sdMyMusic =
                File(Environment.getExternalStorageDirectory().absolutePath + "/MyMusic")
            val root = File(getExternalFilesDir(null).toString() + "/download")
            val resultList = JSONArray()
            initList(root)?.also {
                resultList.addAll(it)
            }
            initList(sdDownload)?.also {
                resultList.addAll(it)
            }
            initList(sdMusic)?.also {
                resultList.addAll(it)
            }
            initList(sdMyMusic)?.also {
                resultList.addAll(it)
            }
            withContext(Dispatchers.Main) {
                if (resultList.isNotEmpty()) {
                    localMusicAdapter!!.addData(resultList)
                    rv_main!!.visibility = View.VISIBLE
                } else {
                    showNoMusicMessage()
                }
            }
        }
    }

    fun showPermissionDeniedMessage() {
        lifecycleScope.launch(Dispatchers.Main) {
            val rv_main = findViewById<RecyclerView>(R.id.rv_main)
            rv_main.visibility = View.GONE
            findViewById<View>(R.id.lv_loading).visibility = View.GONE
            val mv_message = findViewById<MessageView>(R.id.mv_message)
            mv_message.setImageResource(R.drawable.ic_baseline_sd_storage_24)
            mv_message.setText(R.string.permission_denied)
        }
    }

    fun showNoMusicMessage() {
        lifecycleScope.launch(Dispatchers.Main) {
            val rv_main = findViewById<RecyclerView>(R.id.rv_main)
            rv_main.visibility = View.GONE
            findViewById<View>(R.id.lv_loading).visibility = View.GONE
            val mv_message = findViewById<MessageView>(R.id.mv_message)
            mv_message.setImageResource(R.drawable.ic_baseline_assignment_24)
            mv_message.setText(R.string.msg_noMusic)
            mv_message.visibility = View.VISIBLE
        }
    }

    fun showErrorMessage() {
        lifecycleScope.launch(Dispatchers.Main) {
            val rv_main = findViewById<RecyclerView>(R.id.rv_main)
            rv_main.visibility = View.GONE
            findViewById<View>(R.id.lv_loading).visibility = View.GONE
            val mv_message = findViewById<MessageView>(R.id.mv_message)
            mv_message.visibility = View.VISIBLE
            mv_message.setContent(MessageView.LOAD_FAILED) { v: View? ->
                mv_message.visibility = View.GONE
                loadMusic()
            }
        }
    }

    companion object {
        fun getCacheRootDir(context: Context, internal: Boolean): String {
            val cachePath: String
            cachePath = if (internal) {
                context.filesDir.path
            } else {
                try {
                    if ("mounted" != Environment.getExternalStorageState()) {
                        context.filesDir.path
                    } else {
                        val externalFilesDir = context.getExternalFilesDir(null as String?)
                        if (externalFilesDir != null) {
                            externalFilesDir.path
                        } else {
                            context.filesDir.path
                        }
                    }
                } catch (var4: Throwable) {
                    return context.filesDir.path
                }
            }
            return cachePath
        }
    }
}