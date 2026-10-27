package com.example.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Point;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.Marker;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private DataManager dataManager;
    private ProjectItem currentEditingItem;

    // Mapa vectorial MapLibre + OpenFreeMap (datos de OpenStreetMap) — gratis, sin API key
    private MapView mapView;
    private MapLibreMap maplibreMap;
    private Marker mapMarker;
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 200;
    private static final LatLng DEFAULT_MAP_CENTER = new LatLng(8.3084, -73.6166); // Aguachica, Cesar
    private boolean hasSelectedPoint = false;
    private boolean userPickedPoint = false;

    // User-Agent único que identifica la app (lo exige el buscador Nominatim de OpenStreetMap)
    private static final String APP_USER_AGENT =
            "NookFlow/1.0 (Android; +https://github.com/rcastroclavijo597-gif/NookFlow)";

    // Estilos GRATUITOS y SIN API KEY. Toca el título del mapa para cambiar de estilo.
    //  - OpenFreeMap (https://openfreemap.org): mapas vectoriales con datos de OpenStreetMap.
    //  - Satélite: fotos aéreas de Esri World Imagery + nombres de barrios, calles y lugares.
    private static final String STYLE_SATELLITE = "SATELITE";
    private static final String[] MAP_STYLE_URLS = {
            "https://tiles.openfreemap.org/styles/liberty",   // detallado, con edificios en 3D
            STYLE_SATELLITE,                                  // fotos satelitales + nombres
            "https://tiles.openfreemap.org/styles/bright",    // colorido, estilo clásico de OSM
            "https://tiles.openfreemap.org/styles/positron"   // claro y minimalista
    };
    private static final String[] MAP_STYLE_NAMES = {
            "Liberty (detallado)", "Satélite (barrios y calles)", "Bright", "Positron (claro)"
    };

    // Estilo satelital: 3 capas de imágenes (raster) una encima de otra:
    // 1) foto satelital, 2) calles y carreteras, 3) nombres de barrios, sectores y lugares.
    private static final String SATELLITE_STYLE_JSON = "{"
            + "\"version\": 8,"
            + "\"name\": \"Satelite NookFlow\","
            + "\"sources\": {"
            + "  \"satelite\": {\"type\": \"raster\", \"tileSize\": 256, \"maxzoom\": 19,"
            + "    \"tiles\": [\"https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}\"],"
            + "    \"attribution\": \"Imágenes © Esri, Maxar, Earthstar Geographics\"},"
            + "  \"calles\": {\"type\": \"raster\", \"tileSize\": 256, \"maxzoom\": 19,"
            + "    \"tiles\": [\"https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Transportation/MapServer/tile/{z}/{y}/{x}\"],"
            + "    \"attribution\": \"© Esri\"},"
            + "  \"nombres\": {\"type\": \"raster\", \"tileSize\": 256, \"maxzoom\": 19,"
            + "    \"tiles\": [\"https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}\"],"
            + "    \"attribution\": \"© Esri\"}"
            + "},"
            + "\"layers\": ["
            + "  {\"id\": \"satelite\", \"type\": \"raster\", \"source\": \"satelite\"},"
            + "  {\"id\": \"calles\", \"type\": \"raster\", \"source\": \"calles\", \"paint\": {\"raster-opacity\": 0.9}},"
            + "  {\"id\": \"nombres\", \"type\": \"raster\", \"source\": \"nombres\"}"
            + "]}";

    /** Crea el estilo según su tipo: URL de OpenFreeMap o el JSON del satélite. */
    private static Style.Builder buildMapStyle(int index) {
        String style = MAP_STYLE_URLS[index];
        if (STYLE_SATELLITE.equals(style)) {
            return new Style.Builder().fromJson(SATELLITE_STYLE_JSON);
        }
        return new Style.Builder().fromUri(style);
    }

    private int mapStyleIndex = 0;

    private String selectedLocationText = "Ubicación Marcada en Mapa";

    private final String[] monthNames = {
            "Enero 2024", "Febrero 2024", "Marzo 2024", "Abril 2024",
            "Mayo 2024", "Junio 2024", "Julio 2024", "Agosto 2024",
            "Septiembre 2024", "Octubre 2024", "Noviembre 2024", "Diciembre 2024"
    };
    private int currentMonthIndex = 4; // Mayo 2024 por defecto

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar MapLibre ANTES de inflar cualquier layout que tenga el mapa
        MapLibre.getInstance(this);

        dataManager = new DataManager(this);
        setLoginLayout();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showInputDialog(String title, String currentVal, String hint, OnValueEnteredListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        final EditText input = new EditText(this);
        input.setText(currentVal != null ? currentVal : "");
        input.setHint(hint);
        input.setPadding(50, 40, 50, 40);
        builder.setView(input);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String value = input.getText().toString().trim();
            if (!value.isEmpty()) {
                listener.onValueEntered(value);
            } else {
                showToast("Campo sin cambios");
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showColorPickerDialog(int cardId) {
        CardView targetCard = findViewById(cardId);
        String[] colorNames = {"Azul NookFlow", "Verde Esmeralda", "Púrpura", "Rojo Pasión", "Amarillo Sol"};
        int[] colorValues = {
                0xFFD1E4F9, // Azul
                0xFFE1F9E8, // Verde
                0xFFE1D1F9, // Púrpura
                0xFFF9D1D1, // Rojo
                0xFFFFF4D1  // Amarillo
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar Color de Tema");
        builder.setItems(colorNames, (dialog, which) -> {
            if (targetCard != null) {
                targetCard.setCardBackgroundColor(colorValues[which]);
            }
            if (currentEditingItem != null) {
                currentEditingItem.setCardColor(colorValues[which]);
            }
            showToast("Color seleccionado: " + colorNames[which]);
        });
        builder.show();
    }

    private void showSearchDialog() {
        showInputDialog("Buscar en NookFlow", "", "Ingresa nombre del evento o proyecto...", query -> {
            List<ProjectItem> all = dataManager.getAllProjects();
            StringBuilder matches = new StringBuilder();
            for (ProjectItem p : all) {
                if (p.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                    p.getCategory().toLowerCase().contains(query.toLowerCase())) {
                    matches.append("• ").append(p.getTitle()).append(" (").append(p.getCategory().toUpperCase()).append(")\n");
                }
            }
            if (matches.length() > 0) {
                new AlertDialog.Builder(this)
                        .setTitle("Resultados para: \"" + query + "\"")
                        .setMessage(matches.toString())
                        .setPositiveButton("Aceptar", null)
                        .show();
            } else {
                showToast("No se encontraron coincidencias para: " + query);
            }
        });
    }

    // -------------------------------------------------------------
    // MAPA: MapLibre + OpenFreeMap (datos de OpenStreetMap) + BÚSQUEDA NOMINATIM
    // Todo gratis y sin API key.
    // -------------------------------------------------------------
    private void showLocationOptionsDialog(String category) {
        String[] options = {"📍 Buscar / Seleccionar en el Mapa", "✍️ Escribir Nombre / Aula / Edificio"};
        new AlertDialog.Builder(this)
                .setTitle("Configurar Ubicación de " + category.toUpperCase())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openMapLayout(category);
                    } else {
                        showInputDialog("Configurar Ubicación",
                                currentEditingItem != null ? currentEditingItem.getBuilding() : "",
                                "Escribe el edificio, aula o dirección...",
                                val -> {
                                    if (currentEditingItem != null) currentEditingItem.setBuilding(val);
                                    showToast("Ubicación guardada: " + val);
                                });
                    }
                })
                .show();
    }

    private void openMapLayout(String category) {
        releaseMap(); // por si había un mapa anterior abierto
        setContentView(R.layout.map_dialog);
        hasSelectedPoint = false;
        mapMarker = null;
        maplibreMap = null;

        TextView tvCoords = findViewById(R.id.tvSelectedLocationCoords);
        if (tvCoords != null) tvCoords.setText("Cargando mapa...");

        // 1. Crear el mapa (MapView necesita recibir los eventos de ciclo de vida)
        mapView = findViewById(R.id.map);
        mapView.onCreate(null);
        mapView.onStart();
        mapView.onResume();

        mapStyleIndex = getSharedPreferences("nookflow_map", MODE_PRIVATE).getInt("style_v3", 1);
        if (mapStyleIndex < 0 || mapStyleIndex >= MAP_STYLE_URLS.length) mapStyleIndex = 0;

        mapView.getMapAsync(map -> {
            maplibreMap = map;
            map.getUiSettings().setCompassEnabled(true);
            map.getUiSettings().setRotateGesturesEnabled(true);
            map.getUiSettings().setTiltGesturesEnabled(true);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_MAP_CENTER, 15.0));

            map.setStyle(buildMapStyle(mapStyleIndex), style -> {
                TextView tv = findViewById(R.id.tvSelectedLocationCoords);
                if (tv != null && !hasSelectedPoint) tv.setText("Toca el mapa o busca un lugar");
                // 3. Ubicación GPS del usuario (pide permiso si hace falta)
                requestMyLocation();
            });

            // 2. Tocar el mapa para colocar un marcador
            map.addOnMapClickListener(point -> {
                String name = String.format(Locale.getDefault(),
                        "Lat %.5f, Lon %.5f", point.getLatitude(), point.getLongitude());
                setMarkerAndMoveCamera(point, name, false);
                return true;
            });
        });

        // Zoom con la ruedita del mouse (emulador o mouse conectado al celular)
        mapView.setOnGenericMotionListener((v, event) -> {
            if (maplibreMap != null
                    && (event.getSource() & InputDevice.SOURCE_CLASS_POINTER) != 0
                    && event.getAction() == MotionEvent.ACTION_SCROLL) {
                float scroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                if (scroll != 0f) {
                    // Rueda hacia arriba = acercar; hacia abajo = alejar. Zoom centrado en el puntero.
                    Point focus = new Point((int) event.getX(), (int) event.getY());
                    maplibreMap.animateCamera(CameraUpdateFactory.zoomBy(scroll > 0 ? 1.0 : -1.0, focus), 200);
                    return true;
                }
            }
            return false;
        });

        // Botones + / −
        View btnZoomIn = findViewById(R.id.btnZoomIn);
        if (btnZoomIn != null) {
            btnZoomIn.setOnClickListener(v -> {
                if (maplibreMap != null) maplibreMap.animateCamera(CameraUpdateFactory.zoomIn(), 250);
            });
        }
        View btnZoomOut = findViewById(R.id.btnZoomOut);
        if (btnZoomOut != null) {
            btnZoomOut.setOnClickListener(v -> {
                if (maplibreMap != null) maplibreMap.animateCamera(CameraUpdateFactory.zoomOut(), 250);
            });
        }

        // 4. Buscador de lugares
        EditText etSearch = findViewById(R.id.etSearchPlace);
        if (etSearch != null) {
            etSearch.setOnEditorActionListener((v, actionId, event) -> {
                boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN;
                if (actionId == EditorInfo.IME_ACTION_SEARCH || enter) {
                    searchPlace(v.getText().toString().trim());
                    return true;
                }
                return false;
            });
        }
        View btnSearch = findViewById(R.id.btnSearchPlace);
        if (btnSearch != null && etSearch != null) {
            btnSearch.setOnClickListener(v -> searchPlace(etSearch.getText().toString().trim()));
        }

        // Botón visible: alterna entre Satélite y Mapa (Liberty)
        View btnMapStyle = findViewById(R.id.btnMapStyle);
        if (btnMapStyle != null) {
            btnMapStyle.setOnClickListener(v -> toggleSatellite());
        }
        updateStyleButton();

        // Tocar el título recorre TODOS los estilos
        View tvMapTitle = findViewById(R.id.tvMapHeaderTitle);
        if (tvMapTitle != null) {
            tvMapTitle.setOnClickListener(v -> switchMapStyle());
        }

        // 5. Botones Cerrar y Confirmar (vuelven al formulario SIN perder lo editado)
        ImageView btnMapClose = findViewById(R.id.btnMapClose);
        if (btnMapClose != null) {
            btnMapClose.setOnClickListener(v -> {
                releaseMap();
                showEditorLayout(category);
            });
        }

        View.OnClickListener confirmListener = v -> {
            if (!hasSelectedPoint) {
                showToast("Toca el mapa o busca un lugar primero");
                return;
            }
            if (currentEditingItem != null) {
                currentEditingItem.setFocusArea("📍 " + selectedLocationText);
            }
            showToast("📍 Zona de Enfoque fijada: " + selectedLocationText);
            releaseMap();
            showEditorLayout(category);
        };
        Button btnConfirmMapLocation = findViewById(R.id.btnConfirmMapLocation);
        if (btnConfirmMapLocation != null) btnConfirmMapLocation.setOnClickListener(confirmListener);
        View btnMapConfirm = findViewById(R.id.btnMapConfirm);
        if (btnMapConfirm != null) btnMapConfirm.setOnClickListener(confirmListener);
    }

    private void switchMapStyle() {
        applyMapStyle((mapStyleIndex + 1) % MAP_STYLE_URLS.length);
        showToast("🗺️ Estilo: " + MAP_STYLE_NAMES[mapStyleIndex] + " (toca el título para cambiar)");
    }

    /** Alterna entre la vista Satélite (índice 1) y el mapa Liberty (índice 0). */
    private void toggleSatellite() {
        applyMapStyle(mapStyleIndex == 1 ? 0 : 1);
    }

    private void applyMapStyle(int index) {
        if (maplibreMap == null) return;
        mapStyleIndex = index;
        getSharedPreferences("nookflow_map", MODE_PRIVATE).edit().putInt("style_v3", mapStyleIndex).apply();
        maplibreMap.setStyle(buildMapStyle(mapStyleIndex));
        updateStyleButton();
    }

    private void updateStyleButton() {
        TextView btn = findViewById(R.id.btnMapStyle);
        if (btn != null) {
            btn.setText(mapStyleIndex == 1 ? "🗺️ Ver mapa" : "🛰️ Ver satélite");
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /** Pide UNA posición GPS al sistema y pone el marcador "Mi ubicación actual". */
    private void requestMyLocation() {
        if (mapView == null) return;

        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm == null) return;

        String provider;
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
        } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            provider = LocationManager.NETWORK_PROVIDER;
        } else {
            showToast("Activa la ubicación (GPS) del dispositivo");
            return;
        }

        try {
            // Primero la última posición conocida (instantánea), luego una posición fresca
            Location last = lm.getLastKnownLocation(provider);
            if (last != null && !hasSelectedPoint) {
                setMarkerAndMoveCamera(new LatLng(last.getLatitude(), last.getLongitude()),
                        "🎯 Mi ubicación actual", true);
            }
            lm.getCurrentLocation(provider, null, ContextCompat.getMainExecutor(this), location -> {
                if (location != null && mapView != null && maplibreMap != null && !userPickedPoint) {
                    setMarkerAndMoveCamera(new LatLng(location.getLatitude(), location.getLongitude()),
                            "🎯 Mi ubicación actual", true);
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void setMarkerAndMoveCamera(LatLng point, String title, boolean zoomIn) {
        if (maplibreMap == null || point == null) return;

        if (mapMarker != null) {
            maplibreMap.removeMarker(mapMarker);
        }
        mapMarker = maplibreMap.addMarker(new MarkerOptions().position(point).title(title));

        if (zoomIn) {
            maplibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 16.5));
        } else {
            maplibreMap.animateCamera(CameraUpdateFactory.newLatLng(point));
        }

        hasSelectedPoint = true;
        if (!title.startsWith("🎯")) userPickedPoint = true; // el GPS ya no sobrescribe la elección del usuario
        selectedLocationText = title;
        TextView tvCoords = findViewById(R.id.tvSelectedLocationCoords);
        if (tvCoords != null) {
            tvCoords.setText(String.format(Locale.getDefault(), "📍 %s\n(%.5f, %.5f)",
                    title, point.getLatitude(), point.getLongitude()));
        }
    }

    /** Busca un lugar con Nominatim (buscador gratuito de OpenStreetMap) en segundo plano. */
    private void searchPlace(String query) {
        if (query.isEmpty()) {
            showToast("Escribe un lugar para buscar");
            return;
        }
        hideKeyboard();
        showToast("Buscando: " + query + "...");

        networkExecutor.execute(() -> {
            LatLng result = null;
            String name = null;
            HttpURLConnection con = null;
            try {
                String url = "https://nominatim.openstreetmap.org/search?format=json&limit=1"
                        + "&accept-language=es&q=" + URLEncoder.encode(query, "UTF-8");
                con = (HttpURLConnection) new URL(url).openConnection();
                con.setRequestProperty("User-Agent", APP_USER_AGENT); // Nominatim lo exige
                con.setConnectTimeout(10000);
                con.setReadTimeout(10000);

                if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                    }
                    JSONArray arr = new JSONArray(sb.toString());
                    if (arr.length() > 0) {
                        JSONObject o = arr.getJSONObject(0);
                        result = new LatLng(
                                Double.parseDouble(o.getString("lat")),
                                Double.parseDouble(o.getString("lon")));
                        name = shortPlaceName(o.optString("display_name", query));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (con != null) con.disconnect();
            }

            final LatLng finalResult = result;
            final String finalName = name;
            mainHandler.post(() -> {
                if (mapView == null || maplibreMap == null) return; // el usuario ya cerró el mapa
                if (finalResult != null) {
                    setMarkerAndMoveCamera(finalResult, finalName, true);
                } else {
                    showToast("No se encontró \"" + query + "\" (revisa tu conexión a internet)");
                }
            });
        });
    }

    /** Nominatim devuelve nombres muy largos; dejamos solo las 3 primeras partes. */
    private String shortPlaceName(String displayName) {
        String[] parts = displayName.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length && i < 3; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(part);
        }
        return sb.length() > 0 ? sb.toString() : displayName;
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

    private void releaseMap() {
        if (mapView != null) {
            mapView.onPause();
            mapView.onStop();
            mapView.onDestroy();
            mapView = null;
        }
        maplibreMap = null;
        mapMarker = null;
        userPickedPoint = false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean granted = false;
            for (int r : grantResults) {
                if (r == PackageManager.PERMISSION_GRANTED) { granted = true; break; }
            }
            if (granted) {
                showToast("Permiso de ubicación GPS concedido");
                requestMyLocation();
            } else {
                showToast("⚠️ Permiso de ubicación denegado. Puedes tocar el mapa o buscar un lugar.");
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mapView != null) mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        releaseMap();
        networkExecutor.shutdownNow();
        super.onDestroy();
    }

    private interface OnValueEnteredListener {
        void onValueEntered(String value);
    }

    // -------------------------------------------------------------
    // LOGIN SCREEN
    // -------------------------------------------------------------
    private void setLoginLayout() {
        setContentView(R.layout.iniciodesesion);

        EditText emailInput = findViewById(R.id.emailInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnGoToRegister = findViewById(R.id.btnGoToRegister);

        if (emailInput != null && dataManager.hasRegisteredAccount()) {
            emailInput.setText(dataManager.getRegisteredEmail());
        }

        if (btnGoToRegister != null) {
            btnGoToRegister.setOnClickListener(v -> setRegistroLayout());
        }

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                String email = emailInput != null ? emailInput.getText().toString().trim() : "";
                String password = passwordInput != null ? passwordInput.getText().toString().trim() : "";

                if (email.isEmpty() || password.isEmpty()) {
                    showToast("⚠️ Por favor ingresa correo y contraseña.");
                    return;
                }

                if (!dataManager.hasRegisteredAccount()) {
                    showToast("⚠️ No tienes una cuenta creada aún. Por favor regístrate primero.");
                    setRegistroLayout();
                    return;
                }

                if (dataManager.validateCredentials(email, password)) {
                    showToast("¡Bienvenido de nuevo, " + dataManager.getUserName() + "!");
                    setLobbyLayout();
                } else {
                    showToast("❌ Correo o contraseña incorrectos.");
                }
            });
        }

        Button btnGoogle = findViewById(R.id.btnGoogle);
        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(v -> handleSocialAuth("Google"));
        }

        Button btnApple = findViewById(R.id.btnApple);
        if (btnApple != null) {
            btnApple.setOnClickListener(v -> handleSocialAuth("Apple"));
        }

        Button btnFacebook = findViewById(R.id.btnFacebook);
        if (btnFacebook != null) {
            btnFacebook.setOnClickListener(v -> handleSocialAuth("Facebook"));
        }
    }

    // -------------------------------------------------------------
    // REGISTRATION / CREAR CUENTA SCREEN
    // -------------------------------------------------------------
    private void setRegistroLayout() {
        setContentView(R.layout.crearcuenta);

        EditText regNameInput = findViewById(R.id.regNameInput);
        EditText regEmailInput = findViewById(R.id.regEmailInput);
        EditText regPasswordInput = findViewById(R.id.regPasswordInput);
        EditText regConfirmPasswordInput = findViewById(R.id.regConfirmPasswordInput);
        Button btnCreateAccount = findViewById(R.id.btnCreateAccount);
        Button btnGoToLogin = findViewById(R.id.btnGoToLogin);

        if (btnGoToLogin != null) {
            btnGoToLogin.setOnClickListener(v -> setLoginLayout());
        }

        if (btnCreateAccount != null) {
            btnCreateAccount.setOnClickListener(v -> {
                String name = regNameInput != null ? regNameInput.getText().toString().trim() : "";
                String email = regEmailInput != null ? regEmailInput.getText().toString().trim() : "";
                String password = regPasswordInput != null ? regPasswordInput.getText().toString().trim() : "";
                String confirm = regConfirmPasswordInput != null ? regConfirmPasswordInput.getText().toString().trim() : "";

                if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                    showToast("⚠️ Por favor completa todos los campos.");
                    return;
                }

                if (!email.contains("@")) {
                    showToast("⚠️ Ingresa un correo electrónico válido.");
                    return;
                }

                if (password.length() < 4) {
                    showToast("⚠️ La contraseña debe tener al menos 4 caracteres.");
                    return;
                }

                if (!password.equals(confirm)) {
                    showToast("⚠️ Las contraseñas no coinciden.");
                    return;
                }

                dataManager.registerUser(name, email, password);
                showToast("¡Cuenta creada exitosamente para " + name + "! Ahora inicia sesión.");
                setLoginLayout();
            });
        }
    }

    private void handleSocialAuth(String provider) {
        if (!dataManager.hasRegisteredAccount()) {
            showToast("⚠️ No tienes una cuenta en NookFlow. Regístrate primero.");
            setRegistroLayout();
        } else {
            showToast("¡Sesión iniciada con " + provider + " (" + dataManager.getUserName() + ")!");
            setLobbyLayout();
        }
    }

    // -------------------------------------------------------------
    // LOBBY SCREEN & CALENDAR HIGHLIGHTS
    // -------------------------------------------------------------
    private void setLobbyLayout() {
        setContentView(R.layout.loby);

        setupBottomNavigation();

        ImageView btnProfile = findViewById(R.id.btnProfile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> setOpcionesLayout("loby"));
        }

        ImageView btnNotifications = findViewById(R.id.btnNotifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> setNotiLayout("loby"));
        }

        View searchBar = findViewById(R.id.searchBar);
        if (searchBar != null) {
            searchBar.setOnClickListener(v -> showSearchDialog());
        }

        // Month Navigation
        TextView tvMonthYear = findViewById(R.id.tvMonthYear);
        if (tvMonthYear != null) {
            tvMonthYear.setText(monthNames[currentMonthIndex]);
        }

        ImageView btnPrevMonth = findViewById(R.id.btnPrevMonth);
        if (btnPrevMonth != null) {
            btnPrevMonth.setOnClickListener(v -> {
                currentMonthIndex = (currentMonthIndex > 0) ? currentMonthIndex - 1 : monthNames.length - 1;
                if (tvMonthYear != null) tvMonthYear.setText(monthNames[currentMonthIndex]);
                setLobbyLayout();
            });
        }

        ImageView btnNextMonth = findViewById(R.id.btnNextMonth);
        if (btnNextMonth != null) {
            btnNextMonth.setOnClickListener(v -> {
                currentMonthIndex = (currentMonthIndex < monthNames.length - 1) ? currentMonthIndex + 1 : 0;
                if (tvMonthYear != null) tvMonthYear.setText(monthNames[currentMonthIndex]);
                setLobbyLayout();
            });
        }

        setupCalendarDayHighlightsAndClicks();

        // Bind dynamic project data to Lobby Spaces
        bindSpaceCard(R.id.spaceClases, R.id.tvClases, "clases");
        bindSpaceCard(R.id.spacePersonal, R.id.tvPersonal, "personal");
        bindSpaceCard(R.id.spaceTrabajo, R.id.tvTrabajo, "trabajo");
        bindSpaceCard(R.id.spaceGym, R.id.tvGym, "gym");
    }

    private void bindSpaceCard(int cardId, int textId, String category) {
        View card = findViewById(cardId);
        if (card != null) {
            ProjectItem item = dataManager.getProjectByCategory(category);
            TextView tv = findViewById(textId);
            if (tv != null && item != null) {
                tv.setText(item.getTitle());
            }
            card.setOnClickListener(v -> openCategoryLayout(category));
        }
    }

    private GradientDrawable createDayBadge(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(20f);
        drawable.setColor(color);
        drawable.setStroke(2, 0x33000000);
        return drawable;
    }

    private void setupCalendarDayHighlightsAndClicks() {
        View calendarCard = findViewById(R.id.calendarCard);
        if (calendarCard instanceof ViewGroup) {
            findAndHighlightCalendarDays((ViewGroup) calendarCard);
        }
    }

    private void findAndHighlightCalendarDays(ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof ViewGroup) {
                findAndHighlightCalendarDays((ViewGroup) child);
            } else if (child instanceof TextView) {
                TextView tv = (TextView) child;
                String text = tv.getText().toString().trim();

                if (text.matches("\\d+") && tv.getCurrentTextColor() != 0xFFC7C7CC) {
                    int dayNum = Integer.parseInt(text);
                    ProjectItem matchedItem = getProjectForDay(dayNum);

                    if (matchedItem != null) {
                        tv.setBackground(createDayBadge(matchedItem.getCardColor()));
                        tv.setTypeface(null, Typeface.BOLD);
                        tv.setPadding(6, 6, 6, 6);

                        tv.setOnClickListener(v -> {
                            new AlertDialog.Builder(this)
                                    .setTitle("📅 Día " + dayNum + " - " + monthNames[currentMonthIndex])
                                    .setMessage("📌 Actividad Pendiente:\n• " + matchedItem.getTitle() +
                                            "\n⏰ Horario: " + matchedItem.getTime() +
                                            "\n📍 Ubicación: " + matchedItem.getBuilding())
                                    .setPositiveButton("Ver Pendiente ➔", (dialog, which) -> openCategoryLayout(matchedItem.getCategory()))
                                    .setNeutralButton("+ Nueva Actividad", (dialog, which) -> openCategoryLayout("newproyect"))
                                    .setNegativeButton("Cerrar", null)
                                    .show();
                        });
                    } else {
                        tv.setOnClickListener(v -> {
                            new AlertDialog.Builder(this)
                                    .setTitle("📅 Día " + dayNum + " - " + monthNames[currentMonthIndex])
                                    .setMessage("Sin actividades pendientes registradas para este día.")
                                    .setPositiveButton("+ Agregar Pendiente", (dialog, which) -> openCategoryLayout("newproyect"))
                                    .setNegativeButton("Cerrar", null)
                                    .show();
                        });
                    }
                }
            }
        }
    }

    private ProjectItem getProjectForDay(int dayNum) {
        if (dayNum == 1 || dayNum == 15) return dataManager.getProjectByCategory("clases");
        if (dayNum == 5 || dayNum == 20) return dataManager.getProjectByCategory("trabajo");
        if (dayNum == 8 || dayNum == 22) return dataManager.getProjectByCategory("personal");
        if (dayNum == 12 || dayNum == 28) return dataManager.getProjectByCategory("eventos");
        if (dayNum == 10 || dayNum == 25) return dataManager.getProjectByCategory("gym");
        return null;
    }

    // -------------------------------------------------------------
    // INICIO / DASHBOARD SCREEN
    // -------------------------------------------------------------
    private void setInicioLayout() {
        setContentView(R.layout.inicio);

        setupSidebarNavigation();
        setupBottomNavigation();

        ImageView btnProfile = findViewById(R.id.btnProfile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> setOpcionesLayout("inicio"));
        }

        ImageView btnNotifications = findViewById(R.id.btnNotifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> setNotiLayout("inicio"));
        }

        View searchBar = findViewById(R.id.searchBar);
        if (searchBar != null) {
            searchBar.setOnClickListener(v -> showSearchDialog());
        }

        // Bind dashboard cards
        bindDashboardCard(R.id.cardClases, R.id.titleClases, "clases");
        bindDashboardCard(R.id.cardTrabajo, R.id.titleTrabajo, "trabajo");
        bindDashboardCard(R.id.cardPersonal, R.id.titlePersonal, "personal");
        bindDashboardCard(R.id.cardEventos, R.id.titleEventos, "eventos");
        bindDashboardCard(R.id.cardCrear, R.id.titleCrear, "newproyect");
        bindDashboardCard(R.id.cardGym, R.id.titleGym, "gym");
    }

    private void bindDashboardCard(int cardId, int titleId, String category) {
        View card = findViewById(cardId);
        if (card != null) {
            ProjectItem item = dataManager.getProjectByCategory(category);
            TextView tvTitle = findViewById(titleId);
            if (tvTitle != null && item != null) {
                tvTitle.setText(item.getTitle());
            }
            if (card instanceof CardView && item != null) {
                ((CardView) card).setCardBackgroundColor(item.getCardColor());
            }

            card.setOnClickListener(v -> openCategoryLayout(category));
            card.setOnLongClickListener(v -> {
                showProjectMenuDialog(category);
                return true;
            });
        }
    }

    private void showProjectMenuDialog(String category) {
        ProjectItem item = dataManager.getProjectByCategory(category);
        if (item == null) return;

        String[] options = {"✏️ Editar", "📍 Ubicación en Mapa", "✅ Marcar como Completada", "🗑️ Eliminar"};
        new AlertDialog.Builder(this)
                .setTitle(item.getTitle())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openCategoryLayout(category);
                    } else if (which == 1) {
                        currentEditingItem = item;
                        openMapLayout(category);
                    } else if (which == 2) {
                        showToast("\"" + item.getTitle() + "\" marcada como completada");
                    } else if (which == 3) {
                        dataManager.deleteProject(item.getId());
                        showToast("Proyecto eliminado");
                        setInicioLayout();
                    }
                })
                .show();
    }

    private void openCategoryLayout(String category) {
        currentEditingItem = dataManager.getProjectByCategory(category);
        if (currentEditingItem == null) {
            currentEditingItem = new ProjectItem(
                    String.valueOf(System.currentTimeMillis()),
                    category,
                    "Nuevo " + category.toUpperCase(),
                    "Tema Libre",
                    "Ubicación",
                    "Aula / Sala",
                    "Contacto",
                    "Días programados",
                    "09:00 AM - 10:00 AM",
                    true,
                    "Alta",
                    0xFFD1E4F9,
                    "Descripción..."
            );
        }

        showEditorLayout(category);
    }

    /** Muestra el formulario de la categoría usando currentEditingItem tal cual (sin recargarlo). */
    private void showEditorLayout(String category) {
        switch (category.toLowerCase()) {
            case "clases": setClasesLayout(); break;
            case "trabajo": setTrabajosLayout(); break;
            case "personal": setPersonalLayout(); break;
            case "eventos": setEventosLayout(); break;
            case "gym": setGymLayout(); break;
            default: setNewProyectLayout(); break;
        }
    }

    private void setupSidebarNavigation() {
        View navSchool = findViewById(R.id.navIconSchool);
        if (navSchool != null) navSchool.setOnClickListener(v -> openCategoryLayout("clases"));

        View navWork = findViewById(R.id.navIconWork);
        if (navWork != null) navWork.setOnClickListener(v -> openCategoryLayout("trabajo"));

        View navPerson = findViewById(R.id.navIconPerson);
        if (navPerson != null) navPerson.setOnClickListener(v -> openCategoryLayout("personal"));

        View navGym = findViewById(R.id.navIconGym);
        if (navGym != null) navGym.setOnClickListener(v -> openCategoryLayout("gym"));

        View navEvent = findViewById(R.id.navIconEvent);
        if (navEvent != null) navEvent.setOnClickListener(v -> openCategoryLayout("eventos"));

        View navList = findViewById(R.id.navIconList);
        if (navList != null) navList.setOnClickListener(v -> setInicioLayout());

        View btnAdd = findViewById(R.id.sidebarAdd);
        if (btnAdd == null) btnAdd = findViewById(R.id.btnFloatingAddInicio);
        if (btnAdd != null) btnAdd.setOnClickListener(v -> openCategoryLayout("newproyect"));
    }

    private void setupBottomNavigation() {
        View btnNavHome = findViewById(R.id.btnNavHome);
        if (btnNavHome != null) btnNavHome.setOnClickListener(v -> setLobbyLayout());

        View btnFloatingAdd = findViewById(R.id.btnFloatingAdd);
        if (btnFloatingAdd == null) btnFloatingAdd = findViewById(R.id.btnFloatingAddInicio);
        if (btnFloatingAdd != null) btnFloatingAdd.setOnClickListener(v -> openCategoryLayout("newproyect"));

        View btnNavAssignment = findViewById(R.id.btnNavAssignment);
        if (btnNavAssignment != null) btnNavAssignment.setOnClickListener(v -> setInicioLayout());
    }

    // -------------------------------------------------------------
    // CATEGORY FORM EDITORS
    // -------------------------------------------------------------
    private void setupDetailCommonListeners(String categoryName) {
        setupSidebarNavigation();
        setupBottomNavigation();

        View btnClose = findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> setInicioLayout());
        }

        View btnConfirm = findViewById(R.id.btnConfirm);
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                if (currentEditingItem != null) {
                    dataManager.saveProject(currentEditingItem);
                }
                showToast("¡" + categoryName + " guardado con éxito!");
                setInicioLayout();
            });
        }

        // Editable Option Cards
        View cardLocation = findViewById(R.id.cardLocation);
        if (cardLocation != null) {
            cardLocation.setOnClickListener(v -> showInputDialog(
                    "Configurar Edificio / Ubicación Textual",
                    currentEditingItem != null ? currentEditingItem.getBuilding() : "",
                    "Escribe el edificio, aula o dirección...",
                    val -> {
                        if (currentEditingItem != null) currentEditingItem.setBuilding(val);
                        showToast("Ubicación guardada: " + val);
                    }));
        }

        View cardFocusArea = findViewById(R.id.cardFocusArea);
        if (cardFocusArea != null) {
            TextView tvFocusValue = findViewById(R.id.tvFocusAreaValue);
            if (tvFocusValue != null && currentEditingItem != null) {
                tvFocusValue.setText(currentEditingItem.getFocusArea());
            }
            cardFocusArea.setOnClickListener(v -> {
                String cat = currentEditingItem != null ? currentEditingItem.getCategory() : categoryName.toLowerCase();
                openMapLayout(cat);
            });
        }

        View cardOptionsInfo = findViewById(R.id.cardOptionsInfo);
        if (cardOptionsInfo != null) {
            cardOptionsInfo.setOnClickListener(v -> showInputDialog(
                    "Nombre del Proyecto / Opción",
                    currentEditingItem != null ? currentEditingItem.getTitle() : "",
                    "Escribe el nombre...",
                    val -> {
                        if (currentEditingItem != null) currentEditingItem.setTitle(val);
                        showToast("Título actualizado: " + val);
                    }));
        }

        View cardEventInfo = findViewById(R.id.cardEventInfo);
        if (cardEventInfo != null) {
            cardEventInfo.setOnClickListener(v -> showInputDialog(
                    "Detalles / Vestimenta del Evento",
                    currentEditingItem != null ? currentEditingItem.getContact() : "",
                    "Escribe vestimenta o datos...",
                    val -> {
                        if (currentEditingItem != null) currentEditingItem.setContact(val);
                        showToast("Evento actualizado: " + val);
                    }));
        }

        View cardGymInfo = findViewById(R.id.cardGymInfo);
        if (cardGymInfo != null) {
            cardGymInfo.setOnClickListener(v -> showInputDialog(
                    "Rutina / Foco de Gimnasio",
                    currentEditingItem != null ? currentEditingItem.getTopic() : "",
                    "Ej: Pecho y Tríceps...",
                    val -> {
                        if (currentEditingItem != null) currentEditingItem.setTopic(val);
                        showToast("Rutina actualizada: " + val);
                    }));
        }

        View cardPriority = findViewById(R.id.cardPriority);
        if (cardPriority != null) {
            cardPriority.setOnClickListener(v -> {
                String[] priorities = {"Alta 🔴", "Media 🟡", "Baja 🟢"};
                new AlertDialog.Builder(this)
                        .setTitle("Seleccionar Prioridad")
                        .setItems(priorities, (dialog, which) -> {
                            if (currentEditingItem != null) currentEditingItem.setPriority(priorities[which]);
                            showToast("Prioridad: " + priorities[which]);
                        })
                        .show();
            });
        }

        View cardDate = findViewById(R.id.cardDate);
        if (cardDate != null) {
            cardDate.setOnClickListener(v -> showInputDialog(
                    "Configurar Días / Fechas",
                    currentEditingItem != null ? currentEditingItem.getDays() : "",
                    "Ej: Lunes y Miércoles...",
                    val -> {
                        if (currentEditingItem != null) currentEditingItem.setDays(val);
                        showToast("Días actualizados: " + val);
                    }));
        }

        View cardTime = findViewById(R.id.cardTime);
        if (cardTime != null) {
            cardTime.setOnClickListener(v -> showInputDialog(
                    "Configurar Horario",
                    currentEditingItem != null ? currentEditingItem.getTime() : "",
                    "Ej: 08:00 AM - 10:00 AM...",
                    val -> {
                        if (currentEditingItem != null) currentEditingItem.setTime(val);
                        showToast("Horario actualizado: " + val);
                    }));
        }

        View cardReminder = findViewById(R.id.cardReminder);
        if (cardReminder != null) {
            cardReminder.setOnClickListener(v -> {
                String[] reminderTimes = {"5 minutos antes", "15 minutos antes", "1 hora antes", "1 día antes"};
                new AlertDialog.Builder(this)
                        .setTitle("Recordatorio")
                        .setItems(reminderTimes, (dialog, which) -> {
                            if (currentEditingItem != null) currentEditingItem.setReminderEnabled(true);
                            showToast("Recordatorio fijado: " + reminderTimes[which]);
                        })
                        .show();
            });
        }

        View cardColor = findViewById(R.id.cardColor);
        if (cardColor != null) {
            cardColor.setOnClickListener(v -> showColorPickerDialog(R.id.cardColor));
        }

        View cardBottomDesc = findViewById(R.id.cardBottomDesc);
        if (cardBottomDesc != null) {
            cardBottomDesc.setOnClickListener(v -> showInputDialog(
                    "Descripción / Tarea",
                    currentEditingItem != null ? currentEditingItem.getDescription() : "",
                    "Escribe la descripción...",
                    val -> {
                        if (currentEditingItem != null) currentEditingItem.setDescription(val);
                        showToast("Descripción guardada: " + val);
                    }));
        }
    }

    private void setClasesLayout() {
        setContentView(R.layout.clases);
        setupDetailCommonListeners("Clases");
    }

    private void setTrabajosLayout() {
        setContentView(R.layout.trabajos);
        setupDetailCommonListeners("Trabajo");
    }

    private void setPersonalLayout() {
        setContentView(R.layout.personal);
        setupDetailCommonListeners("Personal");
    }

    private void setEventosLayout() {
        setContentView(R.layout.eventos);
        setupDetailCommonListeners("Eventos");
    }

    private void setNewProyectLayout() {
        setContentView(R.layout.newproyect);
        setupDetailCommonListeners("Nuevo Proyecto");
    }

    private void setGymLayout() {
        setContentView(R.layout.gym);
        setupDetailCommonListeners("Gimnasio");
    }

    // -------------------------------------------------------------
    // USER PROFILE & ACCOUNT SETTINGS SCREEN
    // -------------------------------------------------------------
    private void setOpcionesLayout(String from) {
        setContentView(R.layout.opciones);

        TextView tvProfileName = findViewById(R.id.tvProfileName);
        if (tvProfileName != null) {
            tvProfileName.setText(dataManager.getUserName());
        }

        TextView tvProfileEmail = findViewById(R.id.tvProfileEmail);
        if (tvProfileEmail != null) {
            tvProfileEmail.setText(dataManager.getRegisteredEmail().isEmpty() ? "usuario@nookflow.com" : dataManager.getRegisteredEmail());
        }

        View btnClose = findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                if ("loby".equals(from)) setLobbyLayout();
                else setInicioLayout();
            });
        }

        View btnConfirm = findViewById(R.id.btnConfirm);
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                showToast("Perfil guardado correctamente");
                if ("loby".equals(from)) setLobbyLayout();
                else setInicioLayout();
            });
        }

        View optionEditName = findViewById(R.id.optionEditName);
        if (optionEditName != null) {
            optionEditName.setOnClickListener(v -> showInputDialog(
                    "Editar Nombre de Usuario",
                    dataManager.getUserName(),
                    "Escribe tu nombre...",
                    newName -> {
                        dataManager.setUserName(newName);
                        if (tvProfileName != null) tvProfileName.setText(newName);
                        showToast("Nombre actualizado: " + newName);
                    }));
        }

        View optionNotifications = findViewById(R.id.optionNotifications);
        if (optionNotifications != null) {
            optionNotifications.setOnClickListener(v -> setNotiLayout(from));
        }

        View btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("🚪 Cerrar Sesión")
                        .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                        .setPositiveButton("Sí, Cerrar Sesión", (dialog, which) -> {
                            showToast("Sesión cerrada correctamente");
                            setLoginLayout();
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            });
        }
    }

    // -------------------------------------------------------------
    // NOTIFICATIONS CENTER SCREEN
    // -------------------------------------------------------------
    private void setNotiLayout(String from) {
        setContentView(R.layout.noti);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if ("loby".equals(from)) setLobbyLayout();
                else setInicioLayout();
            });
        }

        bindNotiCard(R.id.notiCardClases, R.id.notiTitleClases, R.id.notiDescClases, "clases");
        bindNotiCard(R.id.notiCardTrabajo, R.id.notiTitleTrabajo, R.id.notiDescTrabajo, "trabajo");
        bindNotiCard(R.id.notiCardPersonal, R.id.notiTitlePersonal, R.id.notiDescPersonal, "personal");
        bindNotiCard(R.id.notiCardEventos, R.id.notiTitleEventos, R.id.notiDescEventos, "eventos");
        bindNotiCard(R.id.notiCardGym, R.id.notiTitleGym, R.id.notiDescGym, "gym");
    }

    private void bindNotiCard(int cardId, int titleId, int descId, String category) {
        View card = findViewById(cardId);
        if (card != null) {
            ProjectItem item = dataManager.getProjectByCategory(category);
            TextView tvTitle = findViewById(titleId);
            TextView tvDesc = findViewById(descId);

            if (item != null) {
                if (tvTitle != null) {
                    tvTitle.setText(item.getTitle());
                }
                if (tvDesc != null) {
                    String descText = item.getDescription();
                    if (item.getBuilding() != null && !item.getBuilding().isEmpty()) {
                        descText += " • " + item.getBuilding();
                    }
                    if (item.getTime() != null && !item.getTime().isEmpty()) {
                        descText += " (" + item.getTime() + ")";
                    }
                    tvDesc.setText(descText);
                }
                if (card instanceof CardView) {
                    ((CardView) card).setCardBackgroundColor(item.getCardColor());
                }
            }

            card.setOnClickListener(v -> openCategoryLayout(category));
        }
    }
}
