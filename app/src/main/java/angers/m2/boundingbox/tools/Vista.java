package angers.m2.boundingbox.tools;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Vista {
    private Activity activity;

    private SensorManager sensorManager;
    private Sensor sensorAccelerometre;
    private Sensor sensorMagnetometre;
    private Sensor sensorGravity;

    private float[] accelerometreValues = new float[3];
    private float[] magnetometreValues = new float[3];
    private float[] gravityValues = new float[3];
    private float[] resultMatrix = new float[9];
    private float[] gyroscopeValues = new float[3];

    private double angleVision;
    private List<Float> pileGyroscope;

    private static Rect vista;

    public static final int INSIDE = 0;
    public static final int EXTERN_UP = 1;
    public static final int EXTERN_DOWN = -1;
    private static final String TAG = "VISTA";

    /**
     * Classe de gestion de l'horizon
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
                sensorGravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

                CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(manager.getCameraIdList()[0]);

                angleVision = Math.toDegrees(2 * Math.atan(characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE).getWidth() / (2 * characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)[0])));
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
            sensorManager.registerListener((SensorEventListener) activity, sensorMagnetometre, SensorManager.SENSOR_DELAY_FASTEST);
            sensorManager.registerListener((SensorEventListener) activity, sensorGravity, SensorManager.SENSOR_DELAY_FASTEST);
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
            sensorManager.unregisterListener((SensorEventListener) activity, sensorGravity);
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
            } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY) {
                gravityValues = sensorEvent.values;
            }

            Log.d("GRAVITY", gravityValues[0] + " - " + gravityValues[1] + " - " + gravityValues[2]);
            Log.d("GYROS", Math.toDegrees(gyroscopeValues[0]) + " - " + Math.toDegrees(gyroscopeValues[1]) + " - " + Math.toDegrees(gyroscopeValues[2]));

            SensorManager.getRotationMatrix(resultMatrix, null, accelerometreValues, magnetometreValues);
            SensorManager.getOrientation(resultMatrix, gyroscopeValues);

            pileGyroscope.add((float) Math.abs(Math.toDegrees(gyroscopeValues[1])));
        } else {
            Log.e(TAG, "Erreur, l'initialisation n'a pas été effectuée !");
        }
    }

    /**
     * Calcul l'horizon en fonction de la position du téléphone.
     *
     * @param mat            Matrice représentant la trame d'entrée de la caméra.
     * @param angleCalibrage Angle du téléphone pour le calibrage de la librarie.
     * @param percent        Pourcentage de marge d'erreur du rectangle correspondant à l'horizon.
     */
    public void cameraFrame(Mat mat, float angleCalibrage, float percent) {
        cameraFrame(mat, angleCalibrage, percent, false);
    }

    /**
     * Calcul l'horizon en fonction de la position du téléphone.
     *
     * @param mat            Matrice représentant la trame d'entrée de la caméra.
     * @param angleCalibrage Angle du téléphone pour le calibrage de la librarie.
     * @param percent        Pourcentage de marge d'erreur du rectangle correspondant à l'horizon.
     * @param display        Afficher le rectangle correspondant à l'horizon.
     */
    public void cameraFrame(Mat mat, float angleCalibrage, float percent, boolean display) {
        if (activity != null) {
            // Calcul des variables
            float margePixel = (mat.width() * percent / 100);
            double PixelByDegree = mat.width() / angleVision;

            // Calcul de l'angle d'orientation du matériel
            float currentAngle = getAverageGyroscope();
            currentAngle = gravityValues[2] < 0 ? Math.abs(currentAngle - angleCalibrage) + angleCalibrage : currentAngle;


            // Calcul de la valeur de l'horizon
            double valeur = (currentAngle - angleCalibrage) * PixelByDegree + (mat.width() / 2);
            Point point = new Point(valeur, 0);

            Log.d("GYROS2", valeur + "");

            // Création du rectangle correspondant à la ligne d'horizon plus la marge d'erreur
            vista = new Rect((int) point.x - (int) margePixel / 2, (int) point.y, (int) margePixel, mat.width());

            if (display) {
                // Affichage du rectangle
                Point point2 = new Point(valeur, mat.height() - 1);
                Imgproc.rectangle(mat, new Point(vista.x, vista.y), new Point(vista.x + vista.width, vista.y + vista.height), new Scalar(255, 0, 0), 2);
                Imgproc.line(mat, point, point2, new Scalar(255, 0, 0, 255), 3);
            }
        } else {
            Log.e(TAG, "Erreur, l'initialisation n'a pas été effectuée !");
        }
    }

    /**
     * Vérifie si un point est contenu, au-dessus ou en dessous de l'horizon.
     *
     * @param mat   Matrice représentant la trame d'entrée de la caméra.
     * @param point point à tester.
     * @return 0 (contenu), 1 (au dessus) ou -1 (en dessous)
     */
    public static int getPositionPoint(Mat mat, Point point) {
        ArrayList<Point> listPosition = new ArrayList<>();
        listPosition.add(point);

        return getPositionPoint(mat, listPosition).get(0);
    }

    /**
     * Vérifie si une liste de points est contenue, au-dessus ou en dessous de l'horizon.
     *
     * @param mat       Matrice représentant la trame d'entrée de la caméra.
     * @param listPoint Liste de points à tester.
     * @return Une liste de 0 (contenu), 1 (au dessus) ou -1 (en dessous) correspondant à la liste de points.
     */
    public static ArrayList<Integer> getPositionPoint(Mat mat, ArrayList<Point> listPoint) {
        if (vista != null) {
            ArrayList<Integer> listPosition = new ArrayList<>();
            for (Point P : listPoint) {
                if (vista.contains(P)) {
                    listPosition.add(INSIDE);
                } else {
                    listPosition.add(P.y > vista.y ? EXTERN_UP : EXTERN_DOWN);
                }
            }

            return listPosition;
        } else {
            Log.e(TAG, "Erreur, la méthode CameraFrame n'est pas appelée !");
            return null;
        }
    }

    /**
     * Calcul une sous matrice de la matrice passé en paramétre en fonction de la position souhaité par rapport à l'horizon
     *
     * @param mat      Matrice représentant la trame d'entrée de la caméra.
     * @param position 0 (contenu), 1 (au dessus) ou -1 (en dessous).
     * @return Une sous matrice de la matrice mat en fonction de la position.
     */
    public static Mat getSubMat(Mat mat, int position) {
        if (vista != null) {
            if ((vista.x > 0) && (vista.x < mat.height())) {
                if (position == INSIDE) {
                    return mat.submat(vista);
                } else if (position == EXTERN_UP) {
                    return mat.submat(0, mat.height()-1,0, vista.x);
                } else {
                    return mat.submat(0, mat.height()-1,vista.x, mat.width()-1);
                }
            } else {
                Log.e(TAG, "Erreur la matrice n'est pas proportionnée !");
            }
        } else {
            Log.e(TAG, "Erreur, la méthode CameraFrame n'est pas appelée !");
        }
        return null;
    }

    /**
     * Retourne les angles en degré du téléphone.
     *
     * @return Un tableau de 3 float correspondant aux angles d'orientation X, Y et Z.
     */
    public float[] getAngleOrientation() {
        if (activity != null) {
            return gyroscopeValues;
        } else {
            Log.e(TAG, "Erreur, l'initialisation n'a pas été effectuée !");
            return null;
        }
    }

    /**
     * Calcul la moyenne du gyroscope.
     * @return La moyenne du gyroscope.
     */
    private float getAverageGyroscope() {
        ArrayList<Float> pileGyroscopeCopy = new ArrayList<>(pileGyroscope);
        pileGyroscope.clear();

        float currentAngle = 0;
        for (float f : pileGyroscopeCopy) {
            currentAngle +=f;
        }
        return currentAngle /pileGyroscopeCopy.size();
    }
}