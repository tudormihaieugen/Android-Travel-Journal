package com.tudormihai.traveljournal.Activities;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.tudormihai.traveljournal.FirebaseDB.Trip;
import com.tudormihai.traveljournal.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class NewTripActivity extends AppCompatActivity {

    private static final String TAG = "tag";

    private ImageView returnToHomeImageView;
    private ImageView openCameraImageView;
    private ImageView openGalleryImageView;
    private ImageView uploadedPhotoImageView;

    private String pathToFile;
    private String photoURL;

    private ProgressDialog mLoadingBar;

    private StorageReference storageReference;
    private DatabaseReference databaseTripReference;

    public static final int CAMERA_REQUEST_CODE = 101;
    public static final int GALLERY_REQUEST_CODE = 102;

    private EditText tripNameEditText;
    private EditText destinationEditText;

    private RadioGroup tripTypeRadioGroup;

    private TextView showPriceEUR;
    private TextView startDateTextView;
    private TextView endDateTextView;

    private SeekBar priceSeekBar;

    private RatingBar ratingBarTrip;

    private int year;
    private int month;
    private int day;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_trip);

        openCameraImageView = findViewById(R.id.openCameraImageView);
        openGalleryImageView = findViewById(R.id.openGalleryImageView);
        uploadedPhotoImageView = findViewById(R.id.uploadedPhotoImageView);
        tripNameEditText = findViewById(R.id.tripNameEditText);
        destinationEditText = findViewById(R.id.destinationEditText);
        tripTypeRadioGroup = (RadioGroup) findViewById(R.id.tripTypeRadioGroup);
        showPriceEUR = findViewById(R.id.showPriceEUR);
        startDateTextView = findViewById(R.id.startDateTextView);
        endDateTextView = findViewById(R.id.endDateTextView);
        priceSeekBar = (SeekBar) findViewById(R.id.priceSeekBar);
        ratingBarTrip = (RatingBar) findViewById(R.id.ratingBarTrip);

        mLoadingBar = new ProgressDialog(NewTripActivity.this);

        storageReference = FirebaseStorage.getInstance().getReference();
        databaseTripReference = FirebaseDatabase.getInstance().getReference("trips");

        priceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                showPriceEUR.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        openCameraImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        openGalleryImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(gallery, GALLERY_REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                File f = new File(pathToFile);
                uploadedPhotoImageView.setImageURI(Uri.fromFile(f));
                Log.d("tag", "Photo URL: " + Uri.fromFile(f));
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(f);
                mediaScanIntent.setData(contentUri);
                this.sendBroadcast(mediaScanIntent);
                uploadImageToFirebase(f.getName(), contentUri);
            }
        }

        if (requestCode == GALLERY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri contentUri = data.getData();
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = "TravelJournal_" + timeStamp + "." + getFileExt(contentUri);
                Log.d("tag", "Gallery URI:  " + imageFileName);
                uploadedPhotoImageView.setImageURI(contentUri);
                uploadImageToFirebase(imageFileName, contentUri);
            }
        }
    }

    public void onClickReturnToHome(View view) {
            Intent intent = new Intent(getApplicationContext(), MainScreen.class);
            startActivity(intent);
    }

    private void uploadImageToFirebase(String name, Uri contentUri) {
        mLoadingBar.setTitle("Uploading photo");
        mLoadingBar.setMessage("Please wait");
        mLoadingBar.setCanceledOnTouchOutside(false);
        mLoadingBar.show();

        final StorageReference image = storageReference.child("pictures/" + name);
        image.putFile(contentUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {

            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                image.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        mLoadingBar.dismiss();
                        photoURL = uri.toString();
                    }
                });
                Toast.makeText(NewTripActivity.this, "Photo Uploaded Succesfully", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                mLoadingBar.dismiss();
                Toast.makeText(NewTripActivity.this, "Upload Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getFileExt(Uri contentUri) {
        ContentResolver c = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(c.getType(contentUri));
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "TravelJournal_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        pathToFile = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                Log.e(TAG, String.valueOf(e));
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.tudormihai.traveljournal.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
        }
    }

    public void addTripOnClick(View view) {
        String tripName = tripNameEditText.getText().toString().trim();
        String destination = destinationEditText.getText().toString().trim();

        int selectedIdRadioGroup = tripTypeRadioGroup.getCheckedRadioButtonId();
        RadioButton radioButtonSelectedOption = (RadioButton) findViewById(selectedIdRadioGroup);

        int priceOfTrip = priceSeekBar.getProgress();
        float tripRating = ratingBarTrip.getRating();

        String startDate = startDateTextView.getText().toString().trim();
        String endDate = endDateTextView.getText().toString().trim();

        String url = photoURL;

        if (TextUtils.isEmpty(tripName)) {
            Toast.makeText(this, "Please complete the Trip Name", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(destination)) {
            Toast.makeText(this, "Please complete the Trip Destination", Toast.LENGTH_SHORT).show();
        } else if (selectedIdRadioGroup == -1) {
            Toast.makeText(this, "Please select the Trip Type", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(url)) {
            Toast.makeText(this, "Please upload a Trip Image", Toast.LENGTH_SHORT).show();
        } else {
            String radioButtonToStringTripType = (String) radioButtonSelectedOption.getText();

            String id = databaseTripReference.push().getKey();

            Trip trip = new Trip(id, tripName, destination, radioButtonToStringTripType, priceOfTrip, tripRating, startDate, endDate, url, false);

            databaseTripReference.child(id).setValue(trip);

            Toast.makeText(this, "Trip added successfully", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(NewTripActivity.this, MainScreen.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    public void onClickPickStartDate(View view) {
        final Calendar calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.getTime();

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                startDateTextView.setText(dayOfMonth + "-" + (month + 1) + "-" + year);
            }
        }, year, month, day);
        datePickerDialog.show();
    }

    public void onClickPickEndDate(View view) {
        final Calendar calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.getTime();

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                endDateTextView.setText(dayOfMonth + "-" + (month + 1) + "-" + year);
            }
        }, year, month, day);
        datePickerDialog.show();
    }
}