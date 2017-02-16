package com.example.mitsuo.sobelfilter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by mitsuo on 2017/02/11.
 */

public class SobelTask extends AsyncTask<Bitmap, Bundle, Bitmap> {

    private ImageView imageView;
    private ProgressDialog progressDialog;
    private Activity uiActivity;

    public SobelTask(Activity ac, ImageView iv) {
        super();
        uiActivity = ac;
        imageView = iv;
    }

    @Override
    protected void onCancelled(Bitmap result) {
        progressDialog.dismiss();
    }

    @Override
    protected void onPreExecute() {
        progressDialog = new ProgressDialog(uiActivity);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMessage("processing...");
        progressDialog.setIndeterminate(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    @Override
    protected Bitmap doInBackground(Bitmap... bitmaps) {

        Bitmap bmpout = bitmaps[0].copy(Bitmap.Config.ARGB_8888, true);

        return sobel(bmpout);
    }

    @Override
    protected void onProgressUpdate(Bundle... values) {
        super.onProgressUpdate(values);
        Bundle bundle = values[0];
        if (bundle.containsKey("Message")) {
            progressDialog.setMessage(bundle.getString("Message"));
        }
        if (bundle.containsKey("Max")) {
            progressDialog.setMax(bundle.getInt("Max"));
        }
        if (bundle.containsKey("Progress")) {
            progressDialog.setProgress(bundle.getInt("Progress"));
        }
    }

    /**
     * バックグランド処理が完了し、UIスレッドに反映する
     */
    @Override
    protected void onPostExecute(Bitmap result) {
        progressDialog.dismiss();
        if (result != null) {
            imageView.setImageBitmap(result);
        } else {
            imageView = (ImageView) uiActivity.findViewById(R.id.imageView);
        }
    }

    public void setProgressMessage(String message){
        Bundle data = new Bundle();
        data.putString("Message", message);
        publishProgress(data);
    }
    /**
     * setMax
     * プログレスダイアログの最大値を設定します。
     * @param max
     */
    public void setProgressMax(int max){
        Bundle data = new Bundle();
        data.putInt("Max", max);
        publishProgress(data);
    }
    /**
     * setProgress
     * プログレスダイアログの進捗値を設定します。
     * @param progress
     */
    public void setProgress(int progress){
        Bundle data = new Bundle();
        data.putInt("Progress", progress);
        publishProgress(data);
    }


    private Bitmap sobel(Bitmap bitmap) {
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Mat mat = new Mat();
        Mat matR = new Mat();
        Mat matG = new Mat();
        Mat matB = new Mat();
        Utils.bitmapToMat(bitmap, mat);

        Core.extractChannel(mat, matR, 0);
        myFiltering(matR);

        Core.extractChannel(mat, matG, 1);
        myFiltering(matG);

        Core.extractChannel(mat, matB, 2);
        myFiltering(matB);

        matMax3(matB, matG, matR, matB);

        Utils.matToBitmap(matB, bitmap);
        return bitmap;
    }

    private void myFiltering(Mat src){
        Mat matX = new Mat();
        Mat matY = new Mat();

        Imgproc.GaussianBlur(src, src, new Size(3, 3), 0, 0);
        Imgproc.Sobel(src, matX, src.depth(), 0, 1);
        Imgproc.Sobel(src, matY, src.depth(), 1, 0);
        matRMS(matX, matY, src);
    }

    private void matRMS(Mat src1, Mat src2, Mat dst) {

        int size = (int) (src1.total() * src1.channels());
        byte[] temp1 = new byte[size];
        byte[] temp2 = new byte[size];
        byte[] temp3 = new byte[size];
        src1.get(0, 0, temp1);
        src2.get(0, 0, temp2);

        for (int i = 0; i < size; i++) {
            temp3[i] = (byte)Math.sqrt((temp1[i] * temp1[i] + temp2[i] * temp2[i]) / 2);
        }

        dst.put(0, 0, temp3);
    }

    private void matMax3(Mat src1, Mat src2, Mat src3, Mat dst) {

        int size = (int) (src1.total() * src1.channels());
        byte[] temp1 = new byte[size];
        byte[] temp2 = new byte[size];
        byte[] temp3 = new byte[size];
        byte[] temp4 = new byte[size];
        src1.get(0, 0, temp1);
        src2.get(0, 0, temp2);
        src3.get(0, 0, temp3);

        for (int i = 0; i < size; i++) {
            temp4[i] = chooseBig(chooseBig(temp1[i], temp2[i]), temp3[i]);
        }

        dst.put(0, 0, temp4);
    }

    private byte chooseBig(byte a, byte b) {
        if(b > a) {
            return b;
        }else {
            return a;
        }
    }
}

