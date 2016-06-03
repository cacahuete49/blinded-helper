package angers.m2.boundingbox.form;

import org.opencv.core.Mat;
import org.opencv.core.RotatedRect;

import angers.m2.boundingbox.form.constraint.IConstraint;
import angers.m2.boundingbox.tools.Vista;

/**
 * Created by cacahuete on 03/06/16.
 */
public class BlockForm extends AbstractForm implements IForm {

    private static class SingletonHolder {
        private static final BlockForm INSTANCE = new BlockForm();
    }

    public static BlockForm getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private BlockForm() {
        seuil = 1.0f;

        /**
         * Contrainte de proportion
         */
        constraints.add(new IConstraint() {
            @Override
            public boolean assertConstraint(RotatedRect rect, Mat src) {
                return Vista.getPositionPoint(rect.center) == Vista.EXTERN_DOWN;
            }
        });
    }
}
