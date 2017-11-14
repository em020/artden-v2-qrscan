package yimin.sun.artdenv2qrscan.sample;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.common.logging.FLog;
import com.facebook.common.util.ByteConstants;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.listener.RequestLoggingListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.HashSet;
import java.util.Set;

import yimin.sun.artdenv2qrscan.QRScanActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ImagePipelineConfig.Builder builder = ImagePipelineConfig.newBuilder(this);
        builder.setDownsampleEnabled(true);

        DiskCacheConfig diskCacheConfig = DiskCacheConfig.newBuilder(this).setMaxCacheSize(128 * ByteConstants.MB).build();
        builder.setMainDiskCacheConfig(diskCacheConfig);

        if (false) {

            //log查看
            Set<RequestListener> requestListeners = new HashSet<>();
            requestListeners.add(new RequestLoggingListener());
            builder.setRequestListeners(requestListeners);
        }

        ImagePipelineConfig config = builder.build();
        Fresco.initialize(this, config);

        if (false) {
            FLog.setMinimumLoggingLevel(FLog.VERBOSE);
        }
    }

    public void startScan(View view) {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setOrientationLocked(false);
        integrator.setCaptureActivity(QRScanActivity.class);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IntentIntegrator.REQUEST_CODE) {

            // 由相机扫到了二维码信息
            if (resultCode == Activity.RESULT_OK) {

                // handle scan qr-code result
                IntentResult qrScanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                if (qrScanResult != null && !TextUtils.isEmpty(qrScanResult.getContents())) {

                    String qrInfo = qrScanResult.getContents();
                    handleQRScanResult(qrInfo);
                }
            }

            // 解析了相册图片中的二维码信息
            if (resultCode == QRScanActivity.RECOGNIZED_FROM_IMAGE) {

                String qrInfo = data.getStringExtra("result");
                handleQRScanResult(qrInfo);
            }
        }
    }

    private void handleQRScanResult(String qrInfo) {
        Toast.makeText(this, qrInfo, Toast.LENGTH_SHORT).show();
    }
}
