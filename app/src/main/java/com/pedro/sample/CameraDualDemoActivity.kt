package com.pedro.sample

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
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
import kotlinx.android.synthetic.main.activity_dual_camera_demo.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CameraDualDemoActivity : AppCompatActivity(), ConnectCheckerRtsp, View.OnClickListener {

  private lateinit var rtspServerCamera1: RtspServerCamera2
  private lateinit var rtspServerCamera2: RtspServerCamera2

  private lateinit var button: Button
  private lateinit var bRecord: Button

  private var currentDateAndTime = ""
  private lateinit var folder: File

  private var currentDateAndTime_watermark = ""

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    setContentView(R.layout.activity_dual_camera_demo)
    folder = File(getExternalFilesDir(null)!!.absolutePath + "/rtmp-rtsp-stream-client-java")
    button = findViewById(R.id.b_start_stop_dual)
    button.setOnClickListener(this)

    // 创建 rtsp server。 如果需要打开两个摄像头，经过平台测试，必须先打开id1 、 再打开id0
    rtspServerCamera1 = RtspServerCamera2(surfaceView2, this, 1935, "1")
    rtspServerCamera2 = RtspServerCamera2(surfaceView1, this, 2000, "0")

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



    val textObjectFilterRender1 = TextObjectFilterRender()
    rtspServerCamera2.glInterface.setFilter(textObjectFilterRender1)
    textObjectFilterRender1.setText(currentDateAndTime_watermark, 22f, Color.WHITE)
    textObjectFilterRender1.setDefaultScale(
            rtspServerCamera2.streamWidth,
            rtspServerCamera2.streamHeight
    )
    textObjectFilterRender1.setPosition(TranslateTo.TOP_LEFT)
  }

  override fun onNewBitrateRtsp(bitrate: Long) {

  }

  override fun onConnectionSuccessRtsp(cameraId: String) {
    runOnUiThread {
      Toast.makeText(this@CameraDualDemoActivity, "camera id = $cameraId is Connection success", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onConnectionFailedRtsp(reason: String, cameraId: String) {
    runOnUiThread {
      Toast.makeText(this@CameraDualDemoActivity, "camera id = $cameraId is Connection failed. $reason", Toast.LENGTH_SHORT)
          .show()
      // 停止rtsp 服务
      rtspServerCamera1.stopStream()
      rtspServerCamera2.stopStream()
      button.setText(R.string.start_button)
    }
  }

  override fun onConnectionStartedRtsp(rtspUrl: String) {
  }

  override fun onDisconnectRtsp(cameraId: String) {
    runOnUiThread {
      Toast.makeText(this@CameraDualDemoActivity, "camera id = $cameraId is Disconnected", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onAuthErrorRtsp() {
    runOnUiThread {
      Toast.makeText(this@CameraDualDemoActivity, "Auth error", Toast.LENGTH_SHORT).show()
      rtspServerCamera1.stopStream()
      rtspServerCamera2.stopStream()
      button.setText(R.string.start_button)
      tv_url_dual.text = ""
    }
  }

  override fun onPause() {
    super.onPause()
    rtspServerCamera1.stopStream()
    rtspServerCamera2.stopStream()
  }

  override fun onAuthSuccessRtsp() {
    runOnUiThread {
      Toast.makeText(this@CameraDualDemoActivity, "Auth success", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onClick(view: View) {
    when (view.id) {
      R.id.b_start_stop_dual -> if (!rtspServerCamera1.isStreaming) {
        if (rtspServerCamera1.prepareAudio() && rtspServerCamera1.prepareVideo()) {
          button.setText(R.string.stop_button)
          // 开启camera -> rtsp 传输
          rtspServerCamera1.startStream()
          if (rtspServerCamera2.prepareAudio() && rtspServerCamera2.prepareVideo()) {
            rtspServerCamera2.startStream()
          }
          // 获取rtsp 地址
          tv_url_dual.text = rtspServerCamera1.getEndPointConnection()
        } else {
          Toast.makeText(this, "Error preparing stream, This device cant do it", Toast.LENGTH_SHORT)
              .show()
        }
      } else {
        button.setText(R.string.start_button)
        rtspServerCamera1.stopStream()
        rtspServerCamera2.stopStream()
        tv_url_dual.text = ""
      }
      else -> {
      }
    }
  }
}
