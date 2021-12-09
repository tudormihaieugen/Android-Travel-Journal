package com.tudormihai.traveljournal.Activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tudormihai.traveljournal.FirebaseDB.Trip;
import com.tudormihai.traveljournal.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ViewTripActivity extends AppCompatActivity {

    private String tripId;
    private String destinationNameString;
    private String tripTypeString;
    private String toolbarTitleString;
    private String tripPriceString;
    private String tripStartDateString;
    private String tripEndDateString;
    private String tripImageString;
    private float ratingTrip;
    private int priceInteger;

    private Toolbar toolbarTitle;
    private TextView destinationNameTextView;
    private TextView viewTripTypeTextView;
    private TextView viewTripPriceTextView;
    private TextView viewTripActualStartDateTextView;
    private TextView viewTripActualEndDateTextView;
    private ImageView tripImageView;
    private RatingBar tripRatingViewTrip;

    private TextView locationTemperature;
    private TextView weatherDescription;
    private ImageView temperatureImageView;
    private ImageView descriptionImageView;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    FloatingActionButton fabAddToFavorites;
    FloatingActionButton fabDeleteTrip;
    FloatingActionButton fabShareItem;

    Boolean fav;

    private static final String API_KEY = "c71f6bfc9b5850bcfad67ee704baf9d9";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_view_trip);

        toolbarTitle = findViewById(R.id.tripNameToolbarText);
        destinationNameTextView = findViewById(R.id.destinationName);
        viewTripTypeTextView = findViewById(R.id.viewTripTypeTextView);
        viewTripPriceTextView = findViewById(R.id.viewTripPriceTextView);
        viewTripActualStartDateTextView = findViewById(R.id.viewTripActualStartDateTextView);
        viewTripActualEndDateTextView = findViewById(R.id.viewTripActualEndDateTextView);
        tripImageView = findViewById(R.id.tripImageView);
        tripRatingViewTrip = findViewById(R.id.tripRatingViewTrip);

        locationTemperature = findViewById(R.id.locationTemperature);
        weatherDescription = findViewById(R.id.weatherDescription);
        temperatureImageView = findViewById(R.id.temperatureImageView);
        descriptionImageView = findViewById(R.id.descriptionImageView);

        tripId = getIntent().getExtras().get("tripId").toString();
        destinationNameString = getIntent().getExtras().get("tripDestination").toString();
        toolbarTitleString = getIntent().getExtras().get("tripName").toString();
        tripTypeString = getIntent().getExtras().get("tripType").toString();
        tripPriceString = getIntent().getExtras().get("tripPrice").toString();
        tripStartDateString = getIntent().getExtras().get("startDate").toString().trim();
        tripEndDateString = getIntent().getExtras().get("endDate").toString();
        tripImageString = getIntent().getExtras().get("photo").toString();
        ratingTrip = getIntent().getExtras().getFloat("rating");

        toolbarTitle.setTitle(toolbarTitleString.toString().trim());
        destinationNameTextView.setText(destinationNameString.toString().trim());
        viewTripTypeTextView.setText(tripTypeString.toString().trim());
        viewTripPriceTextView.setText(tripPriceString.toString().trim() + " €");
        viewTripActualStartDateTextView.setText(tripStartDateString);
        viewTripActualEndDateTextView.setText(tripEndDateString);
        tripRatingViewTrip.setRating(ratingTrip);

        fabAddToFavorites = findViewById(R.id.fabAddToFavorites);
        fabDeleteTrip = findViewById(R.id.fabDeleteTrip);
        fabShareItem = findViewById(R.id.fabShareItem);

        Glide.with(getApplicationContext()).load(tripImageString).into(tripImageView);

        priceInteger = Integer.parseInt(tripPriceString);

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("trips");

        fav = getIntent().getExtras().getBoolean("fav");

        showTemperature();

        printKeyHash();

        final DatabaseReference dbSelectedTrip = firebaseDatabase.getReference("trips").child(fav.toString());

        if (dbSelectedTrip.toString().contains("true")) {
            fabAddToFavorites.setImageResource(R.drawable.ic_baseline_favorite_24);
        } else if (dbSelectedTrip.toString().contains("false")) {
            fabAddToFavorites.setImageResource(R.drawable.ic_baseline_favorite_border_24_blue);
        }


        fabAddToFavorites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickAddOrRemoveFromFavorites(tripId);
            }
        });

        fabDeleteTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder deleteDialog = new AlertDialog.Builder(ViewTripActivity.this);
                deleteDialog.setMessage("Are you sure you want to delete this trip?").setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteTrip(tripId);
                                Intent intent = new Intent(getApplicationContext(), MainScreen.class);
                                startActivityForResult(intent, 1);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                AlertDialog alert = deleteDialog.create();
                alert.setTitle("Delete trip");
                alert.show();
            }
        });

        fabShareItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                String shareBody = "Here is the share content body";
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Awesome Trip");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, "Share via"));
            }
        });
    }

    private void printKeyHash() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo("com.tudormihai.traveljournal", PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest messageDigest = MessageDigest.getInstance("SHA");
                messageDigest.update(signature.toByteArray());
                Log.d("KeyHash", Base64.encodeToString(messageDigest.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public void onClickAddOrRemoveFromFavorites(String tripId) {
        final DatabaseReference dbSelectedTrip = firebaseDatabase.getReference("trips").child(fav.toString());

        Log.d("Is trip favorite", dbSelectedTrip.toString());

        if (dbSelectedTrip.toString().contains("true")) {
            fabAddToFavorites.setImageResource(R.drawable.ic_baseline_favorite_border_24_blue);
            removeFromFavorites(tripId);
        } else {
            fabAddToFavorites.setImageResource(R.drawable.ic_baseline_favorite_24);
            addToFavorites(tripId);
        }
    }

    public void addToFavorites(String tripId) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("trips").child(tripId);
        boolean isFavorite = true;
        Trip favTrip = new Trip(tripId, toolbarTitleString, destinationNameString, tripTypeString, priceInteger, ratingTrip, tripStartDateString, tripEndDateString, tripImageString, isFavorite);
        databaseReference.setValue(favTrip);
        Toast.makeText(getApplicationContext(), "Added to Favorites list", Toast.LENGTH_LONG).show();
    }

    public void removeFromFavorites(String tripId) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("trips").child(tripId);
        boolean isFavorite = false;
        Trip favTrip = new Trip(tripId, toolbarTitleString, destinationNameString, tripTypeString, priceInteger, ratingTrip, tripStartDateString, tripEndDateString, tripImageString, isFavorite);
        databaseReference.setValue(favTrip);
        Toast.makeText(getApplicationContext(), "Removed from Favorites list", Toast.LENGTH_LONG).show();
    }

    public void deleteTrip(String tripId) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("trips").child(tripId);
        databaseReference.removeValue();
        Toast.makeText(getApplicationContext(), "Trip deleted", Toast.LENGTH_LONG).show();
    }

    public void showTemperature() {
        String url = "https://api.openweathermap.org/data/2.5/weather?q=" + destinationNameString + "&appid=" + API_KEY + "&units=Metric";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject jsonObject = response.getJSONObject("main");
                    JSONArray jsonArray = response.getJSONArray("weather");
                    JSONObject object = jsonArray.getJSONObject(0);
                    double temperatureDouble = jsonObject.getDouble("temp");
                    String description = object.getString("main");


                    double temperatureRounded = (int) Math.round(temperatureDouble);
                    int temperatureInt = (int) temperatureRounded;
                    String temp = String.valueOf(temperatureInt + "°C");

                    temperatureImageView.setImageResource(R.drawable.ic_temperature);
                    locationTemperature.setText(temp);

                    if (description.contains("Cloud")) {
                        descriptionImageView.setImageResource(R.drawable.ic_cloud);
                        weatherDescription.setText(description);
                    } else if (description.contains("Clear")) {
                        descriptionImageView.setImageResource(R.drawable.ic_sun);
                        weatherDescription.setText(description);
                    } else if (description.contains("Rain")) {
                        descriptionImageView.setImageResource(R.drawable.ic_rain);
                        weatherDescription.setText(description);
                    } else if (description.contains("Snow")) {
                        descriptionImageView.setImageResource(R.drawable.ic_snow);
                        weatherDescription.setText(description);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
    }
}