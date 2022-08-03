package com.pedro.sample

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pedro.encoder.input.gl.render.filters.`object`.TextObjectFilterRender
import com.pedro.encoder.utils.BitmapUtils
import com.pedro.encoder.utils.gl.TranslateTo
import com.pedro.rtplibrary.view.OpenGlView
import com.pedro.rtplibrary.view.TakePhotoCallback
import com.pedro.rtsp.utils.ConnectCheckerRtsp
import com.pedro.rtspserver.RtspServerCamera2
import kotlinx.android.synthetic.main.activity_camera_demo.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import com.pedro.rtsp.rtsp.VideoCodec


class CameraDemoActivity : AppCompatActivity(), ConnectCheckerRtsp, View.OnClickListener, SeekBar.OnSeekBarChangeListener  {

  private lateinit var rtspServerCamera1: RtspServerCamera2

  private lateinit var button: Button
  private lateinit var buttonServer: Button

  private lateinit var bRecord: Button
  private lateinit var bCapture: Button

  private var currentDateAndTime = ""
  private lateinit var folder: File

  private var currentDateAndTime_watermark = ""

  private var mPreviewW: Int = 1920
  private var mPreviewH: Int = 1080

  private lateinit var seekBar: SeekBar

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    setContentView(R.layout.activity_camera_demo)
    folder = File(getExternalFilesDir(null)!!.absolutePath + "/rtmp-rtsp-stream-client-java")
    button = findViewById(R.id.b_start_stop_stream)
    button.setOnClickListener(this)
    buttonServer = findViewById(R.id.b_start_stop_server)
    buttonServer.setOnClickListener(this)
    bRecord = findViewById(R.id.b_record)
    bRecord.setOnClickListener(this)
    bCapture = findViewById(R.id.b_start_capture)
    bCapture.setOnClickListener(this)
    seekBar = findViewById(R.id.seek_bar_ev)
    seekBar.setOnSeekBarChangeListener(this)
    seekBar.min = -24
    seekBar.max = 24
    seekBar.setProgress(0, false)

    // 创建 rtsp server。
    rtspServerCamera1 = RtspServerCamera2(surfaceView, this, 1935, "1")

    // 遍历支持的size
    for (size in rtspServerCamera1.getResolutionsBack()) {
      Log.e("stormxz", size.width.toString() + "X" + size.height)
    }
    rtspServerCamera1.setVideoCodec(VideoCodec.H265)
//    rtspServerCamera1.setVideoCodec(VideoCodec.H264)


    // 可以给 预览宽高 赋值
//    mPreviewW = 1440
//    mPreviewH = 1080
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
      R.id.b_start_capture -> {
        val df = SimpleDateFormat("yyyyMMddHHmmss")
        capture("/storage/emulated/0/Android/data/com.pedro.sample/files/RSTP_" + df.format(Date()) + "_" + System.currentTimeMillis() + ".jpg", surfaceView)
      }
      R.id.b_start_stop_stream -> if (!rtspServerCamera1.isStreaming) {
        if (rtspServerCamera1.prepareAudio() && rtspServerCamera1.prepareVideo(mPreviewW, mPreviewH)) {
          button.setText("close camera")
          // 开启camera -> rtsp 传输
          rtspServerCamera1.startStream()

        } else {
          Toast.makeText(this, "Error preparing stream, This device cant do it", Toast.LENGTH_SHORT)
                  .show()
        }
      }
      else {
        button.setText("open camera")
        if (rtspServerCamera1.isRecording) {
          rtspServerCamera1.stopRecord()
          bRecord.setText(R.string.start_record)
        }
        rtspServerCamera1.stopStream()
        rtspServerCamera1.stopPreview()
        tv_url.text = ""
      }
      R.id.b_start_stop_server -> if (!rtspServerCamera1.isServering()) {
        buttonServer.setText("stop server")
        rtspServerCamera1.startServer()
        // 获取rtsp 地址
        tv_url.text = rtspServerCamera1.getEndPointConnection()
//        surfaceView.takePhoto(this@CameraDemoActivity)
      }
      else {
        buttonServer.setText("start server")
        rtspServerCamera1.stopServer()
        tv_url.text = ""
      }
      R.id.b_record -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
          if (!rtspServerCamera1.isRecording) {
            try {
              if (!folder.exists()) {
                folder.mkdir()
              }
              val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
              currentDateAndTime = sdf.format(Date())
              if (!rtspServerCamera1.isStreaming) {
                  Toast.makeText(
                          this, "please start stream",
                          Toast.LENGTH_SHORT
                  ).show()
              } else {
                rtspServerCamera1.startRecord(folder.absolutePath + "/" + currentDateAndTime + ".mp4")
                bRecord.setText(R.string.stop_record)
                Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show()
              }
            } catch (e: IOException) {
              rtspServerCamera1.stopRecord()
              bRecord.setText(R.string.start_record)
              Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
          } else {
            rtspServerCamera1.stopRecord()
            bRecord.setText(R.string.start_record)
            Toast.makeText(
                    this, "file " + currentDateAndTime + ".mp4 saved in " + folder.absolutePath,
                    Toast.LENGTH_SHORT
            ).show()
          }
        } else {
          Toast.makeText(this, "You need min JELLY_BEAN_MR2(API 18) for do it...", Toast.LENGTH_SHORT).show()
        }
      }
      else -> {
      }
    }
  }

  override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
    Log.e("stormxz", " seekbar progress = " + seekBar!!.progress)
  }

  override fun onStartTrackingTouch(seekBar: SeekBar?) {
    Log.e("stormxz", " seekbar progress = " + seekBar!!.progress)

  }

  override fun onStopTrackingTouch(seekBar: SeekBar?) {
    Log.e("stormxz", " seekbar progress = " + seekBar!!.progress)
    rtspServerCamera1.setExposure(seekBar!!.progress)
  }

  fun capture(string : String, view: OpenGlView) {
    view.capture(string)
  }

}
