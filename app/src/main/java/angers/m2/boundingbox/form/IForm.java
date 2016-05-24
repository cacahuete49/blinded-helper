package angers.m2.boundingbox.form;

import org.opencv.core.Mat;
import org.opencv.core.RotatedRect;

/**
 * Created by cacahuete on 22/05/16.
 */
public interface IForm {

    /**
     * @return true if a form is recongnized
     */
    public boolean isRecognized(RotatedRect rect, Mat src);

}
