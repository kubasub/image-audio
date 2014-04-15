package ca.brocku.imajadio.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import imajadio.Imajadio;
import imajadio.WaveHeader;

public class MainActivity extends Activity {

    private ShareActionProvider mShareActionProvider;

    Imajadio imajadio;

    // Activity request codes
    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    public static final int MEDIA_TYPE_IMAGE = 1;
    private static int RESULT_LOAD_IMAGE = 2;
    // directory name to store captured images
    private static final String IMAGE_DIRECTORY_NAME = "Imajadio";
    private Uri fileUri; // file url to store image

    private ImageView imgPreview;
    private Button playButton;
    public final static String APP_PATH_SD_CARD = "/Imajadio/"; //directory to store images

    private Bitmap image; //the image to be converted


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDensity = getResources().getDisplayMetrics().densityDpi;
        image = BitmapFactory.decodeResource(this.getResources(), R.drawable.linear_decreasing_freq, options);

        imgPreview = (ImageView) findViewById(R.id.imgPreview);
        imgPreview.setImageBitmap(image);

        playButton = (Button) findViewById(R.id.playButton);
        playButton.setOnClickListener(new PlayButtonHandler());



    }

    @Override
    protected void onResume() {
        super.onResume();
    }//onResume

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if the result is capturing Image
        if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // successfully captured the image
                // display it in image view
                previewCapturedImage();

                //deletes the file after it has been displayed
                //this is the uncropped image getting deleted
                File file = new File(fileUri.getPath());
                boolean deleted = file.delete();


            } else if (resultCode == RESULT_CANCELED) {
                // user cancelled Image capture
            } else {
                // failed to capture image
            }
        } else if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            //This is to LOAD IMAGE FROM SDCARD
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex); //contains the path of the selected image
            cursor.close();

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inDensity = getResources().getDisplayMetrics().densityDpi;
            image = BitmapFactory.decodeFile(picturePath, options);

            //Bitmap test = BitmapFactory.decodeResource(this.getResources(), R.drawable.decreasing_freq, options); //used to test images in drawable/

            imgPreview.setImageBitmap(image);

           // image = ((BitmapDrawable)imgPreview.getDrawable()).getBitmap();

            imgPreview.setDrawingCacheEnabled(false);
            imgPreview.setDrawingCacheEnabled(true);
            image = imgPreview.getDrawingCache();


            Log.e("impreveiw height", String.valueOf(imgPreview.getHeight()));
            Log.e("impreveiw weight", String.valueOf(imgPreview.getWidth()));

        }
    }//onActivityResult

    /**
     * Here we store the file url as it will be null after returning from camera
     * app
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save file url in bundle as it will be null on screen orientation
        // changes
        outState.putParcelable("file_uri", fileUri);
    }//onSaveInstanceState

    /*
     * Here we restore the fileUri again
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // get the file url
        fileUri = savedInstanceState.getParcelable("file_uri");
    }//onRestoreInstanceState

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }//onCreateOptionsMenu


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {

            case R.id.action_settings:
                return true;

            case R.id.action_exportAudio:

                try {
                    saveWav();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                return true;

            case R.id.action_newImage:
                captureImage();
                return true;

            case R.id.action_loadImage:
                //load image from sdcard
                Intent i = new Intent(
                Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, RESULT_LOAD_IMAGE);
                return true;

            case R.id.action_saveImage:
                //export image to sdcard
                imgPreview.setDrawingCacheEnabled(false);
                imgPreview.setDrawingCacheEnabled(true);
                Bitmap bitmap = imgPreview.getDrawingCache();
                saveImageToExternalStorage(bitmap);
                deleteLastFromDCIM();

                return true;
            case R.id.action_exit:
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;

        }

        return super.onOptionsItemSelected(item);

    }//onOptionsItemSelected


    public boolean saveImageToExternalStorage(Bitmap image) {
        String fullPath = Environment.getExternalStorageDirectory().getAbsolutePath() + APP_PATH_SD_CARD;
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());

        try {
            File dir = new File(fullPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            OutputStream fOut = null;
            File file = new File(fullPath, "IMG_" + timeStamp + ".png");
            file.createNewFile();
            fOut = new FileOutputStream(file);

            // 100 means no compression, the lower you go, the stronger the compression
            image.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();

            //This is important. It is used to let the scanner know a new file was added.
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));


            MediaStore.Images.Media.insertImage(getApplicationContext().getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());
            Toast.makeText(getApplicationContext(), "Saved to Imajadio folder", Toast.LENGTH_SHORT).show();
            return true;

        } catch (Exception e) {
            Log.e("saveToExternalStorage()", e.getMessage());
            return false;
        }
    }//saveImageToExternalStorage


    ////////START CAMERA HELPER METHODS///////////////////////////////////////////////////////////////////////////////

    /**
     * Checking device has camera hardware or not
     */
    private boolean isDeviceSupportCamera() {
        if (getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }//isDeviceSupportCamera

    /*
    * Capturing Camera Image will launch camera app request image capture
    */
    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

        // start the image capture Intent
        startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
    }//captureImage

    /**
     * Receiving activity result method will be called after closing the camera
     */

     /*
     * Display image from a path to ImageView
     */
    private void previewCapturedImage() {
        try {
            imgPreview.setVisibility(View.VISIBLE);

            // bitmap factory
            BitmapFactory.Options options = new BitmapFactory.Options();

            final Bitmap bitmap = BitmapFactory.decodeFile(fileUri.getPath(),
                    options);

            imgPreview.setImageBitmap(bitmap);

            imgPreview.setDrawingCacheEnabled(false);
            imgPreview.setDrawingCacheEnabled(true);
            image = imgPreview.getDrawingCache();


        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }//previewCapturedImage

    /**
     * Creating file uri to store image/video
     */
    public Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }//getOutputMediaFileUri

    /*
     * returning image / video
     */
    private static File getOutputMediaFile(int type) {

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
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }


        return mediaFile;
    }//getOutputMediaFile
    ////////END CAMERA HELPER METHODS///////////////////////////////////////////////////////////////////////////////


    private void deleteLastFromDCIM() {

        File f = new File(Environment.getExternalStorageDirectory() + "/DCIM/Camera" );

        Log.i("Log", "file name in delete folder :  "+f.toString());
        File [] files = f.listFiles();

        //Log.i("Log", "List of files is: " +files.toString());
        Arrays.sort(files, new Comparator<Object>() {
            public int compare(Object o1, Object o2) {

                if (((File) o1).lastModified() > ((File) o2).lastModified()) {
                    //         Log.i("Log", "Going -1");
                    return -1;
                } else if (((File) o1).lastModified() < ((File) o2).lastModified()) {
                    //     Log.i("Log", "Going +1");
                    return 1;
                } else {
                    //     Log.i("Log", "Going 0");
                    return 0;
                }
            }
        });

        //Log.i("Log", "Count of the FILES AFTER DELETING ::"+files[0].length());
        files[0].delete();

    }//deleteLastFromDCIM


    public boolean saveWav() throws FileNotFoundException {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());

        String fullPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Imajadio/";
        FileOutputStream all = new FileOutputStream(fullPath + "IMAJADIO_"+timeStamp+".wav");

        //This creates the header for the wav file.
        WaveHeader w = new WaveHeader((short) 1, imajadio.getAudioChannelCount(), imajadio.getAudioSampleRate(), (short) 16, imajadio.getDATA().length);

        Log.e("HEADERINFO", w.toString());

        try {
            //write header
            w.write(all);

            //write the data
            all.write(imajadio.getDATA());

            all.close();

            Toast.makeText(getApplicationContext(), "Saved audio to Imajadio folder", Toast.LENGTH_SHORT).show();

                    } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }//save

    private class PlayButtonHandler implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            //Start IMAJADIO WORK

            imajadio = new Imajadio(image, 16, .1);
            Log.e("IMAGE DIMENS (H/W)", image.getHeight() + "; " + image.getWidth());

            imajadio.bitmapToAudio();

            imajadio.audioNormalize();

            imajadio.play();
            //End IMAJADIO WORK
        }
    }
}
