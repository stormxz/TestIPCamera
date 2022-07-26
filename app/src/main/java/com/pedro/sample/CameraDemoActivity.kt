package com.pedro.sample

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.SurfaceHolder
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pedro.encoder.input.gl.render.filters.`object`.TextObjectFilterRender
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.encoder.input.video.CameraOpenException
import com.pedro.encoder.utils.gl.TranslateTo
import com.pedro.rtsp.utils.ConnectCheckerRtsp
import com.pedro.rtspserver.RtspServerCamera1
import com.pedro.rtspserver.RtspServerCamera2
import kotlinx.android.synthetic.main.activity_camera_demo.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CameraDemoActivity : AppCompatActivity(), ConnectCheckerRtsp, View.OnClickListener {

  private lateinit var rtspServerCamera1: RtspServerCamera2

  private lateinit var button: Button
  private lateinit var bRecord: Button

  private var currentDateAndTime = ""
  private lateinit var folder: File

  private var currentDateAndTime_watermark = ""

  private var mPreviewW: Int = 1920
  private var mPreviewH: Int = 1080


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    setContentView(R.layout.activity_camera_demo)
    folder = File(getExternalFilesDir(null)!!.absolutePath + "/rtmp-rtsp-stream-client-java")
    button = findViewById(R.id.b_start_stop)
    button.setOnClickListener(this)

    // 创建 rtsp server。
    rtspServerCamera1 = RtspServerCamera2(surfaceView, this, 1935, "0")

    // 遍历支持的size
    for (size in rtspServerCamera1.getResolutionsBack()) {
      Log.e("stormxz", size.width.toString() + "X" + size.height)
    }

    // 可以给 预览宽高 赋值
//    mPreviewW = 1920
//    mPreviewH = 1440
  }

  override fun onResume() {
    super.onResume()

    // 添加OSD
    initTimeWaterMarkFormat()
  }

  private fun initTimeWaterMarkFormat() {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    currentDateAndTime_watermark = sdf.format(Date())
    val textObjectFilterRender = TextObjectFilterRender()
    rtspServerCamera1.glInterface.setFilter(textObjectFilterRender)
    textObjectFilterRender.setText(currentDateAndTime_watermark, 22f, Color.WHITE)
    textObjectFilterRender.setDefaultScale(
            rtspServerCamera1.streamWidth,
            rtspServerCamera1.streamHeight
    )
    textObjectFilterRender.setPosition(TranslateTo.TOP_LEFT)
//        spriteGestureController.setBaseObjectFilterRender(textObjectFilterRender) //Optional
  }

  override fun onNewBitrateRtsp(bitrate: Long) {

  }

  override fun onConnectionSuccessRtsp(cameraId: String) {
    runOnUiThread {
      Toast.makeText(this@CameraDemoActivity, "camera id = $cameraId is Connection success", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onConnectionFailedRtsp(reason: String, cameraId: String) {
    runOnUiThread {
      Toast.makeText(this@CameraDemoActivity, "camera id = $cameraId is Connection failed. $reason", Toast.LENGTH_SHORT)
          .show()
      // 停止rtsp 服务
      rtspServerCamera1.stopStream()
      button.setText(R.string.start_button)
    }
  }

  override fun onConnectionStartedRtsp(rtspUrl: String) {
  }

  override fun onDisconnectRtsp(cameraId: String) {
    runOnUiThread {
      Toast.makeText(this@CameraDemoActivity, "camera id = $cameraId is  Disconnected", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onAuthErrorRtsp() {
    runOnUiThread {
      Toast.makeText(this@CameraDemoActivity, "Auth error", Toast.LENGTH_SHORT).show()
      rtspServerCamera1.stopStream()
      button.setText(R.string.start_button)
      tv_url.text = ""
    }
  }

  override fun onPause() {
    super.onPause()
    rtspServerCamera1.stopStream()
  }

  override fun onAuthSuccessRtsp() {
    runOnUiThread {
      Toast.makeText(this@CameraDemoActivity, "Auth success", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onClick(view: View) {
    when (view.id) {
      R.id.b_start_stop -> if (!rtspServerCamera1.isStreaming) {
        if (rtspServerCamera1.prepareAudio() && rtspServerCamera1.prepareVideo(mPreviewW, mPreviewH)) {
          button.setText(R.string.stop_button)
          // 开启camera -> rtsp 传输
          rtspServerCamera1.startStream()
          // 获取rtsp 地址
          tv_url.text = rtspServerCamera1.getEndPointConnection()
        } else {
          Toast.makeText(this, "Error preparing stream, This device cant do it", Toast.LENGTH_SHORT)
              .show()
        }
      } else {
        button.setText(R.string.start_button)
        rtspServerCamera1.stopStream()
        tv_url.text = ""
      }
      else -> {
      }
    }
  }
}
