package chengdu.ws.mediacompress.telegram;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.telegram.messenger.MediaController;
import org.telegram.messenger.VideoConvertUtil;
import org.telegram.messenger.VideoEditedInfo;
import org.telegram.messenger.VideoReqCompressionInfo;

import java.io.File;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private TextView infoTV;
    private Button telegramBTN;
    private String videoPath;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_activity_main);
        infoTV = findViewById(R.id.tv_info);
        telegramBTN = findViewById(R.id.btn_telegram);
        Button btnSelect = findViewById(R.id.btn_select);
        btnSelect.setOnClickListener(v -> pickVideoFromGallery());

        telegramBTN.setOnClickListener(view -> {
            File dir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "convert"
            );
            if (!dir.exists() && !dir.mkdir()) return;
            File vi = new File(dir, UUID.randomUUID().toString()+".mp4");
//            compress();
            VideoReqCompressionInfo info = new VideoReqCompressionInfo(videoPath, vi.getAbsolutePath(), 1_000_000, 720);
            Integer telegramId = VideoConvertUtil.startVideoConvert(info, new MediaController.ConvertorListener() {
                @Override
                public void onConvertStart(VideoEditedInfo info, float progress, long lastFrameTimestamp) {
                    Log.d("convertInfo: ", info.toString());
                }

                @Override
                public void onConvertProgress(VideoEditedInfo info, long availableSize, float progress, long lastFrameTimestamp) {
                    infoTV.setText("Compress progress: " + progress);
                }

                @Override
                public void onConvertSuccess(VideoEditedInfo info, long fileLength, long lastFrameTimestamp) {
                    infoTV.setText("Compress success: " + info.toString());
                }

                @Override
                public void onConvertFailed(VideoEditedInfo info, float progress, long lastFrameTimestamp) {
                    infoTV.setText("Compress failed: " + info.toString());

                }
            });
            if (telegramId == null) {
                infoTV.setText("Compress start failed:");
            }
        });
    }

    private static final int REQUEST_PICK_VIDEO = 1;

    private void pickVideoFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_VIDEO && resultCode == RESULT_OK && data != null) {
            Uri selectedVideoUri = data.getData();
            String[] projection = {MediaStore.Video.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedVideoUri, projection, null, null, null);
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            videoPath = cursor.getString(columnIndex);
            cursor.close();
        }
    }

}
