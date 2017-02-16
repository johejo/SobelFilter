package com.example.mitsuo.sobelfilter;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static int SOBEL_APP = 1001;

    public static int CAMERA = 1;
    public static int GALLERY = 2;

    private final static int REQUEST_PERMISSION_STORAGE = 1002;
    private final static int REQUEST_PERMISSION_CAMERA = 1003;

    public int intentType = 0;
    ImageView iv;
    Bitmap bitmap;
    SobelTask sobelTask;
    Button btCamera, btGallery, btReset, btStart, btSave, btSample;

    private Uri cameraUri;

    private String filePath;
    private File  cameraFile;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
//                    Log.d(TAG, "OpenCV loaded successfully");

                    /* Now enable camera view to start receiving frames */
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
//            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
//            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null){
            cameraUri = savedInstanceState.getParcelable("CaptureUri");
        }

        bitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.sample);

        iv = (ImageView) findViewById(R.id.imageView);

        start();
    }

    private void start(){
        setContentView(R.layout.activity_main);

        btCamera = (Button)findViewById(R.id.button);
        btGallery = (Button)findViewById(R.id.button2);
        btReset = (Button)findViewById(R.id.button4);
        btStart = (Button)findViewById(R.id.button3);
        btSave = (Button) findViewById(R.id.button6);
        btSample = (Button)findViewById(R.id.button5) ;

        btSave.setEnabled(false);

        iv = (ImageView) findViewById(R.id.imageView);

        sobelTask = new SobelTask(this, iv);

        btCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intentType = CAMERA;
                if (Build.VERSION.SDK_INT >= 23) {
                    checkPermission();
                }
                else {
                    cameraIntent();
                }
            }
        });

        btGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intentType = GALLERY;
                Intent it = new Intent();
                it.setType("image/*");
                it.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(it, SOBEL_APP);
            }
        });

        btReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start();
                iv.setImageBitmap(bitmap);
            }
        });

        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btCamera.setEnabled(false);
                btGallery.setEnabled(false);
                btStart.setEnabled(false);
                bitmap = ((BitmapDrawable)iv.getDrawable()).getBitmap();
                sobelTask.execute(bitmap);
                btSave.setEnabled(true);
                btSample.setEnabled(false);
            }
        });

        btSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveImage();
            }
        });

        btSample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.sample);
                iv.setImageBitmap(bitmap);
                btSave.setEnabled(false);
                btReset.setEnabled(true);
                btStart.setEnabled(true);
                btCamera.setEnabled(true);
                btGallery.setEnabled(true);
            }
        });
    }

    private void cameraIntent(){
        // 保存先のフォルダーを作成
        File cameraFolder = new File(
                Environment.getExternalStorageDirectory().getAbsolutePath(), "SobelFilter"
        );

        try {
            if (!cameraFolder.exists()) {
                cameraFolder.mkdir();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        // 保存ファイル名
        String fileName =generateFileName();
        filePath = cameraFolder.getPath() +"/" + fileName;
        Log.d("debug","filePath:"+filePath);



        // capture画像のファイルパス
        cameraFile = new File(filePath);
//        cameraUri = Uri.fromFile(cameraFile);
        cameraUri = FileProvider.getUriForFile(MainActivity.this, getApplicationContext().getPackageName() + ".provider", cameraFile);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
        startActivityForResult(intent, SOBEL_APP);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent it) {
        if (requestCode == SOBEL_APP && resultCode == RESULT_OK) {
            if (intentType == GALLERY) {
                Uri u = it.getData();
                try {
                    InputStream is = getContentResolver().openInputStream(u);
                    bitmap = BitmapFactory.decodeStream(is);
                    iv.setImageBitmap(bitmap);
                } catch (Exception e) {
                }
            } else if (intentType == CAMERA) {
                if(cameraUri != null){
                    iv.setImageURI(cameraUri);
                }
            }
        }
    }

    protected void onSaveInstanceState(Bundle outState){
        outState.putParcelable("CaptureUri", cameraUri);
    }

    public void saveImage(){
        bitmap = ((BitmapDrawable)iv.getDrawable()).getBitmap();

        //外部ストレージへのアクセスを確認する
        if (!isExternalStorageWritable()) {
            Log.i("saveImageExternal", "External Storage Not Writable.");
            return;
        }

        //パスを取得する
        String storagePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        String directoryName = "/SobelFilter/";
        String fileName = directoryName + generateFileName();

        //保存先のディレクトリがなければ作成する
        File file = new File(storagePath + directoryName);
        try {
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        file = new File(storagePath, fileName);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();

            addImageToGallery(file.getAbsolutePath());
        } catch (IOException e ) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Toast toast = Toast.makeText(this, "Saved " + file.getPath(), Toast.LENGTH_LONG);
        toast.show();
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    private void addImageToGallery(String filePath) {
        try {
            ContentValues values = new ContentValues();

            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.MediaColumns.DATA, filePath);

            getApplicationContext().getContentResolver()
                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String generateFileName() {
        Date date = new Date();
        SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH);
        return fileNameFormat.format(date) + ".jpg";
    }

    // Runtime Permission check
    private void checkPermission(){
        boolean storagePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED;
        boolean cameraPermission =  ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)== PackageManager.PERMISSION_GRANTED;
        // 既に許可している
        if (storagePermission && cameraPermission){
            cameraIntent();
        }
        // 拒否していた場合
        else{
            requestStoragePermission();
        }
    }

    // 許可を求める
    private void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_STORAGE);

        } else {
            Toast toast = Toast.makeText(this, "Need Permission", Toast.LENGTH_SHORT);
            toast.show();

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,}, REQUEST_PERMISSION_STORAGE);

        }
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION_CAMERA);

        } else {
            Toast toast = Toast.makeText(this, "Need Permission", Toast.LENGTH_SHORT);
            toast.show();

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,}, REQUEST_PERMISSION_CAMERA);

        }
    }

    // 結果の受け取り
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_STORAGE) {
            // 使用が許可された
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission();
                return;

            } else {
                // それでも拒否された時の対応
                Toast toast = Toast.makeText(this, "Error", Toast.LENGTH_SHORT);
                toast.show();
            }
        } else if (requestCode == REQUEST_PERMISSION_CAMERA) {
            // 使用が許可された
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                cameraIntent();
                return;

            } else {
                // それでも拒否された時の対応
                Toast toast = Toast.makeText(this, "Error", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }
}
