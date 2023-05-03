[![](https://jitpack.io/v/mnw007/MediaCompress.svg)](https://jitpack.io/#mnw007/MediaCompress)

# MediaCompress
Android video, picture (not yet implemented), audio (not yet implemented) compression library from [Telegram](https://github.com/DrKLO/Telegram)

# Telegram Commit
Commit: 6cb1cdf898a8cfe025b907b79d074c4903d4b424 [6cb1cdf]
Parent: 43401a515c
Author: xaxtix xardas3200@gmail.com
Date: 2022-07-04 15:54:30
Submitted by: xaxtix update to 8. 8.5

# USAGE
## Telegram (minimum support AndroidApi18)

Step 1. Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
Step 2. Add the dependency

	dependencies {
        implementation 'com.github.a365344743s:MediaCompressTelegram:1.0.0'
	}

### Initialization

    org.telegram.messenger.VideoConvertUtil.init(scheduler);

### Start converting
    
    VideoReqCompressionInfo info = new VideoReqCompressionInfo(videoPath, attachPathTelegram, 1000_000, 720);
    Integer convertId = org.telegram.messenger.VideoConvertUtil.startVideoConvert(info, listener)

### Cancel conversion

    org.telegram.messenger.VideoConvertUtil.stopVideoConvert(int convertId);

# Output Video Control

## Illustrate
The video bit rate determines the video clarity. The higher the bit rate, the clearer the video, but the file size will become larger.

The video resolution determines the video width and height, the higher the resolution, the larger the video width and height.

Under the same video bit rate, the higher the video resolution, the blurrier the video.

You can control the output video bit rate and the maximum side.

## Calculation method
1. Calculate the target video resolution based on the source video resolution.


    VideoConvertUtil.createCompressionSettings(String videoPath)

If the maximum side of the source video > 1280, the maximum side of the target video = 1280.

If 1280 >= maximum side of source video > 854, maximum side of target video = 848.

If 854 >= maximum side of source video > 640, maximum side of target video = 640.

If the maximum side of the source video <= 640, the maximum side of the target video = 432.

2.Calculate the target video bit rate

    VideoConvertUtil.makeVideoBitrate(int originalHeight, int originalWidth, int originalBitrate, int height, int width)

1080p, 720p, 480p are divided into four bit rate ranges, and finally the target video bit rate is calculated.

## Video rate control
You can modify the bit rate calculation method in VideoConvertUtil.makeVideoBitrate.

## Video Resolution Control
The video resolution calculation method in VideoConvertUtil.createCompressionSettings can be modified