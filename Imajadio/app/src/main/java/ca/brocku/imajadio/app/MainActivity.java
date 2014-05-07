package ca.brocku.imajadio.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

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
    private View progressBar;
    private SeekBar grainDurationSeekBar;
    private SeekBar seekBar2Temp;
    private TextView progressText;
    private TextView textView2;
    private TextView convertingText;
    private ToggleButton repeatToggleButton;

    private ProgressBar loadingSpinner;

    private float grainDuration;
    private float realGrainDuration;

    private boolean readyToPlay = false;
    private boolean isPlaying = false;


    public final static String APP_PATH_SD_CARD = "/Imajadio/"; //directory to store images

    private Bitmap image; //the image to be converted


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDensity = getResources().getDisplayMetrics().densityDpi;
        image = BitmapFactory.decodeResource(this.getResources(), R.drawable.line_white, options);

        progressBar = (View) findViewById(R.id.progressBar);

        grainDurationSeekBar = (SeekBar) findViewById(R.id.seekBarGrainDuration);
        grainDurationSeekBar.setOnSeekBarChangeListener(new SeekBarHandler());
        grainDuration = 0.025f;

        seekBar2Temp = (SeekBar) findViewById(R.id.seekBar2);
        seekBar2Temp.setOnSeekBarChangeListener(new SeekBarHandler2());


        progressText = (TextView) findViewById(R.id.progressText);
        textView2 = (TextView) findViewById(R.id.textView2);
        convertingText = (TextView) findViewById(R.id.convertingText);

        imgPreview = (ImageView) findViewById(R.id.imgPreview);
        imgPreview.setImageBitmap(image);

        playButton = (Button) findViewById(R.id.playButton);
        playButton.setOnClickListener(new PlayButtonHandler());

        loadingSpinner = (ProgressBar) findViewById(R.id.progressSpinner);

        repeatToggleButton = (ToggleButton) findViewById(R.id.repeatToggleButton);


        playButton.setText("Convert");
        loadingSpinner.setVisibility(View.GONE);
        convertingText.setVisibility(View.GONE);


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

                readyToPlay = false;
                playButton.setText("Convert");

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

            readyToPlay = false;
            playButton.setText("Convert");

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

            Bitmap bitmap = BitmapFactory.decodeFile(fileUri.getPath(),
                    options);

            //fix for orientation
            ExifInterface exif = new ExifInterface(fileUri.getPath());

            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            Log.e("ORITENTATION", String.valueOf(orientation));
            Matrix matrix = new Matrix();

            if (orientation == 8) {
                matrix.postRotate(270);
            } else if (orientation == 3) {
                matrix.postRotate(180);
            } else if (orientation == 6) {
                matrix.postRotate(90);
            }

            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            //end fix for orientation

            //fix for huge resolution
            if (bitmap.getWidth() > 4096 || bitmap.getHeight() > 4096) {
                bitmap = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * .5), (int) (bitmap.getHeight() * .5), false);
            }
            //end fix for huge resolution

            imgPreview.setImageBitmap(bitmap);

            imgPreview.setDrawingCacheEnabled(false);
            imgPreview.setDrawingCacheEnabled(true);
            image = imgPreview.getDrawingCache();


        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (IOException e) {
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

        File f = new File(Environment.getExternalStorageDirectory() + "/DCIM/Camera");

        Log.i("Log", "file name in delete folder :  " + f.toString());
        File[] files = f.listFiles();

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

        if(readyToPlay){


        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());

        String fullPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Imajadio/";


        try {
            File dir = new File(fullPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        } catch (Exception e) {
            Log.e("saveWav()", e.getMessage());
            return false;
        }

        FileOutputStream all = new FileOutputStream(fullPath + "IMAJADIO_" + timeStamp + ".wav");

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
        }
        Toast.makeText(getApplicationContext(), "Audio NOT converted yet", Toast.LENGTH_SHORT).show();
        return false;
    }//save


    private class PlayButtonHandler implements View.OnClickListener {
        @Override
        public void onClick(View view) {

            Log.e("REPEAT BUTTON", String.valueOf(repeatToggleButton.isChecked()));

            if (readyToPlay == true) {


                imajadio.play();
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        isPlaying = true;
                        onUpdateProgressBar(-1);

                        for (int i = 0; i < imgPreview.getWidth(); i++) {

                            try {
                                onUpdateProgressBar(i);
                                Thread.sleep((long) (realGrainDuration * 1000));


                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        onUpdateProgressBar(-1);

                        isPlaying = true;
                    }
                }).start();
            } else {


                new AsyncTaskImajadio().execute();

            }

        }

    }//PlayButtonHandler

    private class SeekBarHandler implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
            grainDuration = (float) ((progress + 1) * .001);
            progressText.setText(String.valueOf(grainDuration));

            readyToPlay = false;
            playButton.setText("Convert");
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }

    private class SeekBarHandler2 implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {

            textView2.setText(String.valueOf(progress));

            readyToPlay = false;
            playButton.setText("Convert");
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }

    Handler handie = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle b = msg.getData();
            int position = b.getInt("PROGRESS_POSITION");

            if (position == -1) {
                progressBar.setVisibility(progressBar.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
            } else {
                progressBar.setX(position);
            }
        }
    };

    public void onUpdateProgressBar(int i) {
        Message message = new Message();
        Bundle b = new Bundle();
        b.putInt("PROGRESS_POSITION", i);
        message.setData(b);
        handie.sendMessage(message);
    }


    private class AsyncTaskImajadio extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            realGrainDuration = grainDuration;
            loadingSpinner.setVisibility(View.VISIBLE);
            convertingText.setVisibility(View.VISIBLE);
            playButton.setEnabled(false);
            grainDurationSeekBar.setEnabled(false);
            seekBar2Temp.setEnabled(false);
        }

        @Override
        protected Void doInBackground(Void... voids) {

            imajadio = new Imajadio(image, 16, grainDuration);
            Log.e("IMAGE DIMENS (H/W)", image.getHeight() + "; " + image.getWidth());

            imajadio.bitmapToAudio();

            //imajadio.normalizeAudio();

            readyToPlay = true;

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            playButton.setText("PLAY");
            loadingSpinner.setVisibility(View.GONE);
            convertingText.setVisibility(View.GONE);

            playButton.setEnabled(true);
            grainDurationSeekBar.setEnabled(true);
            seekBar2Temp.setEnabled(true);
        }
    }//AsyncTaskImajadio
}
