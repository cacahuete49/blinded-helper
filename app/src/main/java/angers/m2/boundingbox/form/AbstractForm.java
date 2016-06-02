package angers.m2.boundingbox.form;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.RotatedRect;

import java.util.LinkedHashSet;
import java.util.Set;

import angers.m2.boundingbox.form.constraint.IConstraint;

/**
 * Created by cacahuete on 22/05/16.
 */
public class AbstractForm implements IForm {

    protected Set<IConstraint> constraints = new LinkedHashSet<>();
    protected float seuil = 1.0f;

    @Override
    public boolean isRecognized(RotatedRect rect, Mat src) {
        if (constraints.size() == 0) return false;
        int resultat = 0;
        for (IConstraint constraint : this.constraints)
            if (constraint.assertConstraint(rect, src))
                resultat++;
        Log.d("abstractForm",this.getClass().getSimpleName()+" valid="+(resultat / (float) constraints.size())+"%");
        return (resultat / (float) constraints.size()) >= seuil;
    }
}
