package imageProcessing;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.core.Point;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import tools.DebugTools;

/**
 * Created by Jason on 15/12/2014.
 */
public class SuperpixelImage {
    Mat sobel;
    private MatOfPoint2f centersOfSuperpixels = null;
    private MatOfDouble valueOfEachSuperpixel = null;
    private MatOfDouble contrastValueOfEachSuperpixel = null;
    private MatOfInt listOfSuperpixelsID = null;
    private MatOfInt pixelCountForEachSuperpixel = null;
    private Mat superpixelsID = null;

    public SuperpixelImage(){
        sobel = new Mat(3, 3, CvType.CV_32FC1);
        sobel.put(0, 0, -1/16.);
        sobel.put(0, 1, -2/16.);
        sobel.put(0, 2, -1/16.);
        sobel.put(1, 0, 0);
        sobel.put(1, 1, 0);
        sobel.put(1, 2, 0);
        sobel.put(2, 0, 1/16.);
        sobel.put(2, 1, 2/16.);
        sobel.put(2, 2, 1/16.);
    }

    public SuperpixelImage(MatOfPoint2f centersOfSuperpixels, MatOfInt listOfSuperpixelsID, Mat superpixelsID, Mat sobel){
        this.centersOfSuperpixels = centersOfSuperpixels;
        this.listOfSuperpixelsID = listOfSuperpixelsID;
        this.superpixelsID = superpixelsID;
        this.sobel = sobel;
    }

    public MatOfPoint2f getCentersOfSuperpixels() {
        return centersOfSuperpixels;
    }

    public void setCentersOfSuperpixels(MatOfPoint2f centersOfSuperpixels) {
        this.centersOfSuperpixels = centersOfSuperpixels;
    }

    public MatOfDouble getValueOfEachSuperpixel() {
        return valueOfEachSuperpixel;
    }

    public void setValueOfEachSuperpixel(MatOfDouble valueOfEachSuperpixel) {
        this.valueOfEachSuperpixel = valueOfEachSuperpixel;
    }

    public MatOfInt getListOfSuperpixelsID() {
        return listOfSuperpixelsID;
    }

    public void setListOfSuperpixelsID(MatOfInt listOfSuperpixelsID) {
        this.listOfSuperpixelsID = listOfSuperpixelsID;
    }

    public Mat getSuperpixelsID() {
        return superpixelsID;
    }

    public void setSuperpixelsID(Mat superpixelsID) {
        this.superpixelsID = superpixelsID;
    }

    public SuperpixelImage calculatePixelCountForEachSuperpixel(){
        List<Integer> superpixelIDList = listOfSuperpixelsID.toList();
        List<Integer> totalPixelsList = new ArrayList<Integer>();

        for(int i = 0; i < superpixelIDList.size(); i++) {
            // Calculate the number of pixels in this superpixel
            int id = superpixelIDList.get(i);
            Mat booleanMat = new Mat();
            Core.compare(superpixelsID, new Scalar(id), booleanMat, Core.CMP_EQ);
            Core.divide(booleanMat, new Scalar(255), booleanMat);
            int totalPixels = Core.countNonZero(booleanMat);
            totalPixelsList.add(totalPixels);
        }

        this.pixelCountForEachSuperpixel = new MatOfInt();
        this.pixelCountForEachSuperpixel.fromList(totalPixelsList);
        return this;
    }

    public SuperpixelImage calculateValue(Mat image_input){
        List<Integer> superpixelIDList = listOfSuperpixelsID.toList();
        List<Double> valueList = new ArrayList<Double>();
        List<Integer> totalPixelsList = new ArrayList<Integer>();

        // For each superpixel...
        for(int i = 0; i < superpixelIDList.size(); i++){
            int id = superpixelIDList.get(i);

            // Use the id to filter the mat to get 1,0 mat
            // 1 = pixel of this superpixel
            Mat booleanMat = new Mat();
            Core.compare(superpixelsID,new Scalar(id),booleanMat,Core.CMP_EQ);
            Core.divide(booleanMat,new Scalar(255),booleanMat);

            // Multiply to get the input image and the 1,0 mat to filter out the value
            Mat valuesMat = new Mat();
            Core.multiply(booleanMat,image_input,valuesMat);

            // Calculate the average value for each superpixel
            double totalValue = Core.sumElems(valuesMat).val[0];
            int totalPixels = Core.countNonZero(booleanMat);
            valueList.add(totalValue / totalPixels);
            totalPixelsList.add(totalPixels);
        }

        this.pixelCountForEachSuperpixel = new MatOfInt();
        this.pixelCountForEachSuperpixel.fromList(totalPixelsList);
        this.valueOfEachSuperpixel = new MatOfDouble();
        this.valueOfEachSuperpixel.fromList(valueList);
        Core.normalize(valueOfEachSuperpixel,valueOfEachSuperpixel,0,255,Core.NORM_MINMAX);
        return this;
    }

    public Mat getValueMat(){
        List<Integer> superpixelIDList = listOfSuperpixelsID.toList();
        List<Double> superpixelValueList = valueOfEachSuperpixel.toList();
        Mat result_mat = Mat.zeros(superpixelsID.size(),CvType.CV_8UC1);

        // For each superpixel...
        for(int i = 0; i < superpixelIDList.size(); i++){
            int id = superpixelIDList.get(i);

            // Use the id to filter the mat to get 1,0 mat
            // 1 = pixel of this superpixel
            Mat booleanMat = new Mat();
            Core.compare(superpixelsID,new Scalar(id),booleanMat,Core.CMP_EQ);
            Core.divide(booleanMat,new Scalar(255),booleanMat);

            // Multiply to convert all 1 to the value of that superpixel
            Mat valuesMat = new Mat();
            Core.multiply(booleanMat, new Scalar(superpixelValueList.get(i)), valuesMat);

            //Put this value matrix to the result matrix
            Core.add(valuesMat, result_mat, result_mat);
        }

        return result_mat;
    }

    public Bitmap getValueBitmap(){
        Mat result_mat = this.getValueMat();

        // Change the value matrix to Bitmap
        Bitmap result_bitmap = Bitmap.createBitmap(result_mat.width(), result_mat.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(result_mat,result_bitmap);
        return result_bitmap;
    }

    public SuperpixelImage calculateContrastValueMat(Mat input){
        if(this.valueOfEachSuperpixel == null)
            this.calculateValue(input);
        if(this.pixelCountForEachSuperpixel == null)
            this.calculatePixelCountForEachSuperpixel();

//        Mat valule_mat = this.getValueMat();
        List<Integer> superpixelIDList = listOfSuperpixelsID.toList();
        List<Point> superpixelCenterList = centersOfSuperpixels.toList();
        List<Double> superpixelValueList = valueOfEachSuperpixel.toList();
        List<Integer> pixelCountList = pixelCountForEachSuperpixel.toList();
        List<Double> contrastValueList = new ArrayList<Double>();
        Mat result_mat = Mat.zeros(superpixelsID.size(),CvType.CV_8UC1);

        // For each superpixel...
        for(int i = 0; i < superpixelIDList.size(); i++){
            double contrastValue = 0;
            double alpha = 30;
            //For each other superpixel...
            for(int j = 0; j < superpixelIDList.size(); j++) {
                if (i != j) {
                    int totalPixels = pixelCountList.get(j);

                    // Calculate the distance factor
                    Point point1 = superpixelCenterList.get(i);
                    Point point2 = superpixelCenterList.get(j);
                    double dist_x_pow = Math.pow(Math.abs(point1.x - point2.x), 2);
                    double dist_y_pow = Math.pow(Math.abs(point1.y - point2.y), 2);
                    double distance = Math.sqrt(dist_x_pow + dist_y_pow);
                    double distance_factor = Math.exp(- distance / (2.0*alpha*alpha));
//                    Log.d("SLIC distance ","distance = "+distance);
//                    Log.d("SLIC distance_factor ","distance_factor = "+distance_factor);

                    // Calculate the difference
                    double value_difference = Math.abs(superpixelValueList.get(i) - superpixelValueList.get(j));

                    // Add the contrast value to the list
                    contrastValue += totalPixels * distance_factor * value_difference;
                }
            }
            contrastValueList.add(contrastValue);
        }
        Log.d("SLIC contrastValueList ","contrastValueList = "+contrastValueList);
        this.contrastValueOfEachSuperpixel = new MatOfDouble();
        this.contrastValueOfEachSuperpixel.fromList(contrastValueList);
        DebugTools.saveMatToString("SLIC_contrastValueOfEachSuperpixel_before_normalize",contrastValueOfEachSuperpixel);
        Core.normalize(contrastValueOfEachSuperpixel,contrastValueOfEachSuperpixel,0,255,Core.NORM_MINMAX);
//        Core.multiply(contrastValueOfEachSuperpixel,new Scalar(255), contrastValueOfEachSuperpixel);
        DebugTools.saveMatToString("SLIC_contrastValueOfEachSuperpixel_after_normalize",contrastValueOfEachSuperpixel);
        return this;
    }

    public Mat getContrastValueMat(){
        List<Integer> superpixelIDList = listOfSuperpixelsID.toList();
        List<Double> superpixelContrastValueList = contrastValueOfEachSuperpixel.toList();
        Mat result_mat = Mat.zeros(superpixelsID.size(),CvType.CV_8UC1);

        // For each superpixel...
        for(int i = 0; i < superpixelIDList.size(); i++){
            int id = superpixelIDList.get(i);

            // Use the id to filter the mat to get 1,0 mat
            // 1 = pixel of this superpixel
            Mat booleanMat = new Mat();
            Core.compare(superpixelsID,new Scalar(id),booleanMat,Core.CMP_EQ);
            Core.divide(booleanMat,new Scalar(255),booleanMat);

            // Multiply to convert all 1 to the value of that superpixel
            Mat valuesMat = new Mat();
            Core.multiply(booleanMat, new Scalar(superpixelContrastValueList.get(i)), valuesMat);

            //Put this value matrix to the result matrix
            Core.add(valuesMat, result_mat, result_mat);
        }

        return result_mat;
    }

    public Bitmap getContrastValueBitmap(){
        Mat result_mat = this.getContrastValueMat();

        // Change the value matrix to Bitmap
        Bitmap result_bitmap = Bitmap.createBitmap(result_mat.width(), result_mat.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(result_mat,result_bitmap);
        return result_bitmap;
    }

    public Bitmap createBoundary(Bitmap input){
        // For error checking
        if(superpixelsID.size().width!=input.getWidth() || superpixelsID.size().height!=input.getHeight())
            return null;

        Mat im = new Mat(input.getWidth(),input.getHeight(),CvType.CV_8U);
        Utils.bitmapToMat(input,im);

        superpixelsID.convertTo(superpixelsID, CvType.CV_32F);
        Mat gx = new Mat();
        Mat gy = new Mat();
        Mat grad = new Mat();
        Imgproc.filter2D(superpixelsID, gx, -1, sobel);
        Imgproc.filter2D(superpixelsID, gy, -1, sobel.t());
        Core.magnitude(gx, gy, grad);

        Core.compare(grad,new Scalar(0.0001),grad,Core.CMP_GT);
        Core.divide(grad,new Scalar(255),grad);
        Mat show = new Mat();
        Core.subtract(Mat.ones(grad.size(),grad.type()),grad,show);
        show.convertTo(show, CvType.CV_8U);

        Vector<Mat> rgb = new Vector<Mat>(3);

        Utils.bitmapToMat(input, im);

        im.convertTo(im,CvType.CV_8UC3);

        Core.split(im, rgb);
        for (int i = 0; i < 3; i++)
            rgb.set(i,rgb.get(i).mul(show));

        Mat outputMat = new Mat();
        Core.merge(rgb, outputMat);

        Bitmap output = Bitmap.createBitmap(outputMat.width() , outputMat.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(outputMat, output);
        return output;
    }
}