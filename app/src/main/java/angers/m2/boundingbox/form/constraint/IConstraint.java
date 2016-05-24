package angers.m2.boundingbox.form.constraint;

import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.RotatedRect;

/**
 * Created by cacahuete on 22/05/16.
 */
public interface IConstraint {

    public boolean assertConstraint(RotatedRect rect, Mat src);
}
