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


# CameraDemoActivity  -  双camera 以及 osd 的rtsp

1. 创建rtsp server
RtspServerCamera2 rtspServerCamera1 = RtspServerCamera2(surfaceView2, this, 1935, "1")

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

# MainActivity  -  不同需求选择activity
1. 跳转camera rtsp
startActivity(Intent(this, CameraDemoActivity::class.java))

2. 跳转video rtsp
startActivity(Intent(this, RtspFromFileActivity::class.java))







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
    Log.e("stormxz", "stormxz stream video 111");
    boolean result = rtspFromFile.prepareVideo(filePath);
    result |= rtspFromFile.prepareAudio(filePath);
    return result;
  }

4. 开启 video -> rtsp 传输
    rtspFromFile.startStream()

5. 获取rtsp 地址
   rtspFromFile.getEndPointConnection()
