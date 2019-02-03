package com.sougata.qrcoder;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    Button selectFileButton, uploadFileButton, scanQRCodeButton;
    TextView fileNotification;
    ImageView qrCodeImageView;
    FirebaseStorage mStorageRef; // Stores files
    FirebaseDatabase mDatabaseRef; // Stores download URL
    Uri filePath; // Path to the file from local storage
    ProgressDialog progressDialog;
    String currentTime;
    String downloadUrl;
    String displayName;
    private static String TAG = "SANTY";


    private static int READ_FILE_PERMISSION = 0;
    private static int WRITE_FILE_PERMISSION = 3;
    private static int CAMERA_PERMISSION = 1;
    private static int READ_FILE_REQUEST_FROM_STORAGE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStorageRef = FirebaseStorage.getInstance(); // returns an Object of Firebase Storage
        mDatabaseRef = FirebaseDatabase.getInstance(); // returns an Object of Firebase Database

        selectFileButton = findViewById(R.id.selectFileButtonId);
        uploadFileButton = findViewById(R.id.uploadButtonId);
        fileNotification = findViewById(R.id.fileReferenceId);
        qrCodeImageView = findViewById(R.id.qrCodeImageViewId);
        scanQRCodeButton = findViewById(R.id.scanQRCodebuttonId);

        selectFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                    chooseFile();
                } else {
                    // The below line will ask user to grant permission for reading storage
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_FILE_PERMISSION);
                }
            }
        });

        uploadFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (filePath != null) {

                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                        uploadFile(filePath);
                    } else {
                        // The below line will ask user to grant permission for reading storage
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_FILE_PERMISSION);
                    }

//                    uploadFile(filePath);
                } else {
                    Toast.makeText(MainActivity.this, "Please select a file", Toast.LENGTH_SHORT).show();
                }
            }
        });

        scanQRCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {

                    Intent intent = new Intent(MainActivity.this, QRScanActivity.class);
                    startActivityForResult(intent, 100);
                } else {
                    // The below line will ask user to grant permission for reading storage
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
                }


            }
        });

    }

    private void uploadFile(Uri filePath) {
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setTitle("Generating QR...");
        progressDialog.setProgress(0);
        progressDialog.show();
        currentTime = String.valueOf(System.currentTimeMillis());
        StorageReference storageReference = mStorageRef.getReference("files").child(displayName); // returns the root path

        storageReference.putFile(filePath) // file stored to firebase
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the download content
//                        String downloadUrl = storageReference.getDownloadUrl().toString();
                        taskSnapshot.getMetadata().getReference().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                downloadUrl = uri.toString();
                                generateQRCode(downloadUrl);
//                                fileNotification.setText(downloadUrl);
//                                Toast.makeText(MainActivity.this, "File successfully uploaded", Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            }
                        });
//                        DatabaseReference databaseReference = mDatabaseRef.getReference("data").child(currentTime); // returns the root path
//                        databaseReference.setValue(downloadUrl).addOnCompleteListener(new OnCompleteListener<Void>() {
//                            @Override
//                            public void onComplete(@NonNull Task<Void> task) {
//                                // Check if the downloadURL is successfully upload to realtime database
//                                if (task.isSuccessful()) {
//                                    Toast.makeText(MainActivity.this, "File successfully uploaded", Toast.LENGTH_SHORT).show();
//                                    progressDialog.dismiss();
//                                } else {
//                                    Toast.makeText(MainActivity.this, "File not successfully uploaded", Toast.LENGTH_SHORT).show();
//                                    progressDialog.dismiss();
//                                }
//                            }
//                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "File not successfully uploaded", Toast.LENGTH_SHORT).show();
                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {

                // track progress of upload
                int currentProgress = (int) (100 * (taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount()));
                progressDialog.setProgress(currentProgress);
            }
        });
    }

    private void generateQRCode(String downloadUrl) {

        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(downloadUrl, BarcodeFormat.QR_CODE, qrCodeImageView.getWidth(), qrCodeImageView.getHeight());
//            BitMatrix bitMatrix = multiFormatWriter.encode(downloadUrl, BarcodeFormat.QR_CODE, 750, 750);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            qrCodeImageView.setImageBitmap(bitmap);
            storeImage(bitmap);
            Toast.makeText(this, "QR Code Saved", Toast.LENGTH_SHORT).show();
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private void storeImage(Bitmap image) {
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            Log.d(TAG,
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    /**
     * Create a File for saving an image or video
     */
    private File getOutputMediaFile() {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
//        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
//                + "/Android/data/"
//                + getApplicationContext().getPackageName()
//                + "/Files");

        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/Download/QRImages");

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
        File mediaFile;
        String mImageName = "QR_" + displayName.substring(0, displayName.indexOf('.')) + ".png";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }


    // The below method will acknowledge the permission given by the user
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == READ_FILE_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            chooseFile();

        } else if (requestCode == CAMERA_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(MainActivity.this, QRScanActivity.class);
            startActivityForResult(intent, 100);

        } else if (requestCode == WRITE_FILE_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            uploadFile(filePath);

        } else {
            Toast.makeText(this, "Please grant permissions.", Toast.LENGTH_SHORT).show();
        }
    }

    private void chooseFile() {
        // To read file and select a file using Intent

        Intent intent = new Intent();
        intent.setType("application/pdf"); // to read only .pdf files
        intent.setAction(Intent.ACTION_GET_CONTENT); // to fetch files
        startActivityForResult(intent, READ_FILE_REQUEST_FROM_STORAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        // Check whether user has selected a file or not
        // requestCode -> check whether the intent to choose file is called
        // resultCode -> check whether user has opened the file manager choose file and closed it
        // data -> whether the chosen file (data) is not null
        if (requestCode == READ_FILE_REQUEST_FROM_STORAGE && resultCode == RESULT_OK && data != null) {
//            filePath = data.getData(); // returns the URI of the file (path)
//            fileNotification.setText(String.format("File Selected : %s", filePath.getPathSegments()));

//      ------------------------------------------------------------------------------------------
            // Get the Uri of the selected file
            filePath = data.getData();
            String uriString = String.valueOf(filePath);
            File myFile = new File(uriString);
//            String displayName = null;

            if (uriString.startsWith("content://")) {
                Cursor cursor = null;
                try {
                    cursor = this.getContentResolver().query(filePath, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            } else if (uriString.startsWith("file://")) {
                displayName = myFile.getName();
            }
            fileNotification.setText(String.format("File Selected : %s", displayName));
//      --------------------------------------------------------------------------------------------------
        } else if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            fileNotification.setText(String.format("Click : %s", data.getStringExtra("URL")));

        } else {
//            Toast.makeText(this, "Please select a file.", Toast.LENGTH_SHORT).show();
        }
    }
}