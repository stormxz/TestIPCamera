# 代码集成更加方便，将之前demo 中的rtspserver 也打包了
# 按这个demo 为准，以后就更新aar 和 接口就行

删除引用
/*
    implementation 'com.github.pedroSG94.rtmp-rtsp-stream-client-java:rtplibrary:2.1.8'
*/

使用libs encoder.aar  rtmp.aar  rtplibrary.aar rtsp.aar rtspserver.aar
    implementation files('libs/rtplibrary.aar')
    implementation files('libs/encoder.aar')
    implementation files('libs/rtmp.aar')
    implementation files('libs/rtsp.aar')
    implementation files('libs/rtspserver.aar')


需要clean project、 rebuild project
---------------------------------------------------------------------------------------------------


# CameraDemoActivity  -  单camera 以及 osd 的rtsp

1. 创建rtsp server
RtspServerCamera2 rtspServerCamera1 = RtspServerCamera2(surfaceView2, this, 1935, "1")

由于涉及到OSD,本项目中 surfaceview 需要使用以下自定义控件
    <com.pedro.rtplibrary.view.OpenGlView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:id="@+id/surfaceView"
        />

2. 添加OSD 方法
initTimeWaterMarkFormat()

3. 准备audio video 配置
if (rtspServerCamera1.prepareAudio() && rtspServerCamera1.prepareVideo()) {
    #开启camera -> rtsp 传输
    rtspServerCamera1.startStream()
}

4. 获取对应rtsp 地址
rtspServerCamera1.getEndPointConnection()


5. 停止rtsp 服务传输
rtspServerCamera1.stopStream()

---------------------------------------------------------------------------------------------------

# CameraDualDemoActivity  -  双camera 以及 osd 的rtsp

与CameraDemoActivity 内接口一致
打开两个摄像头，经过平台测试，必须先打开id1 、 再打开id0



---------------------------------------------------------------------------------------------------

# MainActivity  -  不同需求选择activity
1. 跳转camera rtsp
startActivity(Intent(this, CameraDemoActivity::class.java))

2. 跳转video rtsp
startActivity(Intent(this, RtspFromFileActivity::class.java))

3. 跳转dual camera rtsp
startActivity(Intent(this, CameraDualDemoActivity::class.java))



---------------------------------------------------------------------------------------------------
# RtspFromFileActivity  -  本地video rtsp
1. 创建rtsp server
RtspServerFromFile rtspFromFile = new RtspServerFromFile(mRtspfromfilegl, this, 1234, this, this);

2. video 文件选择 - (也可以你们自己设计)
  case R.id.b_select_file:
       Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
       intent.setType("*/*");
       startActivityForResult(intent, 5);
       break;

  文件选择回调
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {

  }

3. audio video prepare

  private boolean prepare() throws IOException {
    boolean result = rtspFromFile.prepareVideo(filePath);
    result |= rtspFromFile.prepareAudio(filePath);
    return result;
  }

4. 开启 video -> rtsp 传输
    rtspFromFile.startStream()

5. 获取rtsp 地址
   rtspFromFile.getEndPointConnection()


---------------------------------------------------------------------------------------------------
# onConnectionSuccessRtsp(cameraId: String)
# onConnectionFailedRtsp(reason: String, cameraId: String)
# onDisconnectRtsp(cameraId: String)
cameraId 定义：
0, 1, 2..   打开的camera 对应 物理 id
-100        本地video(虚拟设定)
