package angers.m2.boundingbox.Tools;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.HashMap;

/**
 * Created by cacahuete on 22/05/16.
 */
public class Speaker {

    private static Speaker INSTANCE = null;

    private final static String PORTE = "porte";
    private final static String FENETRE = "fenetre";
    private final static String OBSTACLE = "obstacle";
    private final static String GAUCHE = "à gauche";
    private final static String DROITE =  "à droite";
    private final static String DEVANT = "devant";
    private final static String CENTRE = "au centre";
    private final static String DESSUS = "au dessus";
    private final static String DESSOUS = "en dessous";
    private final static String HAUT = "en haut";
    private final static String Bas = "en bas";
    private final static String ATTENTION = "Attention, ";
    private final static String ILYA = "il y a ";
    private final static String UNE = "une";
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
    public final static int UP = 107;
    public final static int DOWN = 108;

    private HashMap<Integer, String> map = new HashMap<>();

    private Speaker() {
        map.put(DOOR, PORTE);
        map.put(WINDOW, FENETRE);
        map.put(BLOCK, OBSTACLE);

        map.put(LEFT, GAUCHE);
        map.put(RIGHT, DROITE);
        map.put(FRONT, DEVANT);
        map.put(CENTER, CENTRE);
        map.put(ABOVE, DESSUS);
        map.put(BELOW, DESSOUS);
        map.put(UP, HAUT);
        map.put(DOWN, Bas);
    }

    public static synchronized Speaker getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Speaker();
        }
        return INSTANCE;
    }

    public String getText(int object, int number, int[] direction) {
        getInstance();

        StringBuilder text = new StringBuilder();

        if (object == BLOCK) {
            text.append(ATTENTION);
        }
        text.append(ILYA);

        if ((number == 1) && (object == DOOR || object == WINDOW)) {
            text.append(UNE + ESPACE).append(map.get(object));
        } else {
            text.append(number).append(ESPACE).append(map.get(object));
        }

        for (int d : direction)  {
            text.append(ESPACE).append(map.get(d));
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
