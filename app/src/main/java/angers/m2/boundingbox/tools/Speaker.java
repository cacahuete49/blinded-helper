package angers.m2.boundingbox.tools;

import org.opencv.core.Mat;
import org.opencv.core.Point;

/**
 * Created by cacahuete on 22/05/16.
 */
public class Speaker {

    /**
     * Split la mat en 9 pour localiser un point
      * @param p
     * @param src
     * @return
     */
    public static String locate(Point p , Mat src) {
        StringBuilder position = new StringBuilder();
        if (p.x / (float) src.width() < 0.33f)
            position.append("haut ");
        else if (p.x / (float) src.width() > 0.66f)
            position.append("bas ");
        else
            position.append("milieu ");


        if (p.y / (float) src.height() < 0.33f)
            position.append("à droite ");
        else
        if (p.y / (float) src.height() > 0.66f)
            position.append("à gauche ");
        else
            position.append("centre ");

        return position.toString();
    }

}
