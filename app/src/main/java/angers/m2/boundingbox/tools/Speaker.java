package angers.m2.boundingbox.tools;

import android.app.Activity;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.HashMap;

import angers.m2.boundingbox.R;

public class Speaker {

    private static Speaker INSTANCE = null;

    private final static String ESPACE = " ";

    public final static int DOOR = 0;
    public final static int WINDOW = 1;
    public final static int BLOCK = 2;

    public final static int LEFT = 100;
    public final static int RIGHT = 101;
    public final static int FRONT = 102;
    public final static int CENTER = 104;
    public final static int ABOVE = 105;
    public final static int BELOW = 106;
    public final static int TOP = 107;
    public final static int BOTTOM = 108;

    private final static int WARNING = 200;
    private final static int THEREARE = 201;
    private final static int A = 202;

    private HashMap<Integer, String> map = new HashMap<>();

    private Speaker(Activity activity) {
        map.put(DOOR, activity.getString(R.string.door));
        map.put(WINDOW, activity.getString(R.string.window));
        map.put(BLOCK, activity.getString(R.string.block));

        map.put(LEFT, activity.getString(R.string.left));
        map.put(RIGHT, activity.getString(R.string.right));
        map.put(FRONT, activity.getString(R.string.front));
        map.put(CENTER, activity.getString(R.string.center));
        map.put(ABOVE, activity.getString(R.string.above));
        map.put(BELOW, activity.getString(R.string.below));
        map.put(TOP, activity.getString(R.string.top));
        map.put(BOTTOM, activity.getString(R.string.bottom));

        map.put(WARNING, activity.getString(R.string.warning));
        map.put(THEREARE, activity.getString(R.string.there_are));
        map.put(A, activity.getString(R.string.a));
    }

    public static synchronized Speaker getInstance(Activity activity) {
        if (INSTANCE == null) {
            INSTANCE = new Speaker(activity);
        }
        return INSTANCE;
    }

    public String getText(int object, int number, int[] direction) {
        StringBuilder text = new StringBuilder();

        if (object == BLOCK) {
            text.append(map.get(WARNING));
        }
        text.append(map.get(THEREARE));

        if ((number == 1) && (object == DOOR || object == WINDOW)) {
            text.append(map.get(A) + ESPACE + map.get(object));
        } else {
            text.append(number + ESPACE + map.get(object));
        }

        for (int d : direction)  {
            text.append(ESPACE + map.get(d));
        }

        return text.toString();
    }

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
