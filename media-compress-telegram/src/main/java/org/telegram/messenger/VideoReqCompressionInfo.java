/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

public class VideoReqCompressionInfo {
    public String videoPath;
    public String attachPath;
    public int bitRate;
    public float maxSize;
    public int frameRate;

    public VideoReqCompressionInfo(String videoPath, String attachPath, int bitRate, int maxSize) {
        this.videoPath = videoPath;
        this.attachPath = attachPath;
        this.bitRate = bitRate;
        this.maxSize = maxSize;
    }

    public VideoReqCompressionInfo(String videoPath, String attachPath, int bitRate, int maxSize, int frameRate) {
        this.videoPath = videoPath;
        this.attachPath = attachPath;
        this.bitRate = bitRate;
        this.maxSize = maxSize;
        this.frameRate = frameRate;
    }
}
