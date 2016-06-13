package com.example.jidigamr.trafficlight;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity{

    // Global Variables
    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100; // Image Capture Variable
    private static final int CAMERA_CAPTURE_VIDEO_REQUEST_CODE = 200; // Video Capture Variable
    public static final int MEDIA_TYPE_IMAGE = 1;                     // For Image media we are setting type as 1
    public static final int MEDIA_TYPE_VIDEO = 2;                     // For Video media we are setting type as 2
    TextToSpeech ttsobject;                                            // Text to Speech Object
    int result;                                                        // Result variable to check whether Text to Speech Language initiated is found or not
    private static final String IMAGE_DIRECTORY_NAME = "Hello Camera"; // directory name to store captured images and videos

    private Uri fileUri;                                               // file url to store image/video

    private ImageView imgPreview;
    private VideoView videoPreview;
    private Button btnCapturePicture, btnRecordVideo;                  // Button on the Home Page for Image and Video Capture Purpose
    public String output;                                              // Variable to store the Output Status
    int flag =0;
    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {  // BaseLoaderCallback Method which is the base class for the Activity
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:           // Loader Successfully loaded
                {
                    imgPreview = (ImageView) findViewById(R.id.imgPreview); // If we run Opencv Setup Successfully the Home page will be loaded with two options
                    btnCapturePicture = (Button) findViewById(R.id.btnCapturePicture);  // Getting button Id

                    btnCapturePicture.setOnClickListener(new View.OnClickListener() { // OnClick Functionality for Image Button
                        @Override
                        public void onClick(View v) {
                            captureImage(); // Method Call on Clicking Image Button on Home Screen
                        }
                    });
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {  // On Creation Method of Main Activity.

        super.onCreate(savedInstanceState);
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mOpenCVCallBack)) // Open Cv Version Which i am Using (3_1_0)
        {
            Log.e("TEST", "Cannot connect to OpenCV Manager");
        }
        setContentView(R.layout.activity_main);  // Setting the Content of main activity page as Home Screen

        imgPreview = (ImageView) findViewById(R.id.imgPreview);         // Image Button
        videoPreview = (VideoView) findViewById(R.id.videoPreview);     // Video Button
        btnCapturePicture = (Button) findViewById(R.id.btnCapturePicture); //Setting Image id
        btnRecordVideo = (Button) findViewById(R.id.btnRecordVideo);       // Setting Video id

        ttsobject = new TextToSpeech(MainActivity.this,new TextToSpeech.OnInitListener(){  // Calling Interface of Text to Speech Class

            @Override
            public void onInit(int status) {        // Initialization Method
                if(status==TextToSpeech.SUCCESS){    // If Status is Successfull
                    result=ttsobject.setLanguage(Locale.UK);  // Choosing the Language of our choice
                }else{
                    Toast.makeText(getApplicationContext(), // Status Fails if we mobile used doesn't have required infrastructure
                            "Feature not supported in your device",Toast.LENGTH_SHORT).show();
                }
            }
        });


        /**
         * Capture image button click event
         */
        btnCapturePicture.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // capture picture
                captureImage();         // Method Call on click of Image Button on Home Screen
            }
        });

        /**
         * Record video button click event
         */
        btnRecordVideo.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // record video
                recordVideo();
            }
        });


        // Checking camera availability
        if (!isDeviceSupportCamera()) {
            Toast.makeText(getApplicationContext(),     // Check for whether user device has camera or not
                    "Sorry! Your device doesn't support camera",
                    Toast.LENGTH_LONG).show();
            // will close the app if the device does't have camera
            finish();
        }
    }

    public void tellStatus(View v){         // Text To Speech Method for Checking Status

        switch (v.getId()){                 // Fetching Id
            case R.id.getStatus:            // On Button Click of get status
                if(result==TextToSpeech.LANG_NOT_SUPPORTED || result==TextToSpeech.LANG_MISSING_DATA){ // Condition for if language is supported by mobile device or not
                    Toast.makeText(getApplicationContext(),
                            "Feature not supported in your device",Toast.LENGTH_SHORT).show(); // If not throwing a message saying mobile is not compatible with language chosen
                }else{
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ttsGreater21(output);       // For Accessing Speak Method if API is greater than or equal to 21
                    } else {
                        ttsUnder20(output);         // For Accessing Speak Method if API is less than 20
                    }
                }
                break;

        }

    }

    @SuppressWarnings("deprecation")
    private void ttsUnder20(String text) { // TO access speak Method for API less than 20
        HashMap<String, String> map = new HashMap<>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
        ttsobject.speak(text, TextToSpeech.QUEUE_FLUSH, map);  // Calling Speak Method
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void ttsGreater21(String text) {        // To access speak method for API greater than 21
        String utteranceId=this.hashCode() + "";
        ttsobject.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId); // Calling Speak Method
    }


    private boolean isDeviceSupportCamera() {          // Check if the mobile device using has inbuilt camera or not
        if (getApplicationContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA)) { // this device has a camera
            return true;
        } else { // no camera on this device
            return false;
        }
    }

    private void captureImage() { //Capturing Camera Image will launch camera app requrest image capture
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); // Setting Intent

        fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);  // Storing Image captured

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

        startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);  // start the image capture Intent
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {   //Here we store the file url as it will be null after returning from camera app
        super.onSaveInstanceState(outState);

        outState.putParcelable("file_uri", fileUri); //save file url in bundle as it will be null on screen orientation changes
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        fileUri = savedInstanceState.getParcelable("file_uri"); // Fetching the URL
    }


    private void recordVideo() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        fileUri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO);

        // set video quality
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file
        // name

        // start the video capture Intent
        startActivityForResult(intent, CAMERA_CAPTURE_VIDEO_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) { //Receiving activity result method will be called after closing the camera
        // if the result is capturing Image
        if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // successfully captured the image
                // display it in image view
                previewCapturedImage();
            } else if (resultCode == RESULT_CANCELED) {
                // user cancelled Image capture
                Toast.makeText(getApplicationContext(),
                        "User cancelled image capture", Toast.LENGTH_SHORT)
                        .show();
            } else {
                // failed to capture image
                Toast.makeText(getApplicationContext(),
                        "Sorry! Failed to capture image", Toast.LENGTH_SHORT)
                        .show();
            }
        } else if (requestCode == CAMERA_CAPTURE_VIDEO_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // video successfully recorded
                // preview the recorded video
                previewVideo();
            } else if (resultCode == RESULT_CANCELED) {
                // user cancelled recording
                Toast.makeText(getApplicationContext(),
                        "User cancelled video recording", Toast.LENGTH_SHORT)
                        .show();
            } else {
                // failed to record video
                Toast.makeText(getApplicationContext(),
                        "Sorry! Failed to record video", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    @Override
    protected void onDestroy(){ // Overriding the On Destroy Method
        super.onDestroy();
        if(ttsobject!=null){        // IF text to Speech object is null
            ttsobject.stop();       // Flushing out the Used memory in object
            ttsobject.shutdown();
        }
    }


    private void previewCapturedImage() {   //Display image from a path to ImageView
        try {

            videoPreview.setVisibility(View.GONE);  // hide video preview

            imgPreview.setVisibility(View.VISIBLE);

            BitmapFactory.Options options = new BitmapFactory.Options(); // bitmap factory to store image in bitmap format

            options.inSampleSize = 8;  //downsizing image as it throws OutOfMemory Exception for larger images

            final Bitmap bitmap = BitmapFactory.decodeFile(fileUri.getPath(),options); // Decoding the File Path where image is stored in the mobile (SD card)

            // Image Captured data will be stored in this Mat Vector
            Mat tmp = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC1); // initialize empty Mat of the correct size

            Utils.bitmapToMat(bitmap, tmp); // Convert bitmap to Mat Vector
            // Create an output Mat
            Mat bgr = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC1); // Mat Vector for RGB --> BGR conversion
            Mat hsv = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC1); // Mat Vector for BGR --> HSV conversion
// Converting the color
            Imgproc.cvtColor(tmp, bgr, Imgproc.COLOR_RGB2BGR);      // Color Conversion from RGB to BGR
            Imgproc.cvtColor(bgr, hsv, Imgproc.COLOR_BGR2HSV);      // Color Conversion from BGR to HSV

            Mat lower = new Mat();              // to Store Lower threshold Values of Red Color
            Mat upper = new Mat();              // to Store Higher threshold Values of Red Color
            Mat red_hue_image = new Mat();      // Red Color Images Vector
            Mat green_hue_range = new Mat();    // Green Color Images Vector
            Mat yellow_hue_range = new Mat();   // Yellow Color Images Vector
            Mat circles = new Mat();


            /*
            * In Brief , in HSV color Space the HUE value varies from 0-180 which is 360/2
            * Saturation and Value will be 360
            * In Built Methods for find and filtering Red Color from the initial image*/
            Core.inRange(hsv,new Scalar(0,100,100),new Scalar(10,255,255),lower);  // Find Red Color lower Hue Value which ranges from 0-10
            Core.inRange(hsv,new Scalar(160,100,100),new Scalar(179,255,255),upper); // Find Red Color higher Hue Value which ranges from 160-179
            Core.addWeighted(lower,1.0,upper,1.0,0.0,red_hue_image);        // Calculating the consolidated image from lower and upper vectors
            Imgproc.GaussianBlur(red_hue_image,red_hue_image,new Size(9,9),2,2);   // Applying Gaussian Blur for Removing Noise on the Red Color Output Vector

            Imgproc.Canny(red_hue_image,red_hue_image,100,255);     // Canny Detection Algorithm for finding edges between lower and higher threshold values
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>(); // Array List for finding Contours
            Mat hierarchy = new Mat();
            Imgproc.findContours(red_hue_image,contours,hierarchy,Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_SIMPLE); // Method for finding/Detecting Contours in the image

            for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) { // Going through each pixel in the image for drawing Contours
                    //output = "Red Color Detected, Don't Go Please";
                    Imgproc.drawContours(tmp, contours, contourIdx, new Scalar(0, 255,0), 5);  // Method for drawing contours . here (0,255,0) draws a Green color Border, '5' is the Thickness
                    flag =1;

            }

            /* In Built Methods for finding and filtering Green Color from the initial image*/
            Core.inRange(hsv,new Scalar(50,100,100),new Scalar(70,255,255),green_hue_range); // Finding the Green color pixel with in image based on HUE value of Green Color
            Imgproc.dilate(green_hue_range,green_hue_range,new Mat()); // Applying Dilation Operation to remove noise
            Imgproc.GaussianBlur(green_hue_range,green_hue_range,new Size(9,9),2,2); // Applying Gaussian Blur to Remove Blur in the image if any
            Imgproc.Canny(green_hue_range,green_hue_range,100,255);     // Canny detection algorithm for finding edges
            List<MatOfPoint> contours_green = new ArrayList<MatOfPoint>();  // Array List for finding Contours
            Mat hierarchy_green = new Mat();
            Imgproc.findContours(green_hue_range,contours_green,hierarchy_green,Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_SIMPLE); // Method for finding/Detecting Contours in the image
//            for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {

//                    Imgproc.drawContours(tmp, contours_green, contourIdx, new Scalar(0, 255,0), 5);
//
//            }

            /* In Built Methods for finding and filtering Green Color from the initial image*/
            Core.inRange(hsv,new Scalar(20,100,100),new Scalar(30,255,255),yellow_hue_range); // Finding the Yellow color pixel with in image based on HUE value of Yellow Color
            Imgproc.dilate(yellow_hue_range,yellow_hue_range,new Mat());  // Applying Dilate Method to remove noise from the imagess
            Imgproc.GaussianBlur(yellow_hue_range,yellow_hue_range,new Size(9,9),2,2); // Applying Gaussian Blur
            Imgproc.Canny(yellow_hue_range,yellow_hue_range,100,255);       // Applying Canny Edge Detection Algorithm
            List<MatOfPoint> contours_yellow = new ArrayList<MatOfPoint>();    // Array List for finding Contours
            Mat hierarchy_yellow = new Mat();
            Imgproc.findContours(yellow_hue_range,contours_yellow,hierarchy_yellow,Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_SIMPLE); // Method for finding/Detecting Contours in the image
            for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
                    Imgproc.drawContours(tmp, contours_yellow, contourIdx, new Scalar(0,255,0), 5);
                      //flag =3;

            }


              //Applying Hough Transform for finding Circular Contours as traffic lights will be in the shape
//            Imgproc.HoughCircles(red_hue_image, circles, Imgproc.CV_HOUGH_GRADIENT, 1, red_hue_image.rows()/8, 100, 20, 0, 0);
            Imgproc.HoughCircles(green_hue_range, circles, Imgproc.CV_HOUGH_GRADIENT, 1, green_hue_range.rows()/8, 100, 20, 0, 0);
//            Imgproc.HoughCircles(yellow_hue_range, circles, Imgproc.CV_HOUGH_GRADIENT, 1, yellow_hue_range.rows()/8, 100, 20, 0, 0);

            // Going through each pixel to draw Circular Contour
            if(circles.cols()>0){
               // System.out.println("Test");
               // System.out.println(circles.cols());
                for (int x = 0; x < circles.cols(); x++)
                {

                    flag = 2;
                    output = "Green Color Detected, you Can move";
                    //System.out.print("Green");
                 //   System.out.println(circles.cols());
                    double vCircle[] = circles.get(0,x); // Using Mat Get method to check each pixel by row and Column wise

                    if (vCircle == null)
                        break;

                    Point center = new Point(Math.round(vCircle[0]), Math.round(vCircle[1]));
                    int radius = (int)Math.round(vCircle[2]);
                  //  System.out.print(radius);
                 //   System.out.print("****************88");
                    Imgproc.circle(tmp,center,radius,new Scalar(0,255,0),5); // Method that draws circle if found in green color with 5 thickness

                }
            }

            // Tried for looping through each pixel in the red_hue_image
//
//   //         Mat C = red_hue_image.clone();
//            red_hue_image.convertTo(red_hue_image,CvType.CV_64FC3);
////            // Convert back to bitmap
//            int size = (int) (red_hue_image.total()*red_hue_image.channels());
//            double[] temp = new double[size];
//            red_hue_image.get(0,0,temp);
//            for(int i=0;i<size;i++){
//                if(temp[i]==255){
//                    System.out.print("test******");
////                    if(temp[i]+15==255){
////                        System.out.print("Red");
////                    }else{
////                        System.out.print("Green");
////                    }
//                }else{
//                    System.out.print("Green");
//                }
//            }

            // Converting the initial Vector to a bitmap for displaying output
            Utils.matToBitmap(tmp, bitmap);
            // Calling image setup method
            imgPreview.setImageBitmap(bitmap);
            // Condition if Status is "STOP" or "Don't Walk"
            if(flag==1){
                output= "The Status got is Red Don't Go";
                System.out.print(output);

            }else if(flag==2){  // Condition if Status is "Ok" "Move"
                output = "The Status found is Green you can walk";
                System.out.print(output);
            }else if(flag==3){  // Condition if Status is "WAIT"
                output = "The Status found is Yellow you should wait";
                System.out.print(output);
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Previewing recorded video
     */
    private void previewVideo() {
        try {
            // hide image preview
            imgPreview.setVisibility(View.GONE);

            videoPreview.setVisibility(View.VISIBLE);
            videoPreview.setVideoPath(fileUri.getPath());
            // start playing
            videoPreview.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ------------ Helper Methods ----------------------
     * */


    public Uri getOutputMediaFileUri(int type) {   //Creating file uri to store image/video
        return Uri.fromFile(getOutputMediaFile(type));
    }

    private static File getOutputMediaFile(int type) { //returning image / video

        // External sdcard location
        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                IMAGE_DIRECTORY_NAME);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(IMAGE_DIRECTORY_NAME, "Oops! Failed create "
                        + IMAGE_DIRECTORY_NAME + " directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "IMG_" + timeStamp + ".jpg");


        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }



}
