package angers.m2.boundingbox.tools;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;

import java.util.Comparator;

/**
 * Created by cacahuete on 30/05/16.
 */
public class MatComparator implements Comparator<Mat> {
    @Override
    public int compare(Mat lhs, Mat rhs) {
        double area1 = Imgproc.contourArea(lhs);
        double area2 = Imgproc.contourArea(rhs);
        if (area1 < area2)
            return 1;
        else if (area1 > area2)
            return -1;
        return 0;
    }
}
