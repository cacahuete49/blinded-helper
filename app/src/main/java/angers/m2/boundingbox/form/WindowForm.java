package angers.m2.boundingbox.form;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import angers.m2.boundingbox.form.constraint.IConstraint;
import angers.m2.boundingbox.tools.Kmeans;
import angers.m2.boundingbox.tools.Vista;

/**
 * Created by cacahuete on 25/05/16.
 */
public class WindowForm extends AbstractForm {

    private static class SingletonHolder {
        private static final WindowForm INSTANCE = new WindowForm();
    }

    public static WindowForm getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public WindowForm() {
        this.seuil = 1.0f;

        constraints.add(new IConstraint() {
            @Override
            public boolean assertConstraint(RotatedRect rect, Mat src) {
                Rect rectTmp = rect.boundingRect();

                Mat tmp = src.clone();
                Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_RGB2GRAY);
                Imgproc.threshold(tmp, tmp, 192, 255, Imgproc.THRESH_BINARY);

                // Imgproc.adaptiveThreshold(tmp,mRgba,255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,Imgproc.THRESH_BINARY_INV, 19, 2);

                if (rectTmp.tl().y > 0 && rectTmp.tl().x > 0 && rectTmp.br().x < src.width() && rectTmp.br().y < src.height()) {
                    double totalAverage = Core.countNonZero(tmp) / (float) tmp.total();
                    tmp = tmp.submat(rectTmp);
                    double localAverage = Core.countNonZero(tmp) / (float) tmp.total();
                    return localAverage > totalAverage;
                }

                return false;
            }
        });

        constraints.add(new IConstraint() {
            @Override
            public boolean assertConstraint(RotatedRect rect, Mat src) {
                Point tmp = rect.center.clone();
                tmp.y = rect.center.y + (rect.size.width / 10.0f);

                int pos = Vista.getPositionPoint(src, tmp);
                return pos == Vista.INSIDE && (Math.abs(rect.angle) < 15);
            }
        });

        constraints.add(new IConstraint() {
            @Override
            public boolean assertConstraint(RotatedRect rect, Mat src) {
                Rect rectTmp = rect.boundingRect();
                Mat tmp = new Mat();
                if (rectTmp.tl().x > 0 && rectTmp.tl().y > 0 && rectTmp.br().y < src.width() && rectTmp.br().x < src.height()) {
                    tmp = src.submat(rectTmp);
                    Imgproc.resize(tmp, tmp, new Size(tmp.width() / 10, tmp.height() / 10));
                    return Kmeans.getPercentMaxColor(tmp, tmp.size(), 2) < 80;
                } else {
                    Log.e("constraint", "Hold the door");
                }
                return false;
            }
        });

    }

}
