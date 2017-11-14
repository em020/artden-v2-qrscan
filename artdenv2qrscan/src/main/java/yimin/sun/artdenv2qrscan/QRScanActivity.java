package yimin.sun.artdenv2qrscan;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import yimin.sun.artdenv2album.AlbumActivity;
import yimin.sun.handyactionbar.HandyActionBarLayout;
import yimin.sun.statusbarfucker.StatusBarFucker;

/**
 * 如何启动扫码页面 和 如何获取扫描结果 请看本类源码的末尾说明
 */
public class QRScanActivity extends AppCompatActivity {

    public static final int RECOGNIZED_FROM_IMAGE = 300;

    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qrs_activity_qrscan);

        StatusBarFucker fucker = new StatusBarFucker();
        fucker.setWindowExtend(1);
        fucker.setStatusBarColor(Color.TRANSPARENT);
        fucker.fuck(getWindow());

        HandyActionBarLayout hab = (HandyActionBarLayout) findViewById(R.id.ab);
        hab.setXWithImage(HandyActionBarLayout.POSITION_L, R.drawable.qrs_ic_back_white, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        hab.setXWithText(HandyActionBarLayout.POSITION_M, "二维码", null);
        hab.setXWithText(HandyActionBarLayout.POSITION_R1, "相册", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(getApplicationContext(), AlbumActivity.class), 1234);
            }
        });
        TextView title = (TextView) hab.getViewAt(HandyActionBarLayout.POSITION_M);
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);

        TextView rightAction = (TextView) hab.getViewAt(HandyActionBarLayout.POSITION_R1);
        rightAction.setTextColor(Color.WHITE);
        rightAction.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);


        barcodeScannerView = (DecoratedBarcodeView)findViewById(R.id.zxing_barcode_scanner);
        Switch switchFlashLight = (Switch) findViewById(R.id.switch_flash_light);


        // if the device does not have flashlight in its camera,
        // then remove the switch flashlight button...
        if (!hasFlash()) {
            switchFlashLight.setVisibility(View.GONE);
        }


        switchFlashLight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    barcodeScannerView.setTorchOn();
                } else {
                    barcodeScannerView.setTorchOff();
                }
            }
        });


        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        capture.decode();
    }

    @Override
    protected void onResume() {
        super.onResume();
        capture.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        capture.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        capture.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        capture.onSaveInstanceState(outState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        /**
         *
         * Intent intent = new Intent();
         intent.putExtra("result_path", path);
         setResult(RESULT_OK, intent);
         finish();
         */

        if (requestCode == 1234 && resultCode == RESULT_OK) {

            String imagePath = data.getStringExtra("result_path");

            final Dialog dialog = ProgressDialog.show(this, null, "正在识别，请稍候...");

            new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... params) {
                    Bitmap bitmap = BitmapDownSampler.decodeSampledBitmapFromFile(params[0], 200, 200);
                    return readQRImage(bitmap);
                }

                @Override
                protected void onPostExecute(String s) {
                    super.onPostExecute(s);
                    dialog.dismiss();
                    Intent data  = new Intent();
                    data.putExtra("result", s);
                    setResult(RECOGNIZED_FROM_IMAGE, data);
                    finish();
                }

            }.execute(imagePath);
        }
    }

    /**
     * Check if the device's camera has a Flashlight.
     * @return true if there is Flashlight, otherwise false.
     */
    private boolean hasFlash() {
        return getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }



    private static String readQRImage(Bitmap bMap) {

        String contents = null;

        int[] intArray = new int[bMap.getWidth()*bMap.getHeight()];

        //copy pixel data from the Bitmap into the 'intArray' array
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());

        LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(), intArray);

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Reader reader = new MultiFormatReader();// use this otherwise ChecksumException

        try {
            Result result = reader.decode(bitmap);
            contents = result.getText();

            //byte[] rawBytes = result.getRawBytes();
            //BarcodeFormat format = result.getBarcodeFormat();
            //ResultPoint[] points = result.getResultPoints();

        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (ChecksumException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        }

        return contents;
    }

    /*public void switchFlashlight(View view) {
        if (getString(R.string.turn_on_flashlight).equals(switchFlashlightButton.getText())) {
            barcodeScannerView.setTorchOn();
        } else {
            barcodeScannerView.setTorchOff();
        }
    }*/

    static class BitmapDownSampler {

        public static Bitmap decodeSampledBitmapFromFileWithDpDimen(Context context, String filePath,
                                                                    int reqWidthDp, int reqHeightDp) {
            int widthPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, reqWidthDp,
                    context.getResources().getDisplayMetrics());

            int heightPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, reqHeightDp,
                    context.getResources().getDisplayMetrics());

            return decodeSampledBitmapFromFile(filePath, widthPx, heightPx);
        }


        public static Bitmap decodeSampledBitmapFromFile(String filePath, int reqWidth, int reqHeight) {

            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(filePath, options);
        }

        private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

            // Raw height and width of image
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {

                final int halfHeight = height / 2;
                final int halfWidth = width / 2;

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2;
                }
            }

            return inSampleSize;
        }
    }


/*  如何启动扫码页面

    public void startScan(View view) {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setOrientationLocked(false);
        integrator.setCaptureActivity(QRScanActivity.class);
        integrator.initiateScan();
    }


*/


/*   如何获取结果：

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


*/

}
