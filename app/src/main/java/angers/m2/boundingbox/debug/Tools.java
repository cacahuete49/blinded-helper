package angers.m2.boundingbox.debug;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Tools {

    public static void writeBitMap(Mat mat, String fileName) {
        File bmpFile = new File(Environment.getExternalStorageDirectory() + File.separator + "OpenCV" + File.separator + fileName);
        if (!bmpFile.getParentFile().exists())
            bmpFile.getParentFile().mkdir();

        Bitmap bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bmp);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(bmpFile);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
