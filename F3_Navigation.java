package com.example.blindoff;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class F3_Navigation extends Fragment implements OnMapReadyCallback {

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final String DIRECTIONS_API_URL = "https://maps.googleapis.com/maps/api/directions/json";
    private static final String GEOCODING_API_URL = "https://maps.googleapis.com/maps/api/geocode/json";
    private static final String API_KEY = "AIzaSyDHWN_F8t9KwD1jY5odI6NoK2KPLuyjN8o";

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng currentLocation;
    private LatLng destinationLocation;
    private String destinationAddress = "Seoul National University"; // Destination address

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.mode3, container, false);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.googlemap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Check and request location permissions
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, REQUEST_LOCATION_PERMISSION);
        } else {
            getCurrentLocation();
        }

        return view;
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, REQUEST_LOCATION_PERMISSION);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        if (mMap != null) {
                            mMap.addMarker(new MarkerOptions().position(currentLocation).title("현재 위치"));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                            geocodeDestination();
                        }
                    } else {
                        Log.e("Location", "Unable to get current location.");
                    }
                });
    }

    private void geocodeDestination() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                String encodedAddress = URLEncoder.encode(destinationAddress, "UTF-8");
                String url = String.format("%s?address=%s&key=%s",
                        GEOCODING_API_URL,
                        encodedAddress,
                        API_KEY);
                Log.d("GeocodeURL", url);

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    throw new Exception("Unexpected code " + response);
                }

                String responseData = response.body().string();
                Log.d("GeocodeResponse", responseData);

                JsonObject jsonResponse = JsonParser.parseString(responseData).getAsJsonObject();
                JsonArray results = jsonResponse.getAsJsonArray("results");
                if (results.size() > 0) {
                    JsonObject firstResult = results.get(0).getAsJsonObject();
                    JsonObject locationObject = firstResult.getAsJsonObject("geometry").getAsJsonObject("location");
                    double lat = locationObject.get("lat").getAsDouble();
                    double lng = locationObject.get("lng").getAsDouble();
                    destinationLocation = new LatLng(lat, lng);

                    getActivity().runOnUiThread(() -> {
                        if (mMap != null) {
                            mMap.addMarker(new MarkerOptions().position(destinationLocation).title("목적지"));
                            getDirections(); // 목적지 좌표를 구한 후 경로를 가져옵니다.
                        }
                    });
                } else {
                    Log.e("Geocode", "No results found.");
                }
            } catch (Exception e) {
                Log.e("GeocodeError", "Error geocoding destination", e);
            }
        });
    }

    private void getDirections() {
        if (currentLocation == null || destinationLocation == null) {
            Log.e("LocationError", "Current location or destination is null.");
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
//                String origin = String.format("%.6f,%.6f", currentLocation.latitude, currentLocation.longitude);
//                currentLocation = new LatLng(37.457639732471, 126.74906125062);
//                String origin = String.format("%.6f,%.6f", currentLocation.latitude, currentLocation.longitude);
//                String destination = String.format("%.6f,%.6f", destinationLocation.latitude, destinationLocation.longitude);
                String origin = "Seoul Museum of Art"; // 출발지 장소 이름
                String destination = "Seoul National University"; // 목적지 장소 이름

                Log.d("Origin", origin);
                Log.d("Destination", destination);

                // Directions API URL 생성
                String url = String.format("%s?destination=%s&origin=%s&mode=TRANSIT&key=%s",
                        DIRECTIONS_API_URL,
                        URLEncoder.encode(destination, "UTF-8"),
                        URLEncoder.encode(origin, "UTF-8"),
                        API_KEY);
                Log.d("DirectionsURL", url);

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    throw new Exception("Unexpected code " + response);
                }

                String responseData = response.body().string();
                Log.d("DirectionsResponse", responseData);

                JsonObject jsonResponse = JsonParser.parseString(responseData).getAsJsonObject();
                JsonArray routes = jsonResponse.getAsJsonArray("routes");
                if (routes.size() > 0) {
                    JsonObject route = routes.get(0).getAsJsonObject();
                    String polyline = route.getAsJsonObject("overview_polyline").get("points").getAsString();

                    List<LatLng> path = decodePolyline(polyline);

                    // 경로를 지도에 표시
                    getActivity().runOnUiThread(() -> {
                        PolylineOptions polylineOptions = new PolylineOptions().addAll(path)
                                .color(getResources().getColor(android.R.color.holo_red_dark));
                        mMap.addPolyline(polylineOptions);

                        // Mark origin and destination
                        mMap.addMarker(new MarkerOptions().position(currentLocation).title("현재 위치"));
                        System.out.println(currentLocation); //test1
                        mMap.addMarker(new MarkerOptions().position(destinationLocation).title("목적지"));
                        System.out.println(destinationLocation); //test2
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                    });
                } else {
                    Log.e("Directions", "No routes found.");
                }
            } catch (Exception e) {
                Log.e("DirectionsError", "Error getting directions", e);
            }
        });
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> polyline = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            LatLng p = new LatLng((((lat) / 1E5)), (((lng) / 1E5)));
            polyline.add(p);
        }
        return polyline;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Check and request location permissions
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, REQUEST_LOCATION_PERMISSION);
        } else {
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Log.e("Permission", "Location permission denied.");
            }
        }
    }
}