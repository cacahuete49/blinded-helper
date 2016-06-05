package angers.m2.boundingbox.algo;

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

import angers.m2.boundingbox.MainActivity;
import angers.m2.boundingbox.form.BlockForm;
import angers.m2.boundingbox.form.DoorForm;
import angers.m2.boundingbox.form.WindowForm;
import angers.m2.boundingbox.tools.Kmeans;
import angers.m2.boundingbox.tools.MatComparator;
import angers.m2.boundingbox.tools.Speaker;
import angers.m2.boundingbox.tools.Vista;

/**
 * Created by cacahuete on 30/05/16.
 */
public class Algorithm {

    public static final Scalar BLUE = new Scalar(0, 0, 255);

    /**
     * Pré-traitement, traitement et reconnaissance
     *
     * @param original
     * @param speaker
     * @param shoot
     * @return
     */
    public static Mat formRecognition(Mat original, Speaker speaker, boolean shoot) {
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
        // compense l'erosion
        Imgproc.dilate(threshold, threshold, new Mat(), new Point(), 1);

        // défini le seuil
        double hightThreshold = Imgproc.threshold(threshold, new Mat(), 0, 255, Imgproc.THRESH_OTSU);
        Imgproc.Canny(src, tmp, hightThreshold / 2, hightThreshold);

        // augmente l'épaisseur
        Imgproc.dilate(tmp, tmp, new Mat(), new Point(), 2);

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(tmp, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        Collections.sort(contours, new MatComparator());

        Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_GRAY2RGB);

        int maxElement = 15;
        deleteInsideForm(contours, maxElement);
        int fenetre = 0;
        int porte = 0;

        for (int i = 0; i < maxElement && i < contours.size(); i++) {
            MatOfPoint e = contours.get(i);

            // boite englobante
            Rect rect = Imgproc.boundingRect(e);

            // boite englobante avec inclinaison =)
            RotatedRect rotRect = Imgproc.minAreaRect(new MatOfPoint2f(e.toArray()));
            Point[] vertices = new Point[4];
            rotRect.points(vertices);

            // controle sur la taille des objets pour éviter le bruit mis en boite
            if (isNotTooBig(rotRect, original) && isNotTooSmall(rotRect, original)) {

                Scalar color = null;
                if (DoorForm.getInstance().isRecognized(rotRect, src)) {
                    porte++;
                    MainActivity.obstacle.add(speaker.getLocation(Speaker.DOOR, porte, rotRect.center, tmp.size()));
                    color = BLUE;
                } else if (WindowForm.getInstance().isRecognized(rotRect, src)) {
                    fenetre++;
                    MainActivity.obstacle.add(speaker.getLocation(Speaker.WINDOW, fenetre, rotRect.center, tmp.size()));
                    color = BLUE;
                } else if (BlockForm.getInstance().isRecognized(rotRect, src)) {
                    MainActivity.obstacle.add(speaker.getLocation(Speaker.WINDOW, 1, rotRect.center, src.size()));
                    color = BLUE;
                } else {
                    Log.d("constraint", "NOT RECOGNIZED");
                }

//                Imgproc.rectangle(tmp, rect.br(), rect.tl(), (color == null ? new Scalar(255, 0, 0) : color), 3);
                for (int j = 0; j < 4; j++) {
                    Imgproc.line(tmp, vertices[j], vertices[(j + 1) % 4], (color == null ? new Scalar(0, 255, 0) : color));
                }
            } else {
                Log.d("size", "Too big element");
            }
        }
        return tmp;
    }

    /**
     * Identification des obstacles sur la partie inférieur de l'image
     *
     * @param src
     * @return
     */
//    public static Mat stuffFinder(Mat src) {
//
//        Mat tmp = new Mat();
//        Mat resizedTmp = Vista.getSubMat(src.clone(), new Point(Vista.getVista().x + Vista.getVista().width + (src.width() / 5), 0));
//
//        if (resizedTmp.isSubmatrix() && resizedTmp.width() > 0 && resizedTmp.height() > 0) {
//            // pré traitement
//            Mat resized = resizedTmp.clone();
//            Imgproc.resize(resizedTmp, resized, new Size(resizedTmp.size().width / 4.0f, resizedTmp.size().height / 4.0f));
//            Imgproc.dilate(resized, resized, new Mat(), new Point(), 2);
//
//            // Traitement
//            int k = 3;
//            List<Mat> clusters = Kmeans.cluster(resized, k);
//
//            // identification de la frame avec le plus de couleur elle sera le sol
//            float maxNonZeroPercent = 0f;
//            int maxNonZeroIndex = -1;
//
//            for (int i = 0; i < k; i++) {
//                Mat frame = clusters.get(i);
//
//                Imgproc.cvtColor(frame, tmp, Imgproc.COLOR_RGB2GRAY);
//
//                long count = Core.countNonZero(tmp);
//                float res = count / (float) tmp.total();
//
//                if (res > maxNonZeroPercent) {
//                    maxNonZeroPercent = res;
//                    maxNonZeroIndex = i;
//                }
//            }
//
//            // fonctionne draw contour
//            resizedTmp = clusters.get(maxNonZeroIndex).clone();
//
//        }
//        return resizedTmp;
//    }

    /**
     * @param rotatedRect
     * @param mat
     * @return true if object isn't too big
     */
    public static boolean isNotTooBig(@NonNull RotatedRect rotatedRect, @NonNull Mat mat) {
        return rotatedRect.size.height * rotatedRect.size.width < mat.height() * mat.width() * 0.7 && rotatedRect.size.width < mat.width() * 0.90 && rotatedRect.size.height < mat.height() * 0.90;
    }

    /**
     * @param rotatedRect
     * @param mat
     * @return true if object isn't too small
     */
    public static boolean isNotTooSmall(@NonNull RotatedRect rotatedRect, @NonNull Mat mat) {
        return rotatedRect.size.height * rotatedRect.size.width > mat.height() * mat.width() * 0.1;
    }


    /**
     * @param listMatOfPoint
     * @param max
     */
    public static void deleteInsideForm(List<MatOfPoint> listMatOfPoint, int max) {
        for (int i = 0; i < max && i < listMatOfPoint.size(); i++) {
            Rect rect1 = Imgproc.boundingRect(listMatOfPoint.get(i));
            int tlxRect1 = (int) rect1.tl().x;
            int tlyRect1 = (int) rect1.tl().y;
            int widthRect1 = tlxRect1 + rect1.width;
            int HeightRect1 = tlyRect1 + rect1.height;

            for (int j = i + 1; j < max && j < listMatOfPoint.size(); j++) {
                Rect rect2 = Imgproc.boundingRect(listMatOfPoint.get(j));
                int tlxRect2 = (int) rect2.tl().x;
                int tlyRect2 = (int) rect2.tl().y;
                int widthRect2 = tlxRect2 + rect2.width;
                int HeightRect2 = tlyRect2 + rect2.height;

                int x_overlap = Math.max(0, Math.min(widthRect1, widthRect2) - Math.max(tlxRect1, tlxRect2));
                int y_overlap = Math.max(0, Math.min(HeightRect1, HeightRect2) - Math.max(tlyRect1, tlyRect2));

                if ((x_overlap * y_overlap) >= (rect2.area() * 0.80)) {
                    listMatOfPoint.remove(listMatOfPoint.get(j));
                }
            }
        }
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
