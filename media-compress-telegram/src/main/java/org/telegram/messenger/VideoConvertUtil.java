package org.telegram.messenger;

import android.media.MediaMetadataRetriever;
import android.util.Log;

import androidx.annotation.Nullable;

import org.telegram.ui.Components.AnimatedFileDrawable;

import java.util.WeakHashMap;

public class VideoConvertUtil {
    private static final String TAG = Utilities.class.getSimpleName();
    private static final WeakHashMap<Integer, VideoEditedInfo> sConvertInfoMap = new WeakHashMap<>();
    private static Scheduler sScheduler;

    /**
     * 初始化
     */
    public static void init(Scheduler scheduler) {
        sScheduler = scheduler;
    }

    public static Scheduler getScheduler() {
        return sScheduler;
    }

    /**
     * Start video conversion
     *
     * @param compressionInfo information for compressing video
     * @param listener   回调
     * @return 唯一id
     */
    @Nullable
    public static Integer startVideoConvert(VideoReqCompressionInfo compressionInfo,
                                            MediaController.ConvertorListener listener) {
        VideoEditedInfo info = createCompressionSettings(compressionInfo);
        if (info == null) {
            return null;
        }
        info.attachPath = compressionInfo.attachPath;
        boolean ret = MediaController.getInstance().scheduleVideoConvert(info, listener);
        if (!ret) {
            return null;
        }
        sConvertInfoMap.put(info.id, info);
        return info.id;
    }

    /**
     * 停止视频转换
     *
     * @param id 唯一id
     */
    public static void stopVideoConvert(int id) {
        VideoEditedInfo info = sConvertInfoMap.remove(id);
        if (info != null) {
            MediaController.getInstance().cancelVideoConvert(info);
        }
    }

    /**
     * 压缩参数设置
     *
     * @param info 视频路径
     */
    private static VideoEditedInfo createCompressionSettings(VideoReqCompressionInfo info) {
        int[] params = new int[AnimatedFileDrawable.PARAM_NUM_COUNT];
        AnimatedFileDrawable.getVideoInfo(info.videoPath, params);

        if (params[AnimatedFileDrawable.PARAM_NUM_SUPPORTED_VIDEO_CODEC] == 0) {
//            if (BuildConfig.DEBUG) {
//                Log.d(TAG, "video hasn't avc1 atom");
//            }
            return null;
        }

        int originalBitrate = getVideoBitrate(info.videoPath);
        if (originalBitrate == -1) {
            originalBitrate = params[AnimatedFileDrawable.PARAM_NUM_BITRATE];
        }
        int bitrate = originalBitrate;
        float videoDuration = params[AnimatedFileDrawable.PARAM_NUM_DURATION];
        int videoFramerate = params[AnimatedFileDrawable.PARAM_NUM_FRAMERATE];

        VideoEditedInfo videoEditedInfo = new VideoEditedInfo();
        videoEditedInfo.startTime = -1;
        videoEditedInfo.endTime = -1;
        videoEditedInfo.originalBitrate = videoEditedInfo.bitrate = bitrate;
        videoEditedInfo.originalPath = info.videoPath;
        videoEditedInfo.framerate = videoFramerate;
        videoEditedInfo.resultWidth = videoEditedInfo.originalWidth = params[AnimatedFileDrawable.PARAM_NUM_WIDTH];
        videoEditedInfo.resultHeight = videoEditedInfo.originalHeight = params[AnimatedFileDrawable.PARAM_NUM_HEIGHT];
        videoEditedInfo.rotationValue = params[AnimatedFileDrawable.PARAM_NUM_ROTATION];
        videoEditedInfo.originalDuration = (long) (videoDuration * 1000);

        int compressionsCount;

        float maxSize = Math.max(videoEditedInfo.originalWidth, videoEditedInfo.originalHeight);
        if (maxSize > 1280) {
            compressionsCount = 4;
        } else if (maxSize > 854) {
            compressionsCount = 3;
        } else if (maxSize > 640) {
            compressionsCount = 2;
        } else {
            compressionsCount = 1;
        }

        // WIFI || MOBILE
        int selectedCompression = Math.round(100 / (100f / compressionsCount));
        // ROAMING
//        int selectedCompression = Math.round(50 / (100f / compressionsCount));

        if (selectedCompression > compressionsCount) {
            selectedCompression = compressionsCount;
        }
        boolean needCompress = false;
        if (selectedCompression != compressionsCount || Math.max(videoEditedInfo.originalWidth, videoEditedInfo.originalHeight) > 1280) {
            needCompress = true;
            switch (selectedCompression) {
                case 1:
                    maxSize = 432.0f;
                    break;
                case 2:
                    maxSize = 640.0f;
                    break;
                case 3:
                    maxSize = 848.0f;
                    break;
                default:
                    maxSize = 1280.0f;
                    break;
            }
            float scale = videoEditedInfo.originalWidth > videoEditedInfo.originalHeight ? maxSize / videoEditedInfo.originalWidth : maxSize / videoEditedInfo.originalHeight;
            videoEditedInfo.resultWidth = Math.round(videoEditedInfo.originalWidth * scale / 2) * 2;
            videoEditedInfo.resultHeight = Math.round(videoEditedInfo.originalHeight * scale / 2) * 2;
        }
        if (info.maxSize > 0 && info.maxSize != maxSize) {
            float scale = videoEditedInfo.originalWidth > videoEditedInfo.originalHeight ? info.maxSize / videoEditedInfo.originalWidth : info.maxSize / videoEditedInfo.originalHeight;
            videoEditedInfo.resultWidth = Math.round(videoEditedInfo.originalWidth * scale / 2) * 2;
            videoEditedInfo.resultHeight = Math.round(videoEditedInfo.originalHeight * scale / 2) * 2;
        }

        bitrate = makeVideoBitrate(
                videoEditedInfo.originalHeight, videoEditedInfo.originalWidth,
                originalBitrate,
                videoEditedInfo.resultHeight, videoEditedInfo.resultWidth
        );

        if (info.bitRate > 0 && info.bitRate < bitrate) {
            bitrate = info.bitRate;
        }

        if (info.frameRate > 0 && info.frameRate < videoFramerate) {
            videoEditedInfo.framerate = info.frameRate;
        }

        if (!needCompress) {
            videoEditedInfo.resultWidth = videoEditedInfo.originalWidth;
            videoEditedInfo.resultHeight = videoEditedInfo.originalHeight;
        }
        videoEditedInfo.bitrate = bitrate;

        return videoEditedInfo;
    }

    private static int getVideoBitrate(String path) {
        int bitrate = -1;
        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            try {
                retriever.setDataSource(path);
                bitrate = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));
            } catch (Exception e) {
                Log.e(TAG, e.getLocalizedMessage());
                e.printStackTrace();
            }
            retriever.release();
        } catch (Exception e) {
            Log.d("retriever error:", e.getLocalizedMessage());
        }
        return bitrate;
    }

    private static int makeVideoBitrate(int originalHeight, int originalWidth, int originalBitrate, int height, int width) {
        float compressFactor;
        float minCompressFactor;
        int maxBitrate;
        if (Math.min(height, width) >= 1080) {
            maxBitrate = 6800_000;
            compressFactor = 0.8f;
            minCompressFactor = 0.9f;
        } else if (Math.min(height, width) >= 720) {
            maxBitrate = 1400_000;
            compressFactor = 0.7f;
            minCompressFactor = 0.7f;
        } else if (Math.min(height, width) >= 480) {
            maxBitrate = 1000_000;
            compressFactor = 0.5f;
            minCompressFactor = 0.7f;
        } else {
            maxBitrate = 750_000;
            compressFactor = 0.3f;
            minCompressFactor = 0.5f;
        }
        int remeasuredBitrate = (int) (originalBitrate / (Math.min(originalHeight / (float) (height), originalWidth / (float) (width))));
        remeasuredBitrate *= compressFactor;
        int minBitrate = (int) (getVideoBitrateWithFactor(minCompressFactor) / (1280f * 720f / (width * height)));
        if (originalBitrate < minBitrate) {
            return remeasuredBitrate;
        }
        if (remeasuredBitrate > maxBitrate) {
            return maxBitrate;
        }
        return Math.max(remeasuredBitrate, minBitrate);
    }

    private static int getVideoBitrateWithFactor(float f) {
        return (int) (f * 2000f * 1000f * 1.13f);
    }
}