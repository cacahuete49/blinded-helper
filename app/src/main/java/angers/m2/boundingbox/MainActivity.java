package angers.m2.boundingbox;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.util.Log;
import android.util.SizeF;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import angers.m2.boundingbox.algo.Algorithm;
import angers.m2.boundingbox.debug.Tools;
import angers.m2.boundingbox.tools.MatComparator;
import angers.m2.boundingbox.tools.Speaker;
import angers.m2.boundingbox.tools.Vista;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, SensorEventListener, View.OnTouchListener {

    /**
     * DEBUGAGE: Affiche la matrice dans une seconde partie de la vue
     */
    class OneShotTask implements Runnable {
        Bitmap bmp;
        ImageView image;

        OneShotTask(ImageView img, Mat src) {
            this.image = img;
            this.bmp = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(src, this.bmp);
        }

        public void run() {
            if (this.bmp != null && BuildConfig.DEBUG)
                this.image.setImageBitmap(this.bmp);
        }
    }

    CameraBridgeViewBase cam0;
    private Mat mRgba;
    private Vista vista = new Vista();
    private Speaker speaker;
    public static ArrayList<String> obstacle = new ArrayList<>();

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("opencv", "OpenCV loaded successfully");
                    if (BuildConfig.DEBUG) {
                        cam0.enableView();
                        cam0.enableFpsMeter();
                    }
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    private TextToSpeech ttobj;
    private boolean clic = false;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ttobj.shutdown();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout screen = (LinearLayout) findViewById(R.id.screen);
        screen.setOnTouchListener(this);

        cam0 = (CameraBridgeViewBase) findViewById(R.id.java_surface_view0);
        cam0.setCvCameraViewListener(this);

        speaker = Speaker.getInstance(this);
        vista.initialize(this);

        // init camera2
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics = null;
        try {
            characteristics = cameraManager.getCameraCharacteristics(cameraManager.getCameraIdList()[0]);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        StreamConfigurationMap configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        SizeF sizeF = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);

        float[] f = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        double result = Math.toDegrees(2 * Math.atan(sizeF.getWidth() / (2 * f[0])));

        //first Size under 500px height
        boolean shunt = false;
        for (int idxSize : configs.getOutputFormats()) {
            for (android.util.Size size : configs.getOutputSizes(idxSize)) {
                if (shunt = (size.getHeight() < 500)) {
                    cam0.setMaxFrameSize(size.getWidth(), size.getHeight());
                    break;
                }
            }
            if (shunt) break;
        }

//        Calcul de la meilleur résolution pour faire du HighFPS
//        if (configs.getHighSpeedVideoSizes().length > 0) {
//            android.util.Size size = configs.getHighSpeedVideoSizes()[configs.getHighSpeedVideoSizes().length - 1];
//            cam0.setMaxFrameSize(size.getWidth(), size.getHeight());
//        }

        ttobj = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR)
                    ttobj.setLanguage(Locale.FRANCE);
            }
        });

    }

    /**
     * Traitement principal
     *
     * @param src
     */
    public void load_AND_display(Mat src) {
        try {
            mRgba = Algorithm.formRecognition(src, speaker, clic);
        } catch (Throwable e) {
            /**
             * Nécessaire pour catcher les différentes erreurs de la librairie
             */
            Log.getStackTraceString(e);
        }
    }


    /**
     * Calcul le Kmeans de la zone sous l'horizon pour mettre en avant les obstacles.
     * TODO: à terminer :'(
     *
     * @param src entre de la camera
     */
    @NonNull
    private void findObstacle(Mat src) {
        /**
         * https://github.com/badlogic/opencv-fun/blob/master/src/pool/tests/Cluster.java
         */
        Mat tmpMat = Algorithm.stuffFinder(src.clone());

        List<MatOfPoint> contours = new ArrayList<>();

        Imgproc.cvtColor(tmpMat, tmpMat, Imgproc.COLOR_RGB2GRAY, 1);
//            Imgproc.threshold(tmpMat, tmpMat, 0, 255, Imgproc.THRESH_OTSU);
        Imgproc.findContours(tmpMat, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.cvtColor(tmpMat, tmpMat, Imgproc.COLOR_GRAY2RGB);

        Collections.sort(contours, new MatComparator());

        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint e = contours.get(i);
            RotatedRect rotatedRect = Imgproc.minAreaRect(new MatOfPoint2f(e.toArray()));

            if (Algorithm.isNotTooBig(rotatedRect, tmpMat) && Algorithm.isNotTooSmall(rotatedRect, tmpMat)) {
                Rect rect = new Rect(0, 0, (int) rotatedRect.size.width, (int) rotatedRect.size.height);
                Imgproc.rectangle(tmpMat, rect.br(), rect.tl(), new Scalar(255, 0, 0));
            }

        }

        // output
        this.runOnUiThread(new OneShotTask((ImageView) findViewById(R.id.imageView), tmpMat));

    }

    @Override
    public void onStart() {
        super.onStart();
        vista.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
    }

    @Override
    public void onStop() {
        super.onStop();
        vista.stop();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        vista.cameraViewStarted(width, height, 20);
    }

    @Override
    public void onCameraViewStopped() {
        releaseMats();

    }

    public void releaseMats() {
        mRgba.release();
    }

    int fps = 0;
    long timer = SystemClock.currentThreadTimeMillis();

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        fpsMeter();

        // repositionnement de l'horizon
        vista.cameraFrame(90f);

        // extraction de la Mat
        mRgba = inputFrame.rgba();

        // traitement
        load_AND_display(mRgba);

        return mRgba;
    }

    private void fpsMeter() {
        if (SystemClock.currentThreadTimeMillis() - timer > 1000) {
            Log.i("FPS", "FPS:" + fps);
            fps = 0;
            timer = SystemClock.currentThreadTimeMillis();
        } else {
            fps++;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        vista.sensorChanged(event);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        startTimer();
        return false;
    }

    /**
     * Speacker onclic méthode
     */
    public void startTimer() {
        obstacle.clear();
        final boolean[] find = {false};
        ttobj.speak("Détection en cours !", TextToSpeech.QUEUE_FLUSH, null, "none");
        new CountDownTimer(3000, 1000) {
            public void onTick(long millisUntilFinished) {
                Log.d("obstacle", "obstacle size" + obstacle.size());
                if (!obstacle.isEmpty()) {
                    ttobj.speak(obstacle.get(obstacle.size() - 1), TextToSpeech.QUEUE_ADD, null, "findObject");
                    obstacle.clear();
                    find[0] = true;
                }
            }

            public void onFinish() {
                if (!find[0]) {
                    ttobj.speak("Aucun objet n'a été détecté !", TextToSpeech.QUEUE_FLUSH, null, "findObject");
                }
            }
        }.start();
    }
}
