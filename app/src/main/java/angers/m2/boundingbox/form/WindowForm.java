package angers.m2.boundingbox.form;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import angers.m2.boundingbox.debug.Tools;
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
        this.seuil = 0.6f;

        constraints.add(new IConstraint() {
            @Override
            public boolean assertConstraint(RotatedRect rect, Mat src) {
                Rect rectTmp = rect.boundingRect();

                Mat tmp = src.clone();
                Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_RGB2GRAY);
//                Tools.writeBitMap(src, "0_windows");
                Imgproc.threshold(tmp, tmp, 192, 255, Imgproc.THRESH_BINARY);
//                Tools.writeBitMap(tmp, "1_threshold");

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

                int pos = Vista.getPositionPoint(tmp);
                return pos == Vista.INSIDE && (Math.abs(rect.angle) < 15);
            }
        });

        constraints.add(new IConstraint() {
            @Override
            public boolean assertConstraint(RotatedRect rect, Mat src) {
                Rect rectTmp = rect.boundingRect();

                if (rectTmp.x < 0)
                    rectTmp.x = 0;
                if (rectTmp.y < 0)
                    rectTmp.y = 0;
                if (rectTmp.x+rectTmp.width > src.width() - 1)
                    rectTmp.width = src.width() - 1-rectTmp.x;
                if (rectTmp.y+rectTmp.height> src.height() - 1)
                    rectTmp.height = src.height() - 1 - rectTmp.y;

                Mat tmp = null;
                try {
                    tmp = src.submat(rectTmp);
                } catch (CvException e) {
                    Log.e("constraint", " rect out of mat -> rectTmp:("+rectTmp.tl()+","+rectTmp.br()+") src:"+src.size());
                    return false;
                }
                Imgproc.resize(tmp, tmp, new Size(tmp.width() / 10, tmp.height() / 10));
                return Kmeans.getPercentMaxColor(tmp, tmp.size(), 2) < 80;

            }
        });
    }

}
