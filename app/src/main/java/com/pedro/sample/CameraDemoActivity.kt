package com.pedro.sample

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pedro.encoder.input.gl.render.filters.`object`.TextObjectFilterRender
import com.pedro.encoder.utils.gl.TranslateTo
import com.pedro.rtplibrary.view.OpenGlView
import com.pedro.rtsp.utils.ConnectCheckerRtsp
import com.pedro.rtspserver.RtspServerCamera2
import kotlinx.android.synthetic.main.activity_camera_demo.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import com.pedro.rtsp.rtsp.VideoCodec
import com.pedro.sample.ui.OverlayView
import com.ubeesky.lib.ai.AIDetectResult
import com.ubeesky.lib.ai.AINative
import com.ubeesky.lib.ai.FileUtils


class CameraDemoActivity : AppCompatActivity(), ConnectCheckerRtsp, View.OnClickListener, SeekBar.OnSeekBarChangeListener, AINative.AICallback{

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

  private val textObjectFilterRender = TextObjectFilterRender()

  private var strList_top_left : ArrayList<String> = arrayListOf()
  private var strList_top_right : ArrayList<String> = arrayListOf()
  private var strList_bottom_left : ArrayList<String> = arrayListOf()
  private var strList_bottom_right : ArrayList<String> = arrayListOf()

  private lateinit var aiNative: AINative
  private lateinit var overlayView: OverlayView

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

    overlayView = findViewById(R.id.overlay)
    aiNative = AINative(this)
    modelInit()

    // 创建 rtsp server。
    rtspServerCamera1 = RtspServerCamera2(surfaceView, this, 1935, "1", aiNative)

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
    val timer = Timer()
    val task: TimerTask = object : TimerTask() {
      override fun run() {
        //要推迟执行的方法
        textObjectFilterRender.updateStringList(
          strList_top_left,
          intArrayOf(0, 1),
          arrayOf(System.currentTimeMillis().toString(), (System.currentTimeMillis() * 2).toString()),
          TranslateTo.TOP_LEFT
        )
        textObjectFilterRender.updateStringList(
          strList_bottom_right,
          intArrayOf(1, 3),
          arrayOf((System.currentTimeMillis() * 3).toString(), (System.currentTimeMillis() * 4).toString()),
          TranslateTo.BOTTOM_RIGHT
        )
      }
    }
    timer.schedule(task, 1000, 30000)
  }

  private fun initTimeWaterMarkFormat() {
    rtspServerCamera1.glInterface.setFilter(textObjectFilterRender)
    textObjectFilterRender.setDefaultScale(640, 480)
    initStrList()
    textObjectFilterRender.setImageTextureList(strList_top_left, TranslateTo.TOP_LEFT)
    textObjectFilterRender.setImageTextureList(strList_top_right, TranslateTo.TOP_RIGHT)
    textObjectFilterRender.setImageTextureList(strList_bottom_left, TranslateTo.BOTTOM_LEFT)
    textObjectFilterRender.setImageTextureList(strList_bottom_right, TranslateTo.BOTTOM_RIGHT)
  }

  //初始化除时间水印之外的其他水印
  private fun initStrList() {
    strList_top_left.add("0.0V 0.0V 0.0A 0% 0℃ T")
    strList_top_left.add("SIM卡盖未拧紧")

    strList_top_right.add("水印 2.1")
    strList_top_right.add("水印 2.2")

    strList_bottom_left.add("水印 3.1")
    strList_bottom_left.add("水印 3.2")
    strList_bottom_left.add("水印 3.3")

    strList_bottom_right.add("水印 4.1")
    strList_bottom_right.add("水印 4.2")
    strList_bottom_right.add("水印 4.3")
    strList_bottom_right.add("水印 4.4")
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
          // 添加OSD
          initTimeWaterMarkFormat()
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

  private fun modelInit() {
    val modelPath: String = copyModel().toString()
    val ret: Int = aiNative.init(modelPath, 1)
    val msg = "模型初始化：" + if (ret == -1) "失败" else "成功"
  }

  private fun copyModel(): String? {
    val targetDir: String = this.filesDir.absolutePath
    val modelPathsDetector = arrayOf(
      "nanodet_m.tnnmodel",
      "nanodet_m.tnnproto"
    )
    for (i in modelPathsDetector.indices) {
      val modelFilePath = modelPathsDetector[i]
      val interModelFilePath = "$targetDir/$modelFilePath"
      FileUtils.copyAsset(
        this.assets,
        "model/$modelFilePath", interModelFilePath
      )
    }
    return targetDir
  }

  override fun steamAIResult(results: Array<AIDetectResult>?) {
    overlayView.setResults(results)
    if (results != null) {
      printArray(results)
    }
  }

  override fun imageAIResult(results: Array<AIDetectResult>?) {
    TODO("Not yet implemented")
  }

  private fun printArray(aiDetectResults: Array<AIDetectResult>?) {
    if (aiDetectResults == null) {
      return
    }
    for (result in aiDetectResults) {
      Log.d("cc", result.toString())
    }
  }

}
