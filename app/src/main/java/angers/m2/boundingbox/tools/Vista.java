package angers.m2.boundingbox.tools;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.support.v4.content.res.TypedArrayUtils;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Vista {
    private Activity activity;

    private SensorManager sensorManager;
    private Sensor sensorAccelerometre;
    private Sensor sensorMagnetometre;

    private float[] accelerometreValues = new float[3];
    private float[] magnetometreValues = new float[3];
    private float[] resultMatrix = new float[9];
    private float[] resultMatrix2 = new float[9];
    private Float[] gyroscopeValues = new Float[3];

    private double angleVision;
    private List<Float[]> pileGyroscope;

    private int margePixel;
    private int pixelByDegree;
    private Size size;

    private static RotatedRect vista;

    public static final int INSIDE = 0;
    public static final int EXTERN_UP = 1;
    public static final int EXTERN_DOWN = -1;
    private static final String TAG = "VISTA";

    /**
     * Classe de gestion de l'horizon.
     *
     * @author Quentin Rabineau / Mattieu Racine
     * @version 1.0
     */
    public Vista() {
    }

    /**
     * Initialise les différentes variables de la librairie (doit être appelé dans le onCreate).
     *
     * @param activity Activité Android à laquelle est rattaché la librairie.
     * @throws Exception si la caméra n'est pas reconnue.
     */
    public void initialize(Activity activity) {
        try {
            if (activity != null) {
                this.activity = activity;
                sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
                sensorAccelerometre = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                sensorMagnetometre = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

                CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(manager.getCameraIdList()[0]);

                float diagonal = (float)Math.sqrt(Math.pow(characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE).getWidth(),2)+Math.pow(characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE).getHeight(),2));
                angleVision = Math.toDegrees(2 * Math.atan(diagonal / (2 * characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)[0])));
                pileGyroscope = Collections.synchronizedList(new ArrayList());
            } else {
                Log.e(TAG, "Erreur, l'activité ne peut pas être nulle !");
            }
        } catch (CameraAccessException cameraAccessException) {
            this.activity = null;
            Log.e(TAG, "Erreur, CameraAccessException: " + cameraAccessException.getMessage());
        }
    }

    /**
     * Activte les capteurs utilisés par la librairie (doit être appelé dans le onStart).
     */
    public void start() {
        if (activity != null) {
            sensorManager.registerListener((SensorEventListener) activity, sensorAccelerometre, SensorManager.SENSOR_DELAY_FASTEST);
            sensorManager.registerListener((SensorEventListener) activity, sensorAccelerometre, SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
            sensorManager.registerListener((SensorEventListener) activity, sensorMagnetometre, SensorManager.SENSOR_DELAY_FASTEST);
            sensorManager.registerListener((SensorEventListener) activity, sensorMagnetometre, SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
        } else {
            Log.e(TAG, "Erreur, l'initialisation n'a pas été effectuée !");
        }
    }

    /**
     * Désactive les capteurs utilisés par la librairie (doit être appelé dans le onStop).
     */
    public void stop() {
        if (activity != null) {
            sensorManager.unregisterListener((SensorEventListener) activity, sensorAccelerometre);
            sensorManager.unregisterListener((SensorEventListener) activity, sensorMagnetometre);
        } else {
            Log.e(TAG, "Erreur, l'initialisation n'a pas été effectuée !");
        }
    }

    /**
     * Initialise la taille de frame et de la marge d'erreur.
     *
     * @param width   Largeur de la frame.
     * @param height  Hauteur de la frame.
     * @param percent Pourcentage de la marge d'erreur
     */
    public void cameraViewStarted(int width, int height, int percent) {
        margePixel = (width * percent) / 100;
        pixelByDegree = (int) ( (width-margePixel) / angleVision);
        size = new Size(width, height);
        vista = new RotatedRect(new Point(width / 2, height / 2), new Size(margePixel, height), 0f);
    }

    /**
     * Calcul l'horizon en fonction de la position du téléphone.
     *
     * @param angleCalibrage Angle du t&eacute;l&eacute;phone pour le calibrage de la librarie.
     */
    public void cameraFrame(float angleCalibrage, Size size) {
        if (activity != null) {
            // Calcul de la valeur de l'horizon
            Float[] angles = getAverageGyroscope();
            int valeur = (int) ((angles[2] + angleCalibrage) * pixelByDegree + (size.width / 2));

            // Modification du rectangle correspondant à la ligne d'horizon + la marge d'erreur
            vista.center.x = valeur;
            vista.angle = -angles[1];
            vista.size.height = size.height / Math.cos(Math.toRadians(vista.angle));
        } else {
            Log.e(TAG, "Erreur, l'initialisation n'a pas été effectuée !");
        }
    }

    /**
     * Enregistre les différentes valeurs relevé par les capteurs utilisés par la librairie (doit être appelé dans le onSensorChanged).
     */
    public void sensorChanged(SensorEvent sensorEvent) {
        if (activity != null) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                accelerometreValues = sensorEvent.values;
            } else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                magnetometreValues = sensorEvent.values;
            }


            if (accelerometreValues != null && magnetometreValues != null) {
                SensorManager.getRotationMatrix(resultMatrix, null, accelerometreValues, magnetometreValues);
                int rotation = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
                switch (rotation) {
                    case Surface.ROTATION_0:
                        SensorManager.remapCoordinateSystem(resultMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Y, resultMatrix2);
                        break;
                    case Surface.ROTATION_90:
                        SensorManager.remapCoordinateSystem(resultMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_X, resultMatrix2);
                        break;
                    case Surface.ROTATION_180:
                        SensorManager.remapCoordinateSystem(resultMatrix, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, resultMatrix2);
                        break;
                    case Surface.ROTATION_270:
                        SensorManager.remapCoordinateSystem(resultMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, resultMatrix2);
                        break;
                }
                float[] tmp = new float[3];
                SensorManager.getOrientation(resultMatrix2, tmp);
                gyroscopeValues[0] = tmp[0];
                gyroscopeValues[1] = tmp[1];
                gyroscopeValues[2] = tmp[2];
            }

            pileGyroscope.add(gyroscopeValues);
        } else {
            Log.e(TAG, "Erreur, l'initialisation n'a pas été effectuée !");
        }
    }

    /**
     * Retourne le rectangle correspondant à l'horizon plus la marge d'erreur.
     *
     * @return Le rectangle correspondant à l'horizon plus la marge d'erreur.
     */
    public static RotatedRect getVista() {
        return vista;
    }

    /**
     * Vérifie si un point est contenu, au-dessus ou en dessous de l'horizon.
     *
     * @param point point à tester.
     * @return 0 (contenu), 1 (au dessus) ou -1 (en dessous)
     */
    public static int getPositionPoint(Point point) {
        ArrayList<Point> listPosition = new ArrayList<>(1);
        listPosition.add(point);

        return getPositionPoint(listPosition).get(0);
    }

    /**
     * Vérifie si une liste de points est contenue, au-dessus ou en dessous de l'horizon.
     *
     * @param listPoint Liste de points à tester.
     * @return Une liste de 0 (contenu), 1 (au dessus) ou -1 (en dessous) correspondant à la liste de points.
     */
    public static ArrayList<Integer> getPositionPoint(ArrayList<Point> listPoint) {
        ArrayList<Integer> listPosition = new ArrayList<>();
        for (Point P : listPoint) {
            if (vista.boundingRect().contains(P)) {
                listPosition.add(INSIDE);
            } else {
                listPosition.add(P.y > vista.center.y - vista.size.width / 2 ? EXTERN_UP : EXTERN_DOWN);
            }
        }

        return listPosition;
    }

    /**
     * Calcul une sous matrice de la matrice passée en paramétre en fonction de la position souhaité par rapport à l'horizon.
     *
     * @param mat      Matrice représentant la trame d'entrée de la caméra.
     * @param position 0 (contenu), 1 (au dessus) ou -1 (en dessous).
     * @return Une sous matrice de la matrice mat en fonction de la position.
     */
    public static Mat getSubMat(Mat mat, int position) {
//        try {
//            if (position == INSIDE && vista.x > 0 && vista.x < mat.height()) {
//                return mat.submat(vista);
//            }
//            if (position == EXTERN_UP && vista.x > 0 && vista.x < mat.height()) {
//                return mat.submat(0, mat.height() - 1, 0, vista.x);
//            }
//            if (position == EXTERN_DOWN && vista.x > 0 && vista.x + vista.width < mat.height()) {
//                return mat.submat(0, mat.height() - 1, vista.x + vista.width, mat.width() - 1);
//            }
//        } catch (CvException e) {
//            Log.e("CvException", "bad submat");
//            Log.getStackTraceString(e);
//        }
//        Log.e(TAG, "Erreur la matrice n'a pas les bonnes dimensions !");
        return mat;
    }

    /**
     * Calcul une sous matrice en dessous de la position d'un point passé en paramétre de la matrice.
     *
     * @param mat Matrice représentant la trame d'entrée de la caméra.
     * @param p
     * @return Une sous matrice de la matrice mat en fonction d'un point.
     */
    public static Mat getSubMat(Mat mat, Point p) {
        try {
            if (p.inside(new Rect(new Point(), mat.size()))) {
                return mat.submat(0, mat.height() - 1, (int) p.x, mat.width() - 1);
            }
        } catch (CvException e) {
            Log.e("CvException", "bad submat");
            Log.getStackTraceString(e);
        }
        return mat;
    }

    /**
     * Calcul la moyenne du gyroscope.
     *
     * @return La moyenne du gyroscope.
     */
    private Float[] getAverageGyroscope() {
        ArrayList<Float[]> pileGyroscopeCopy = new ArrayList<>(pileGyroscope);
        pileGyroscope.clear();

        Float[] currentAngle = {0f, 0f, 0f};
        for (Float[] fs : pileGyroscopeCopy) {
            currentAngle[0] += fs[0];
            currentAngle[1] += fs[1];
            currentAngle[2] += fs[2];
        }

        currentAngle[0] = (float) Math.toDegrees(currentAngle[0] / pileGyroscopeCopy.size());
        currentAngle[1] = (float) Math.toDegrees(currentAngle[1] / pileGyroscopeCopy.size());
        currentAngle[2] = (float) Math.toDegrees(currentAngle[2] / pileGyroscopeCopy.size());

        return currentAngle;
    }
}