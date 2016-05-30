package angers.m2.boundingbox.algo;

import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import angers.m2.boundingbox.form.DoorForm;
import angers.m2.boundingbox.form.WindowForm;
import angers.m2.boundingbox.tools.Kmeans;
import angers.m2.boundingbox.tools.MatComparator;
import angers.m2.boundingbox.tools.Vista;

/**
 * Created by cacahuete on 30/05/16.
 */
public class Algorithm {

    public static Mat formRecognition(Mat original, TextToSpeech ttobj) {
        /** le calcul du range ce fait avec le retour du threshold
         * cf http://www.academypublisher.com/proc/isip09/papers/isip09p109.pdf
         * la valeur min est de maniere empirique la moitier.
         */
        Mat src = original.clone();
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

        Collections.sort(contours, new MatComparator());

        int maxElement = 5;
//        deleteInsideForm(contours, maxElement);
        for (int i = 0; i < maxElement && i < contours.size(); i++) {
            MatOfPoint e = contours.get(i);

            // boite englobante
            Rect rect = Imgproc.boundingRect(e);

            // boite englobante avec inclinaison =)
            RotatedRect rotRect = Imgproc.minAreaRect(new MatOfPoint2f(e.toArray()));
            Point[] vertices = new Point[4];
            rotRect.points(vertices);

            // controle sur la taille des objets pour éviter le bruit mis en boite
            if (isNotTooBig(rotRect, original)) {

                Scalar color = null;
                if (DoorForm.getInstance().isRecognized(rotRect, tmp)) {
//                    ttobj.speak("la porte est située en " + Speaker.locate(rotRect.center, mRgba), TextToSpeech.QUEUE_FLUSH, null, "findDoor");
                    ttobj.speak("porte", TextToSpeech.QUEUE_FLUSH, null, "findDoor");
                    color = new Scalar(0, 0, 255);
                } else if (WindowForm.getInstance().isRecognized(rotRect, original)) {
                    color = new Scalar(0, 0, 255);
//                    ttobj.speak("la fenêtre est située en " + Speaker.locate(rotRect.center, mRgba), TextToSpeech.QUEUE_FLUSH, null, "findWindows");
                    ttobj.speak("fenêtre", TextToSpeech.QUEUE_FLUSH, null, "findWindows");
                }


                if (rect.area() * 0.85 < rotRect.size.height * rotRect.size.width) {
                    Imgproc.rectangle(tmp, rect.br(), rect.tl(), (color == null ? new Scalar(255, 0, 0) : color), 3);
                } else {
                    for (int j = 0; j < 4; j++) {
                        Imgproc.line(tmp, vertices[j], vertices[(j + 1) % 4], (color == null ? new Scalar(0, 255, 0) : color));
                    }
                }
            } else {
                Log.d("size", "Too big element");
            }
        }
        return tmp;
    }

    public static Mat stuffFinder(Mat src) {

        Mat tmp = new Mat();
        Mat resizedTmp = Vista.getSubMat(src.clone(), new Point(Vista.getVista().x + Vista.getVista().width + (src.width() / 5), 0));

        if (resizedTmp.isSubmatrix() && resizedTmp.width() > 0 && resizedTmp.height() > 0) {
            // pré traitement
            Mat resized = resizedTmp.clone();
            Imgproc.resize(resizedTmp, resized, new Size(resizedTmp.size().width / 4.0f, resizedTmp.size().height / 4.0f));
            Imgproc.dilate(resized, resized, new Mat(), new Point(), 2);

            // Traitement
            int k = 3;
            List<Mat> clusters = Kmeans.cluster(resized, k);

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

            // fonctionne draw contour
            resizedTmp = clusters.get(maxNonZeroIndex).clone();

        }
        return resizedTmp;
    }

    /**
     * @param rotatedRect
     * @param mat
     * @return true if object isn't too big
     */
    public static boolean isNotTooBig(@NonNull RotatedRect rotatedRect, @NonNull Mat mat) {
        return rotatedRect.size.height * rotatedRect.size.width < mat.height() * mat.width() * 0.6 && rotatedRect.size.width < mat.width() * 0.90 && rotatedRect.size.height < mat.height() * 0.90;
    }

    /**
     * @param rotatedRect
     * @param mat
     * @return true if object isn't too small
     */
    public static boolean isNotTooSmall(@NonNull RotatedRect rotatedRect, @NonNull Mat mat) {
        return rotatedRect.size.height * rotatedRect.size.width > mat.height() * mat.width() * 0.1;
    }


    public static List<Rect> deleteInsideForm(List<MatOfPoint> listMatOfPoint, int max) {
        Point[] point1 = new Point[4];
        Point[] point2 = new Point[4];

        ArrayList<Boolean> test = new ArrayList<>();
        Collections.fill(test, true);

        for (int i = 0; i < max && i < listMatOfPoint.size(); i++) {
            RotatedRect rectA = Imgproc.minAreaRect(new MatOfPoint2f(listMatOfPoint.get(i).toArray()));

            for (int j = i + 1; j < max && j < listMatOfPoint.size(); j++) {
                RotatedRect rectB = Imgproc.minAreaRect(new MatOfPoint2f(listMatOfPoint.get(j).toArray()));

                if (rectA.boundingRect().tl().x < rectB.boundingRect().br().x && rectA.boundingRect().br().x > rectB.boundingRect().tl().x &&
                        rectA.boundingRect().tl().y < rectB.boundingRect().br().y && rectA.boundingRect().br().y > rectB.boundingRect().tl().y) {


                    if (test.get(i)) {
                        test.remove(i);
                        test.add(i, false);
                    }
                    break;
                }
            }
        }


        for (boolean i : test)
            Log.d("INTERSECT", i ? "YES" : "NO");

        Log.d("INTERSECT", "\n\n\n");


        return new ArrayList<>();

    }

    public static ArrayList<RotatedRect> getSortListForm(List<MatOfPoint> listMatOfPoint, int max) {
        ArrayList<RotatedRect> listForm = new ArrayList<>();

        for (int i = 0; i < max && i < listMatOfPoint.size(); i++) {
            listForm.add(Imgproc.minAreaRect(new MatOfPoint2f(listMatOfPoint.get(i).toArray())));
        }

        Collections.sort(listForm, new Comparator<RotatedRect>() {
            @Override
            public int compare(RotatedRect rectA, RotatedRect rectB) {
                if (rectA.center.y < rectB.center.y) {
                    return -1;
                } else if (rectA.center.y > rectB.center.y) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        return listForm;
    }
}