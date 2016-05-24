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
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SizeF;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import angers.m2.boundingbox.Tools.Speaker;
import angers.m2.boundingbox.Tools.Vista;
import angers.m2.boundingbox.form.DoorForm;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, SensorEventListener {

    // TODO a jeter après les test
    class OneShotTask implements Runnable {
        Bitmap bmp;

        OneShotTask(Bitmap b) {
            this.bmp = b;
        }

        public void run() {
            if (bmp != null)
                image.setImageBitmap(bmp);
        }
    }

    CameraBridgeViewBase cam0;

    private Mat mRgba;

    private Vista vista = new Vista();

    private DoorForm doorsingleton;

    private ImageView image;

    private void initSingleton() {
        doorsingleton = DoorForm.getInstance();
    }

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
        cam0 = (CameraBridgeViewBase) findViewById(R.id.java_surface_view0);
        cam0.setCvCameraViewListener(this);

        image = (ImageView) findViewById(R.id.imageView);

        try {
            vista.initialize(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        initSingleton();

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

        Log.d("photo", "" + sizeF.toString());
        float[] f = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        Log.d("photo", "" + f.length);
        double result = Math.toDegrees(2 * Math.atan(sizeF.getWidth() / (2 * f[0])));
        Log.d("photo", "" + result);

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
        }
        );

    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("opencv", "OpenCV loaded successfully");
                    cam0.enableView();
                    cam0.enableFpsMeter();
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

    private long timer = 0;

    public void load_AND_display(Mat src) {


        findObstacle(src);
        Mat tmp = new Mat();

        Imgproc.cvtColor(src,tmp,Imgproc.COLOR_RGB2GRAY);
        Imgproc.threshold(tmp, tmp, 192, 255, Imgproc.THRESH_BINARY);
//        Imgproc.adaptiveThreshold(tmp,mRgba,255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,Imgproc.THRESH_BINARY_INV, 19, 2);


        tmp = findForm(src);

        mRgba = tmp;
    }

    @NonNull
    private Mat findForm(Mat src) {
        /** le calcul du range ce fait avec le retour du threshold
         * cf http://www.academypublisher.com/proc/isip09/papers/isip09p109.pdf
         * la valeur min est de maniere empirique la moitier.
         */

        Mat tmp = new Mat();
        Mat threshold = new Mat();
        Imgproc.cvtColor(src, threshold, Imgproc.COLOR_RGB2GRAY, 1);

        // vire les details insignifiant
        Imgproc.erode(threshold, threshold, new Mat(), new Point(), 4);
        Imgproc.dilate(threshold, threshold, new Mat(), new Point(), 1);

        double hightThreshold = Imgproc.threshold(threshold, new Mat(), 0, 255, Imgproc.THRESH_OTSU);
        Imgproc.Canny(src, tmp, hightThreshold / 2, hightThreshold);

        Imgproc.dilate(tmp, tmp, new Mat(), new Point(), 2);

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(tmp, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_GRAY2RGB);

        Collections.sort(contours, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint lhs, MatOfPoint rhs) {
                double area1 = Imgproc.contourArea(lhs);
                double area2 = Imgproc.contourArea(rhs);
                if (area1 < area2)
                    return 1;
                else if (area1 > area2)
                    return -1;
                return 0;
            }

            @Override
            public boolean equals(Object object) {
                return super.equals(object);
            }
        });

        for (int i = 0; i < 8 && i < contours.size(); i++) {
            MatOfPoint e = contours.get(i);

            // boite englobante
            Rect rect = Imgproc.boundingRect(e);

            // boite englobante avec inclinaison =)
            RotatedRect rotRect = Imgproc.minAreaRect(new MatOfPoint2f(e.toArray()));
            Point[] vertices = new Point[4];
            rotRect.points(vertices);
            double l1 = rotRect.size.height;//Math.sqrt(Math.pow(vertices[1].x - vertices[0].x, 2) + Math.pow(vertices[1].y - vertices[0].y, 2));
            double l2 = rotRect.size.width;//Math.sqrt(Math.pow(vertices[1].x - vertices[2].x, 2) + Math.pow(vertices[1].y - vertices[2].y, 2));

            if (l1 * l2 < src.height() * src.width() * 0.4 && l2 < src.width() * 0.95 && l1 < src.height() * 0.95) {

                Scalar color = null;
                double div = l1 / l2;
                if (doorsingleton.isRecognized(rotRect, mRgba)) {
                    ttobj.speak("la porte est située en " + Speaker.locate(rotRect.center, mRgba), TextToSpeech.QUEUE_ADD, null, "findDoor");
                    color = new Scalar(0, 0, 255);
                }


                if (l1 * l2 > src.height() / 10 * src.width() / 10) {
                    if (rect.area() * 0.85 < l1 * l2) {
                        Imgproc.rectangle(tmp, rect.br(), rect.tl(), (color == null ? new Scalar(255, 0, 0) : color), 3);
                    } else {
                        for (int j = 0; j < 4; j++) {
                            Imgproc.line(tmp, vertices[j], vertices[(j + 1) % 4], (color == null ? new Scalar(0, 255, 0) : color));
                        }
                    }
                }
            } else {
                Log.d("size", "Too big element");
            }
        }
        return tmp;
    }

    /**
     * Calcul le Kmeans de la zone sous l'horizon pour mettre en avant les obstacles.
     * @param src entre de la camera
     */
    @NonNull
    private void findObstacle(Mat src) {
        /**
         * https://github.com/badlogic/opencv-fun/blob/master/src/pool/tests/Cluster.java
         */

        Mat tmp = new Mat();
        Mat resizedTmp = Vista.getSubMat(src, Vista.EXTERN_DOWN);

        if (resizedTmp != null) {
            // pré traitement
            Mat resized = new Mat();
            Imgproc.resize(resizedTmp, resized, new Size(resizedTmp.size().width / 10, resizedTmp.size().height / 10));
            Imgproc.dilate(resized, resized, new Mat(), new Point(), 2);

            // init des vars
            TermCriteria criteria = new TermCriteria(TermCriteria.COUNT, 100, 1);
            Mat centers = new Mat();
            Mat labels = new Mat();
            Mat samples = resized.reshape(1, resized.cols() * resized.rows());
            Mat samples32f = new Mat();
            samples.convertTo(samples32f, CvType.CV_32F, 1.0 / 255.0);

            // calcul
            int k = 3;
            Core.kmeans(samples32f, k, labels, criteria, 1, Core.KMEANS_PP_CENTERS, centers);

            // reconstruction de la matrice en 2 dimension
            centers.convertTo(centers, CvType.CV_8UC1, 255.0);
            try {
                centers.reshape(3);
            } catch (CvException e) {
                throw e;
            }

            List<Mat> clusters = new ArrayList<>();
            for (int i = 0; i < centers.rows(); i++) {
                clusters.add(Mat.zeros(resized.size(), resized.type()));
            }

            Map<Integer, Integer> counts = new HashMap<>();

            for (int i = 0; i < centers.rows(); i++)
                counts.put(i, 0);


            int rows = 0;
            for (int y = 0; y < resized.rows(); y++) {
                for (int x = 0; x < resized.cols(); x++) {
                    int label = (int) labels.get(rows++, 0)[0];
                    counts.put(label, counts.get(label + 1));
                    clusters.get(label).put(y, x, (int) centers.get(label, 0)[0], (int) centers.get(label, 1)[0], (int) centers.get(label, 2)[0], (int) centers.get(label, 3)[0]);
                }
            }

            // identification de la frame avec le plus de couleur elle sera le sol
            float maxNonZeroPercent = 0f;
            int maxNonZeroIndex = -1;

            for (int i = 0; i < k; i++) {
                Mat frame = clusters.get(i);

                Imgproc.cvtColor(frame, tmp, Imgproc.COLOR_RGB2GRAY);

                long count = Core.countNonZero(tmp);
                float res = count / (float) tmp.total();

                if (res > maxNonZeroPercent) {
                    maxNonZeroPercent = res;
                    maxNonZeroIndex = i;
                }
            }

            Mat tmpTest = new Mat();
            Imgproc.resize(clusters.get(maxNonZeroIndex), tmpTest, new Size(resizedTmp.size().width, resizedTmp.size().height));
            Bitmap bmp = Bitmap.createBitmap(tmpTest.cols(), tmpTest.rows(), Bitmap.Config.ARGB_8888);

            Utils.matToBitmap(tmpTest, bmp);

            // output
            this.runOnUiThread(new OneShotTask(bmp));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            vista.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            vista.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        releaseMats();

    }

    public void releaseMats() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        load_AND_display(mRgba);

        try {
            vista.cameraFrame(mRgba, 86f, 15, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mRgba;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        try {
            vista.sensorChanged(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
