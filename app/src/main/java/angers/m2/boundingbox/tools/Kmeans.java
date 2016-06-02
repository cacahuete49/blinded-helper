package angers.m2.boundingbox.tools;

import org.opencv.BuildConfig;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import angers.m2.boundingbox.debug.Tools;

/**
 * Created by cacahuete on 24/05/16.
 */
public class Kmeans {

    public static float getPercentMaxColor(Mat mat) {
        return getPercentMaxColor(mat, mat.size(), 3);
    }

    public static float getPercentMaxColor(Mat mat, Size size, int k) {
        // Resize
        Mat matResize = new Mat();
        Imgproc.resize(mat, matResize, size);
        Tools.writeBitMap(matResize, "0_cluster");
        // Kmeans
        // Traitement
        Imgproc.cvtColor(matResize, matResize, Imgproc.COLOR_RGBA2RGB, 3);
        Imgproc.dilate(matResize, matResize, new Mat());

        // Calcul du kmeans
        List<Mat> clusters = cluster(matResize, k);

        float maxPercent = 0;
        for (int i = 0; i < k; i++) {
            Mat frame = new Mat();
            Tools.writeBitMap(clusters.get(i), (i + 1) + "_cluster");
            Imgproc.cvtColor(clusters.get(i), frame, Imgproc.COLOR_RGB2GRAY);

            float countZeroFrame = Core.countNonZero(frame);
            float calculPercent = countZeroFrame / frame.total() * 100;
            if (calculPercent > maxPercent) {
                maxPercent = calculPercent;
            }
        }
        return maxPercent;
    }

    /**
     * https://github.com/badlogic/opencv-fun/blob/master/src/pool/tests/Cluster.java
     */
    public static List<Mat> cluster(Mat cutout, int k) {
        Mat samples = cutout.reshape(1, cutout.cols() * cutout.rows());
        Mat samples32f = new Mat();
        samples.convertTo(samples32f, CvType.CV_32F, 1.0 / 255.0);

        Mat labels = new Mat();
        TermCriteria criteria = new TermCriteria(TermCriteria.COUNT, 100, 1);
        Mat centers = new Mat();
        Core.kmeans(samples32f, k, labels, criteria, 1, Core.KMEANS_PP_CENTERS, centers);
        return showClusters(cutout, labels, centers);
    }

    /**
     * https://github.com/badlogic/opencv-fun/blob/master/src/pool/tests/Cluster.java
     */
    private static List<Mat> showClusters(Mat cutout, Mat labels, Mat centers) {
        centers.convertTo(centers, CvType.CV_8UC1, 255.0);
        centers.reshape(3);

        List<Mat> clusters = new ArrayList<Mat>();
        for (int i = 0; i < centers.rows(); i++) {
            clusters.add(Mat.zeros(cutout.size(), cutout.type()));
        }

        Map<Integer, Integer> counts = new HashMap<>();
        for (int i = 0; i < centers.rows(); i++)
            counts.put(i, 0);

        int rows = 0;
        for (int y = 0; y < cutout.rows(); y++) {
            for (int x = 0; x < cutout.cols(); x++) {
                int label = (int) labels.get(rows, 0)[0];
                int r = (int) centers.get(label, 2)[0];
                int g = (int) centers.get(label, 1)[0];
                int b = (int) centers.get(label, 0)[0];
                counts.put(label, counts.get(label) + 1);
                // BGR OR BGRA
                if (cutout.channels() == 3) {
                    clusters.get(label).put(y, x, b, g, r);
                } else {
                    int a = (int) centers.get(label, 3)[0];
                    clusters.get(label).put(y, x, b, g, r, a);
                }

                rows++;
            }
        }
        return clusters;
    }
}
