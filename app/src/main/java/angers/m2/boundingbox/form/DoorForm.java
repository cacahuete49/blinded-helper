package angers.m2.boundingbox.form;

import android.util.Log;

import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;

import angers.m2.boundingbox.form.constraint.IConstraint;
import angers.m2.boundingbox.tools.Kmeans;
import angers.m2.boundingbox.tools.Vista;

/**
 * Created by cacahuete on 22/05/16.
 */
public class DoorForm extends AbstractForm implements IForm {

    private static class SingletonHolder {
        private static final DoorForm INSTANCE = new DoorForm();
    }

    public static DoorForm getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private DoorForm() {
        seuil = 0.7f;

        /**
         * Contrainte de proportion
         */
        constraints.add(new IConstraint() {
            @Override
            public boolean assertConstraint(RotatedRect rect, Mat src) {
                float aspectRatio = (float) rect.size.height / (float) rect.size.width;
                float tolerance = 0.7f;
                return (aspectRatio >= 0.37 * tolerance && aspectRatio <= 0.45 && Math.abs(rect.angle) < 45);
            }
        });

        /**
         * Filtre des formes trop proches des bords
         */
        constraints.add(new IConstraint() {
            @Override
            public boolean assertConstraint(RotatedRect rect, Mat src) {
                Point[] points = new Point[4];
                rect.points(points);
                short count = 0;
                float tolerance = (src.width() + src.height()) / (2 * 100);
                for (Point p : points) {
                    if (p.x < tolerance || p.x > src.width() - tolerance || p.y < tolerance || p.y > src.height() - tolerance)
                        count++;
                }
                return count < 2;
            }
        });

        /**
         *  Contrainte de positionnement horizontal et a l'horizon
         */
        constraints.add(new IConstraint() {
            @Override
            public boolean assertConstraint(RotatedRect rect, Mat src) {
                return Vista.getPositionPoint( rect.center) == Vista.INSIDE && (Math.abs(rect.angle) < 15);
            }
        });

        /**
         * Contrainte de couleurs unis à 80% min
         */
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

                Mat tmp;
                try {
                    tmp = src.submat(rectTmp);
                } catch (CvException e) {
                    Log.e("constraint", " rect out of mat -> rectTmp:("+rectTmp.tl()+","+rectTmp.br()+") src:"+src.size());
                    return false;
                }

                return Kmeans.getPercentMaxColor(tmp)>80;
            }
        });

    }
}
