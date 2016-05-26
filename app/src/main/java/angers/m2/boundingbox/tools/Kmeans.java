package angers.m2.boundingbox.tools;

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

/**
 * Created by cacahuete on 24/05/16.
 */
public class Kmeans {

    public static float getPercentMaxColor(Mat mat) {
        return getPercentMaxColor(mat,mat.size(),3);
    }

    public static float getPercentMaxColor(Mat mat, Size size, int k) {
        // Resize
        Mat matResize = new Mat();
        Imgproc.resize(mat, matResize, size);

        // Kmeans
        // Traitement
        Imgproc.cvtColor(matResize, matResize, Imgproc.COLOR_RGB2HSV, 3);
        Imgproc.dilate(matResize, matResize, new Mat());

        // Calcul du kmeans
        Mat matResize32f = new Mat();
        Mat matResize32fTmp = matResize.reshape(1, matResize.rows() * matResize.cols());
        Mat labels = new Mat();
        Mat centers = new Mat();

        matResize32fTmp.convertTo(matResize32f, CvType.CV_32F, 1.0 / 255.0);
        Core.kmeans(matResize32f, k, labels, new TermCriteria(TermCriteria.COUNT, 100, 1), 1, Core.KMEANS_PP_CENTERS, centers);
        centers.convertTo(centers, CvType.CV_8UC1, 255.0);
        centers.reshape(3);

        List<Mat> clusters = new ArrayList<>();
        for (int i = 0; i < centers.rows(); i++) {
            clusters.add(Mat.zeros(matResize.size(), matResize.type()));
        }

        Map<Integer, Integer> counts = new HashMap<>();
        for (int i = 0; i < centers.rows(); i++)
            counts.put(i, 0);

        int rows = 0;
        for (int y = 0; y < matResize.rows(); y++) {
            for (int x = 0; x < matResize.cols(); x++) {
                int label = (int) labels.get(rows++, 0)[0];
                counts.put(label, counts.get(label + 1));
                clusters.get(label).put(y, x, (int) centers.get(label, 0)[0], (int) centers.get(label, 1)[0], (int) centers.get(label, 2)[0]);
            }
        }

        float maxPercent = 0;
        for (int i = 0; i < k; i++) {
            Mat frame = new Mat();
            Imgproc.cvtColor(clusters.get(i), frame, Imgproc.COLOR_RGB2GRAY);

            float countZeroFrame = Core.countNonZero(frame);
            float calculPercent = countZeroFrame / frame.total() * 100;
            if (calculPercent > maxPercent) {
                maxPercent = calculPercent;
            }
        }
        return maxPercent;
    }
}
