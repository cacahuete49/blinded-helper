package angers.m2.boundingbox.tools;

import android.app.Activity;
import android.util.Size;

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

    /**
     * Classe de gestion du Speaker.
     *
     * @author Quentin Rabineau / Mattieu Racine
     * @version 1.0
     */

    /**
     * Constructeur de la classe Speaker
     *
     * @param activity Activité Android à laquelle est rattaché la librairie.
     */
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

    /**
     * Initialise la classe Speaker en instanciant un singleton.
     *
     * @param activity Activité Android à laquelle est rattaché la librairie.
     */
    public static synchronized Speaker getInstance(Activity activity) {
        if (INSTANCE == null) {
            INSTANCE = new Speaker(activity);
        }
        return INSTANCE;
    }


    /**
     * Construit la phrase correspondant à l'objet et à sa position dans la scène.
     *
     * @param object La nature de l'objet dont on détermine la position.
     * @param number Le nombre d'objet dont on détermine la position.
     * @param direction Les directions de l'objet.
     *
     * @return La phrase contenant le nom de l'objet, la position et son nombre.
     */
    private String getText(int object, int number, int[] direction) {
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
     * @param size
     * @return
     */
    public String getLocation(int object, int number, Point p , org.opencv.core.Size size) {
        int [] direction = new int[2];

        if (p.x / (float) size.width < 0.33f) {
            direction[0] = Speaker.TOP;
        } else if (p.x / (float) size.width > 0.66f) {
            direction[0] = Speaker.BOTTOM;
        } else {
            direction[0] = Speaker.CENTER;
        }

        if (p.y / (float) size.width < 0.33f)
            direction[1] = Speaker.RIGHT;
        else if (p.y / (float) size.width > 0.66f)
            direction[1] = Speaker.LEFT;
        else
            direction[1] = Speaker.FRONT;

        return getText(object, number, direction);
    }

}
