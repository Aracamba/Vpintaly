
package com.aracamba.vpintaly;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import android.app.Activity;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FrameLayout contentFrame;
    private TextView toolbarTitle;
    private EditText documentUrlInput;
    private ActivityResultLauncher<IntentSenderRequest> authorizationLauncher;
    private SharedPreferences preferences;

    private final List<String> logEntries =
            new ArrayList<>();

    private String googleAccessToken;
    private TextView logTextView;
    private TextView homeLogTextView;
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private static final String KEY_VPN_CLIENT = "vpn_client";
    private static final String KEY_AMNEZIA_ENABLED = "amnezia_enabled";
    //private static final String KEY_BYPASS_ENABLED = "bypass_enabled";
    private static final String KEY_AMNEZIA_DEVICE = "amnezia_device";

    private EditText amneziaDeviceInput;
    private static final String PREFS_NAME = "vpintaly_settings";
    private static final String KEY_GOOGLE_CONNECTED = "google_connected";
    private static final String KEY_CONFIGS_JSON = "saved_vpn_configs";
    private static final String KEY_DEVICE_CONFIGS_JSON = "saved_device_configs";
    private static final String KEY_CONFIG_STATUSES = "saved_config_statuses";
    private static final String KEY_CONFIGS_SENT_PREFIX = "sent_vpn_configs_";
    private static final String DOCS_SCOPE = "https://www.googleapis.com/auth/documents.readonly";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        googleAccessToken = preferences.getString(
                "google_access_token", null);

        authorizationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK) {
                        addLog("WARNING", "Подключение Google отменено");
                        return;
                    }

                    try {
                        AuthorizationResult authorizationResult =
                                Identity.getAuthorizationClient(this)
                                        .getAuthorizationResultFromIntent(
                                                result.getData()
                                        );

                        handleAuthorizationSuccess(authorizationResult);

                    } catch (Exception e) {
                        preferences.edit()
                                .putBoolean(KEY_GOOGLE_CONNECTED, false)
                                .apply();

                        addLog("ERROR", "Не удалось завершить авторизацию Google");
                    }
                }
        );

        setContentView(R.layout.activity_main);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(
                        getWindow(),
                        getWindow().getDecorView());

        controller.setAppearanceLightStatusBars(false);

        View mainContent = findViewById(R.id.mainContent);

        ViewCompat.setOnApplyWindowInsetsListener(
                mainContent,
                (view, insets) -> {
                    Insets bars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.statusBars());

                    view.setPadding(
                            view.getPaddingLeft(),
                            bars.top,
                            view.getPaddingRight(),
                            view.getPaddingBottom());

                    return insets;
                }
        );

        ViewCompat.requestApplyInsets(mainContent);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        contentFrame = findViewById(R.id.contentFrame);
        toolbarTitle = findViewById(R.id.toolbarTitle);

        ImageButton menuButton = findViewById(R.id.menuButton);

        menuButton.setOnClickListener(v ->
                drawerLayout.openDrawer(Gravity.LEFT));

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                showHome();
            } else if (id == R.id.nav_sync) {
                showSync();
            } else if (id == R.id.nav_sett) {
                showSettings();
            } else if (id == R.id.nav_log) {
                showLog();
            } else if (id == R.id.nav_sup) {
                showSupport();
            } else if (id == R.id.nav_about) {
                showAbout();
            } else if (id == R.id.nav_test) {
                showSpeedTest();
            }

            drawerLayout.closeDrawer(Gravity.LEFT);
            return true;
            });

        navigationView.setCheckedItem(R.id.nav_home);
        checkLatestVersion();
        showHome();
        addLog("INFO", "Приложение запущено.");
    }

    @Override
    protected void onDestroy() {
        WebView myWebView = new WebView(this);
        if (myWebView != null) {
            if (myWebView.getParent() instanceof ViewGroup) {
                ((ViewGroup) myWebView.getParent()).removeView(myWebView);
            }
            myWebView.stopLoading();
            myWebView.clearHistory();
            myWebView.destroy();
        }
        myWebView = null;
        super.onDestroy();
    }

    private void showHome() {
        toolbarTitle.setText("Главная");
        contentFrame.removeAllViews();

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(getColor(R.color.vp_bg));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(24), dp(20), dp(28));

        scrollView.addView(layout);

        TextView heading = new TextView(this);
        heading.setText("Добро пожаловать в Vpintaly");
        heading.setTextColor(getColor(R.color.vp_text));
        heading.setTextSize(25);
        heading.setTypeface(null, Typeface.BOLD);
        layout.addView(heading);

        TextView subtitle1 = new TextView(this);
        subtitle1.setText("");
        subtitle1.setTextColor(getColor(R.color.vp_text_secondary));
        subtitle1.setTextSize(8);
        subtitle1.setPadding(0, dp(6), 0, dp(8));
        layout.addView(subtitle1);

        TextView subtitle = new TextView(this);
        subtitle.setText("");
        subtitle.setTextColor(getColor(R.color.vp_text_secondary));
        subtitle.setTextSize(8);
        subtitle.setPadding(0, dp(10), 0, dp(8));
        layout.addView(subtitle);

        TextView sectionLabel = new TextView(this);
        sectionLabel.setText("ССЫЛКА НА GOOGLE DOCS");
        sectionLabel.setTextColor(getColor(R.color.vp_text));
        sectionLabel.setTextSize(13);
        sectionLabel.setTypeface(null, Typeface.BOLD);
        layout.addView(sectionLabel);

        documentUrlInput = new EditText(this);
        documentUrlInput.setSingleLine(true);
        documentUrlInput.setTextSize(15);
        documentUrlInput.setTextColor(getColor(R.color.vp_text));
        documentUrlInput.setHintTextColor(
                getColor(R.color.vp_text_secondary));
        documentUrlInput.setHint("https://docs.google.com/document/d/...");
        documentUrlInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_URI);
        documentUrlInput.setPadding(
                dp(14), dp(14), dp(14), dp(14));
        documentUrlInput.setBackground(makeBackground(
                getColor(R.color.vp_surface),
                getColor(R.color.vp_border),
                12));

        LinearLayout.LayoutParams inputParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        inputParams.topMargin = dp(10);
        String savedUrl = loadDocumentUrl();

        if (!savedUrl.isEmpty()) {
            documentUrlInput.setText(savedUrl);
        }
        layout.addView(documentUrlInput, inputParams);



        Button sendConfigsButton = new Button(this);
        sendConfigsButton.setText("Передать конфигурации в VPN");
        sendConfigsButton.setAllCaps(false);
        sendConfigsButton.setTextSize(16);
        sendConfigsButton.setTextColor(Color.WHITE);
        sendConfigsButton.setBackground(makeBackground(
                getColor(R.color.vp_accent),
                getColor(R.color.vp_accent),
                12));

        LinearLayout.LayoutParams sendParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(54));
        sendParams.topMargin = dp(10);

        layout.addView(sendConfigsButton, sendParams);

        sendConfigsButton.setOnClickListener(v -> validateDocumentUrl());

        TextView journalHeading = new TextView(this);
        journalHeading.setText("Журнал");
        journalHeading.setTextColor(getColor(R.color.vp_text));
        journalHeading.setTextSize(18);
        journalHeading.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams journalHeadingParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        journalHeadingParams.topMargin = dp(28);
        layout.addView(journalHeading, journalHeadingParams);

        TextView homeLogText = new TextView(this);
        homeLogText.setTextColor(getColor(R.color.vp_text_secondary));
        homeLogText.setTextSize(13);
        homeLogText.setPadding(dp(14), dp(14), dp(14), dp(14));
        homeLogText.setBackground(makeBackground(
                getColor(R.color.vp_surface),
                getColor(R.color.vp_border),
                12));

        LinearLayout.LayoutParams homeLogParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        homeLogParams.topMargin = dp(12);
        layout.addView(homeLogText, homeLogParams);
        homeLogTextView = homeLogText;
        updateHomeLog(homeLogTextView);

        contentFrame.addView(scrollView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private void showSync(){
        toolbarTitle.setText("Конфигурации");
        contentFrame.removeAllViews();

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(getColor(R.color.vp_bg));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(20), dp(16), dp(24));
        scrollView.addView(layout);

        TextView subtitle = new TextView(this);
        subtitle.setText(
                "Сохранённые конфигурации из Google Docs");
        subtitle.setTextColor(getColor(R.color.vp_text_secondary));
        subtitle.setTextSize(14);
        subtitle.setPadding(0, dp(6), 0, dp(16));
        layout.addView(subtitle);

        LinearLayout table = new LinearLayout(this);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setBackground(makeBackground(
                getColor(R.color.vp_surface),
                getColor(R.color.vp_border),
                12));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(dp(10), dp(12), dp(10), dp(12));

        TextView typeHeader = new TextView(this);
        typeHeader.setText("Тип");
        typeHeader.setTextColor(getColor(R.color.vp_text));
        typeHeader.setTypeface(null, Typeface.BOLD);
        typeHeader.setWidth(dp(70));
        header.addView(typeHeader);

        TextView linkHeader = new TextView(this);
        linkHeader.setText("Конфигурация");
        linkHeader.setTextColor(getColor(R.color.vp_text));
        linkHeader.setTypeface(null, Typeface.BOLD);
        header.addView(linkHeader,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f));

        table.addView(header);

        List<String> configs = loadSavedConfigs();
        String deviceJson = preferences.getString(
                KEY_DEVICE_CONFIGS_JSON, "{}");

        org.json.JSONObject deviceConfigs;

        try {
            deviceConfigs = new JSONObject(deviceJson);
        } catch (JSONException e) {
            deviceConfigs = new JSONObject();
        }

        if (configs.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(
                    "Пока конфигураций нет.\n"
                            + "Проверь статус конфигураций на главной странице.");
            empty.setTextColor(getColor(R.color.vp_text_secondary));
            empty.setTextSize(14);
            empty.setPadding(dp(12), dp(16), dp(12), dp(16));
            table.addView(empty);
        } else {
            for (String config : configs) {
                addConfigRow(table, config);
            }
        }

        layout.addView(table);

        contentFrame.addView(
                scrollView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private void addConfigRow(LinearLayout table, String config) {
        View divider = new View(this);
        divider.setBackgroundColor(getColor(R.color.vp_border));
        table.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(10), dp(12), dp(10), dp(12));
        row.setGravity(Gravity.CENTER_VERTICAL);

        String type = getConfigType(config);
        String deviceNumber = "Отсутствует";

        String deviceJson = preferences.getString(
                KEY_DEVICE_CONFIGS_JSON, "{}");

        try {
            JSONObject deviceConfigs =
                    new JSONObject(deviceJson);

            Iterator<String> keys = deviceConfigs.keys();

            while (keys.hasNext()) {
                String key = keys.next();

                if (config.equals(deviceConfigs.optString(key))) {
                    deviceNumber = key;
                    break;
                }
            }
        } catch (JSONException ignored) {
        }

        TextView typeView = new TextView(this);
        typeView.setText(type);
        typeView.setTextColor(getColor(R.color.vp_accent));
        typeView.setTypeface(null, Typeface.BOLD);
        typeView.setWidth(dp(70));
        typeView.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);

        typeView.setOnClickListener(v -> {
            try {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Copied Text", config);

                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getApplicationContext(), "Текст скопирован в буфер", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception x){
                Toast.makeText(getApplicationContext(), "Не удалось скопировать текст в буффер обмена.", Toast.LENGTH_SHORT).show();
                addLog("ERROR", "Ошибка копирования в буффер обмена: " + x);
            }
        });

        row.addView(typeView);

        TextView linkView = new TextView(this);
        linkView.setText(config);
        String statusesJson = preferences.getString(
                KEY_CONFIG_STATUSES,
                "{}");

        String status = "Без изменений";

        try {
            JSONObject statuses =
                    new JSONObject(statusesJson);
            status = statuses.optString(config, "Без изменений");
        } catch (JSONException ignored) {
        }

        linkView.setText(
                "Устройство: " + deviceNumber
                        + "\n\n" + config
                        + "\n\nСтатус: " + status);
        linkView.setTextColor(getColor(R.color.vp_text));
        linkView.setTextSize(12);
        linkView.setTextIsSelectable(true);
        row.addView(linkView,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f));

        table.addView(row);
    }

    private String getConfigType(String config) {
        int separator = config.indexOf("://");

        if (separator <= 0) {
            return "ERR";
        }

        if("HTTPS".equals(config.substring(0, separator)
                .toUpperCase(Locale.ROOT))){
            return "SUB";
        }

        return config.substring(0, separator)
                .toUpperCase(Locale.ROOT);
    }
    private Set<String> getSentConfigs(String clientId) {
        return new HashSet<>(
                preferences.getStringSet(
                        KEY_CONFIGS_SENT_PREFIX + clientId,
                        new HashSet<>()));
    }

    private List<String> loadSavedConfigs() {
        List<String> configs = new ArrayList<>();

        String json = preferences.getString(KEY_CONFIGS_JSON, "[]");

        try {
            JSONArray array = new JSONArray(json);

            for (int i = 0; i < array.length(); i++) {
                String config = array.optString(i, "");

                if (!config.isEmpty() && !configs.contains(config)) {
                    configs.add(config);
                }
            }
        } catch (JSONException e) {
            addLog("ERROR", "Не удалось прочитать сохранённые конфигурации.");
        }

        return configs;
    }
    private String loadAmneziaConfig(String deviceNumber) {
        String json = preferences.getString(KEY_DEVICE_CONFIGS_JSON, "{}");

        try {
            JSONObject object = new JSONObject(json);
            return object.optString(deviceNumber, "");
        } catch (JSONException e) {
            addLog("ERROR", "Не удалось прочитать конфигурацию AmneziaVPN.");
            return "";
        }
    }
    private List<String> loadV2Configs() {
        List<String> result = new ArrayList<>();

        for (String config : loadSavedConfigs()) {
            String lower = config.trim().toLowerCase(Locale.ROOT);

            if (lower.startsWith("vless://")
                    || lower.startsWith("vmess://")
                    || lower.startsWith("https://")
                    || lower.startsWith("ss://")
                    || lower.startsWith("trojan://")) {
                result.add(config);
            }
        }

        return result;
    }
    private List<String> loadUnsentV2Configs() {
        List<String> unsent = new ArrayList<>();

        //boolean bypassEnabled = preferences.getBoolean(
        //        KEY_BYPASS_ENABLED, false);

        Set<String> sent = getSentConfigs(
                getSelectedVpnClient()
        );

        for (String config : loadV2Configs()) {
            if (!sent.contains(config)
                    //|| bypassEnabled
            ) {
                unsent.add(config);
            }
        }

        return unsent;
    }
    private List<String> loadUnsentAmneziaConfigs(
            String deviceNumber) {

        List<String> result = new ArrayList<>();

        //boolean bypassEnabled = preferences.getBoolean(
        //        KEY_BYPASS_ENABLED, false);

        String config = loadAmneziaConfig(deviceNumber);

        if (config == null || config.trim().isEmpty()) {
            return result;
        }

        Set<String> sent = getSentConfigs("AmneziaVPN");

        if (!sent.contains(config)
                //|| bypassEnabled
        ) {
            result.add(config);
        }

        return result;
    }
    private String getSelectedVpnClient() {
        return preferences.getString(KEY_VPN_CLIENT, "NONE");
    }
    private void markConfigsAsSent(
            String clientId,
            List<String> configs) {

        Set<String> sent = getSentConfigs(clientId);
        sent.addAll(configs);

        preferences.edit()
                .putStringSet(
                        KEY_CONFIGS_SENT_PREFIX + clientId,
                        sent).apply();
    }

    private void sendToXray(List<String> configs, String selectedClient){
        if (configs.isEmpty()) {
            Toast.makeText(this,
                    "Нет новых или изменённых конфигураций для передачи",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> sentNow = new ArrayList<>();
        String url = "";
        if ("V2RayTun".equals(selectedClient)) url = "v2raytun://import/";
        if ("V2Box".equals(selectedClient)) url = "v2box://install-sub?url=";
        if ("V2RayNG".equals(selectedClient)) url = "v2rayng://install-config?url=";
        if ("INCY".equals(selectedClient)) url = "incy://add/";

        for (String config : configs){
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url + config));

                startActivity(intent);
                sentNow.add(config);
            } catch(ActivityNotFoundException e){
                Toast.makeText(this,
                        "Выбранный Xray клиент не найден на устройстве",
                        Toast.LENGTH_LONG).show();
                break;
            }
        }

        markConfigsAsSent(selectedClient, sentNow);
    }
    private void sendToAmnezia(String config) {
        if (config == null || config.trim().isEmpty()) {
            Toast.makeText(this,
                    "Нет новых или измененных конфигураций AmneziaVPN для передачи",
                    Toast.LENGTH_LONG).show();
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(config));

            intent.setPackage("org.amnezia.vpn");
            startActivity(intent);

            markConfigsAsSent("AmneziaVPN",
                    Collections.singletonList(config));

        } catch (ActivityNotFoundException e) {
            Toast.makeText(this,
                    "AmneziaVPN не найден или не поддерживает импорт",
                    Toast.LENGTH_LONG).show();
        }
    }
    private void clearAllAmneziaConfigs() {
        preferences.edit()
                .remove(KEY_CONFIGS_SENT_PREFIX + "AmneziaVPN")
                .apply();

        Toast.makeText(this,
                "Устройство AmneziaVPN сброшено",
                Toast.LENGTH_LONG).show();

        addLog("INFO", "Устройство AmneziaVPN сброшено");
    }
    private void sendConfigsToVpnClient() {
        String selectedClient = getSelectedVpnClient();

        boolean amneziaEnabled = preferences.getBoolean(
                KEY_AMNEZIA_ENABLED, false);

        if (!"NONE".equals(selectedClient)) {
            List<String> configs = loadUnsentV2Configs();
            sendToXray(configs, selectedClient);
        }

        if (amneziaEnabled) {
            String deviceNumber = preferences.getString(
                    KEY_AMNEZIA_DEVICE, "").trim();

            if (deviceNumber.isEmpty()) {
                Toast.makeText(this,
                        "Укажи номер устройства для AmneziaVPN",
                        Toast.LENGTH_LONG).show();
                return;
            }

            String config = loadAmneziaConfig(deviceNumber);

            if (config.isEmpty()) {
                Toast.makeText(this,
                        "Конфигурация для этого устройства не найдена",
                        Toast.LENGTH_LONG).show();
                return;
            }

            List<String> unsentAmnezia =
                    loadUnsentAmneziaConfigs(deviceNumber);

            if (!unsentAmnezia.isEmpty()) {
                sendToAmnezia(unsentAmnezia.get(0));
            } else {
                Toast.makeText(this,
                        "Новых конфигураций AmneziaVPN нет",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveConfigs(List<String> configs) {
        JSONArray array = new JSONArray();

        for (String config : configs) {
            array.put(config);
        }

        preferences.edit()
                .putString(KEY_CONFIGS_JSON, array.toString())
                .apply();
    }

    private void updateHomeLog(TextView target) {
        int count = Math.min(5, logEntries.size());

        if (count == 0) {
            target.setText("Пока событий нет.");
            return;
        }

        StringBuilder text = new StringBuilder();

        for (int i = logEntries.size() - count;
             i < logEntries.size(); i++) {
            if (text.length() > 0) {
                text.append("\n\n");
            }
            text.append(logEntries.get(i));
        }

        target.setText(text.toString());
    }

    private void addLog(String level, String message) {
        String time = new SimpleDateFormat(
                "HH:mm:ss",
                Locale.getDefault()
        ).format(new Date());

        String entry = time + " [" + level + "] " + message;
        logEntries.add(entry);

        // Если журнал сейчас открыт, сразу обновляем экран.
        if (logTextView != null
                && "Журнал".contentEquals(toolbarTitle.getText())) {
            logTextView.setText(
                    String.join("\n\n", logEntries));
        }
        if (homeLogTextView != null) {
            updateHomeLog(homeLogTextView);
        }
    }

    private void showLog() {
        toolbarTitle.setText("Журнал");
        contentFrame.removeAllViews();

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(getColor(R.color.vp_bg));

        logTextView = new TextView(this);
        logTextView.setTextColor(getColor(R.color.vp_text));
        logTextView.setTextSize(14);
        logTextView.setPadding(dp(20), dp(20), dp(20), dp(20));
        logTextView.setTextIsSelectable(true);

        if (logEntries.isEmpty()) {
            logTextView.setText("Журнал пока пуст.");
        } else {
            logTextView.setText(
                    String.join("\n\n", logEntries)
            );
        }

        scrollView.addView(logTextView);

        contentFrame.addView(
                scrollView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );
    }

    private void showSupport(){
        toolbarTitle.setText("Поддержка");
        contentFrame.removeAllViews();

        ScrollView scrollView =
                new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(getColor(R.color.vp_bg));

        LinearLayout layout =
                new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(24), dp(20), dp(28));

        scrollView.addView(layout);

        TextView heading = new TextView(this);
        heading.setText("Поддержка Vpintaly");
        heading.setTextColor(getColor(R.color.vp_text));
        heading.setTextSize(25);
        heading.setTypeface(null, Typeface.BOLD);
        layout.addView(heading);

        TextView description = new TextView(this);
        description.setText(
                "Если нужна помощь или возник вопрос по приложению, " +
                        "открой документ поддержки по ссылке ниже.");
        description.setTextColor(getColor(R.color.vp_text_secondary));
        description.setTextSize(15);
        description.setPadding(0, dp(10), 0, dp(24));
        layout.addView(description);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(makeBackground(
                getColor(R.color.vp_surface),
                getColor(R.color.vp_border),
                12));

        TextView linkTitle = new TextView(this);
        linkTitle.setText("Документ поддержки");
        linkTitle.setTextColor(getColor(R.color.vp_text));
        linkTitle.setTextSize(18);
        linkTitle.setTypeface(null, Typeface.BOLD);
        card.addView(linkTitle);

        TextView link = new TextView(this);
        link.setText("Открыть документ →");
        link.setTextColor(getColor(R.color.vp_accent));
        link.setTextSize(16);
        link.setPadding(0, dp(12), 0, 0);
        link.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);

        String documentUrl = preferences.getString("saved_document_url", "");

        if (documentUrl != null &&
                documentUrl.trim().startsWith("https://docs.google.com/document/d/")) {
            link.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(documentUrl.trim()));
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(
                            this,
                            "Не удалось открыть документ, возможно введена нерабочая ссылка",
                            Toast.LENGTH_LONG).show();
                }
            });
        } else {
            link.setText("Сначала укажи ссылку на главной странице");
            link.setTextColor(getColor(R.color.vp_text_secondary));
        }

        card.addView(link);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.addView(card, cardParams);

        contentFrame.addView(scrollView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private void showSettings(){
        toolbarTitle.setText("Настройки");
        contentFrame.removeAllViews();

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(getColor(R.color.vp_bg));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(24), dp(20), dp(28));

        scrollView.addView(layout);

        TextView heading = new TextView(this);
        heading.setText("Настройки Vpintaya");
        heading.setTextColor(getColor(R.color.vp_text));
        heading.setTextSize(24);
        heading.setTypeface(null, Typeface.BOLD);
        layout.addView(heading);

        TextView subtitle = new TextView(this);
        subtitle.setText("Настройте синхронизацию под ваши нужды");
        subtitle.setTextColor(getColor(R.color.vp_text_secondary));
        subtitle.setTextSize(14);
        subtitle.setPadding(0, dp(6), 0, dp(16));
        layout.addView(subtitle);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(makeBackground(
                getColor(R.color.vp_surface),
                getColor(R.color.vp_border),
                12));

        TextView linkTitle = new TextView(this);
        linkTitle.setText("Если вы ещё не настроили Раздельное Туннелирование в приложении, ОБЯЗАТЕЛЬНО настройте его по гайду ниже");
        linkTitle.setTextColor(getColor(R.color.vp_text));
        linkTitle.setTextSize(18);
        linkTitle.setTypeface(null, Typeface.BOLD);
        card.addView(linkTitle);

        TextView link = new TextView(this);
        link.setText("Настройка р.т. для приложений/сайтов");
        link.setTextColor(getColor(R.color.vp_accent));
        link.setTextSize(16);
        link.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
        link.setPadding(0, dp(12), 0, 0);

        link.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                                        "https://github.com/Aracamba/Vpintaly/blob/master/split_tunneling.md"));
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(
                        this,
                        "Не удалось открыть документ",
                        Toast.LENGTH_LONG).show();
            }
        });

        card.addView(link);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.addView(card, cardParams);

        //CheckBox bypassCheckBox = new CheckBox(this);
        //bypassCheckBox.setText("Принудительная передача конфигураций");
        //bypassCheckBox.setTextColor(getColor(R.color.vp_text));

        //boolean bypassEnabled = preferences.getBoolean(KEY_BYPASS_ENABLED, false);
        //bypassCheckBox.setChecked(bypassEnabled);
        //layout.addView(bypassCheckBox);

        //bypassCheckBox.setOnCheckedChangeListener((button, checked) -> {
        //    preferences.edit()
        //            .putBoolean(KEY_BYPASS_ENABLED, checked)
        //            .apply();
        //    if (checked) {
        //        Toast.makeText(this, "Эта функция отключает автопроверку статуса конфигов", Toast.LENGTH_SHORT).show();
        //        Toast.makeText(this, "Ручная проверка доступна только после передачи конфигураций", Toast.LENGTH_SHORT).show();
        //        Toast.makeText(this, "Если вам это не требуется - отключите данную функцию", Toast.LENGTH_SHORT).show();
        //    }
        //});

        // Настройки VPN-клиента
        TextView vpnHeading = new TextView(this);
        vpnHeading.setText("\nVPN-клиент");
        vpnHeading.setTextColor(getColor(R.color.vp_text));
        vpnHeading.setTextSize(18);
        vpnHeading.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams vpnHeadingParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        vpnHeadingParams.topMargin = dp(24);
        layout.addView(vpnHeading, vpnHeadingParams);

        RadioGroup vpnGroup = new RadioGroup(this);
        vpnGroup.setOrientation(RadioGroup.VERTICAL);

        RadioButton Nov2Button = new RadioButton(this);
        Nov2Button.setText("Отсутствует клиент Xray");
        Nov2Button.setTextColor(getColor(R.color.vp_text));
        Nov2Button.setId(View.generateViewId());

        RadioButton v2rayButton = new RadioButton(this);
        v2rayButton.setText("V2RayTun");
        v2rayButton.setTextColor(getColor(R.color.vp_text));
        v2rayButton.setId(View.generateViewId());

        RadioButton IncyButton = new RadioButton(this);
        IncyButton.setText("INCY");
        IncyButton.setTextColor(getColor(R.color.vp_text));
        IncyButton.setId(View.generateViewId());

        RadioButton v2boxButton = new RadioButton(this);
        v2boxButton.setText("V2Box");
        v2boxButton.setTextColor(getColor(R.color.vp_text));
        v2boxButton.setId(View.generateViewId());

        RadioButton v2rayngButton = new RadioButton(this);
        v2rayngButton.setText("V2RayNG");
        v2rayngButton.setTextColor(getColor(R.color.vp_text));
        v2rayngButton.setId(View.generateViewId());

        vpnGroup.addView(Nov2Button);
        vpnGroup.addView(v2rayButton);
        vpnGroup.addView(IncyButton);
        vpnGroup.addView(v2boxButton);
        vpnGroup.addView(v2rayngButton);

        String savedClient = preferences.getString(
                KEY_VPN_CLIENT, "NONE");

        if ("INCY".equals(savedClient)) {
            vpnGroup.check(IncyButton.getId());
        } else if ("V2Box".equals(savedClient)) {
            vpnGroup.check(v2boxButton.getId());
        } else if ("V2RayTun".equals(savedClient)) {
            vpnGroup.check(v2rayButton.getId());
        } else if ("V2RayNG".equals(savedClient)) {
            vpnGroup.check(v2rayngButton.getId());
        } else {
            vpnGroup.check(Nov2Button.getId());
        }

        vpnGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String client;

            if (checkedId == IncyButton.getId()) {
                client = "INCY";
            } else if (checkedId == v2rayButton.getId()) {
                client = "V2RayTun";
            } else if (checkedId == v2boxButton.getId()) {
                client = "V2Box";
            } else if (checkedId == v2rayngButton.getId()) {
                client = "V2RayNG";
            } else {
                client = "NONE";
            }

            preferences.edit()
                    .putString(KEY_VPN_CLIENT, client)
                    .apply();
        });

        layout.addView(vpnGroup);

// Независимый переключатель AmneziaVPN
        CheckBox amneziaCheckBox = new CheckBox(this);
        amneziaCheckBox.setText("Использовать AmneziaVPN");
        amneziaCheckBox.setTextColor(getColor(R.color.vp_text));

        boolean amneziaEnabled = preferences.getBoolean(
                KEY_AMNEZIA_ENABLED, false);
        amneziaCheckBox.setChecked(amneziaEnabled);

        layout.addView(amneziaCheckBox);

        Button clearAmneziaButton = new Button(this);
        clearAmneziaButton.setText("Сбросить устройство AmneziaVPN");

        clearAmneziaButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Сброс утройства AmneziaVPN")
                    .setMessage("Сбросить устройство AmneziaVPN?")
                    .setPositiveButton("Сбросить", (dialog, which) ->
                            clearAllAmneziaConfigs())
                    .setNegativeButton("Отмена", null)
                    .show();
        });
        LinearLayout.LayoutParams clearAmneziaParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        clearAmneziaParams.topMargin = 12;

        layout.addView(clearAmneziaButton, clearAmneziaParams);

        clearAmneziaButton.setVisibility(
                amneziaEnabled ? View.VISIBLE : View.GONE);

// Поле номера устройства
        amneziaDeviceInput = new EditText(this);
        amneziaDeviceInput.setSingleLine(true);
        amneziaDeviceInput.setTextSize(15);
        amneziaDeviceInput.setTextColor(getColor(R.color.vp_text));
        amneziaDeviceInput.setHint("Поле ввода номера устройства. Например: 2");
        amneziaDeviceInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        amneziaDeviceInput.setPadding(
                dp(14), dp(12), dp(14), dp(12));
        amneziaDeviceInput.setBackground(makeBackground(
                getColor(R.color.vp_surface),
                getColor(R.color.vp_border),
                12));

        amneziaDeviceInput.setText(
                preferences.getString(KEY_AMNEZIA_DEVICE, ""));

        LinearLayout.LayoutParams deviceParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        deviceParams.topMargin = dp(8);

        layout.addView(amneziaDeviceInput, deviceParams);

        amneziaDeviceInput.setVisibility(
                amneziaEnabled ? View.VISIBLE : View.GONE);

        amneziaCheckBox.setOnCheckedChangeListener((button, checked) -> {
            preferences.edit()
                    .putBoolean(KEY_AMNEZIA_ENABLED, checked)
                    .apply();

            amneziaDeviceInput.setVisibility(
                    checked ? View.VISIBLE : View.GONE);
            clearAmneziaButton.setVisibility(
                    checked ? View.VISIBLE : View.GONE);
        });

        amneziaDeviceInput.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                preferences.edit()
                        .putString(
                                KEY_AMNEZIA_DEVICE,
                                amneziaDeviceInput.getText().toString().trim()
                        ).apply();
            }
        });

        contentFrame.addView(scrollView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private void showSpeedTest(){
        toolbarTitle.setText("SpeedTest");
        contentFrame.removeAllViews();

        WebView webView = new WebView(this);
        WebSettings webSettings = webView.getSettings();
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptEnabled(true);
        webView.loadUrl("https://speed.cloudflare.com/ru-ru");
        Toast.makeText(this, "Cloudflare стабильно срезает 50-60% трафика.", Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "Старайтесь смотреть на качество соединения, а не на скорость.", Toast.LENGTH_SHORT).show();

        contentFrame.addView(webView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
    }
    private void showAbout(){
        toolbarTitle.setText("О проекте");
        contentFrame.removeAllViews();

        String versionName = "";

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(getColor(R.color.vp_bg));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(20), dp(16), dp(24));
        scrollView.addView(layout);

        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);

            if (info.versionName != null) {
                versionName = info.versionName;
            }
        } catch (Exception e) {
        }
        String googleStatus = isGoogleConnected()
                ? "Подключение сохранено на устройстве"
                : "Не подключено";

        TextView textView = new TextView(this);
        textView.setText("Vpintaly\n\n"
                + "Версия приложения: " + versionName + "\n\n"
                + "Язык разработки: Java\n"
                + "Версия Java: 21\n"
                + "Минимальная версия Android: 8.0\n\n"
                + "Статус Google: " + googleStatus + "\n\n"
                + "Доступ к Google Docs предоставляется через авторизацию Google.\n\n"
                + "Бесопасен! Зуб дай!");
        textView.setTextColor(getColor(R.color.vp_text_secondary));
        textView.setTextSize(17);
        layout.addView(textView);

        TextView repos = new TextView(this);
        repos.setText("\n https://github.com/Aracamba/Vpintaly");
        repos.setTextColor(getColor(R.color.vp_accent));
        repos.setTextSize(17);
        repos.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);

        repos.setOnClickListener(v -> {
            try{
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Aracamba/Vpintaly"));
                startActivity(intent);
            } catch (ActivityNotFoundException e){
            }
        });

        layout.addView(repos);

        contentFrame.addView(scrollView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private void validateDocumentUrl() {
        String value = documentUrlInput.getText().toString().trim();

        if (value.isEmpty()) {
            Toast.makeText(
                    this,
                    "Вставь ссылку на Google Docs",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if("NONE".equals(getSelectedVpnClient()) && !preferences.getBoolean(KEY_AMNEZIA_ENABLED, false)){
            Toast.makeText(this, "Не выбрано ни одного VPN клиента", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = Uri.parse(value);
        String host = uri.getHost();
        String path = uri.getPath();

        boolean valid =
                "https".equalsIgnoreCase(uri.getScheme())
                        && host != null
                        && host.equalsIgnoreCase("docs.google.com")
                        && path != null
                        && path.startsWith("/document/d/");

        if (valid) {
            saveDocumentUrl(value);
            addLog("INFO", "Ссылка была сохранена");
        } else {
            addLog("WARNING", "Ссылка Google Docs имеет неверный формат.");
        }
        String[] parts = path.split("/");
        if (parts.length < 4 || parts[3].isEmpty()) {
            addLog("WARNING", "Не удалось определить ID документа.");
            return;
        }
        String documentId = parts[3];

        addLog("INFO", "Начинаю загрузку Google Docs.");
        loadGoogleDocument(documentId);
    }
    private void loadGoogleDocument(String documentId) {
        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(
                        "https://docs.googleapis.com/v1/documents/"
                                + Uri.encode(documentId)
                );

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty(
                        "Authorization",
                        "Bearer " + googleAccessToken
                );
                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                int status = connection.getResponseCode();

                InputStream stream = status >= 200 && status < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                StringBuilder response = new StringBuilder();

                if (stream != null) {
                    try (BufferedReader reader =
                                 new BufferedReader(
                                         new InputStreamReader(
                                                 stream,
                                                 StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }
                }

                if (status < 200 || status >= 300) {

                    runOnUiThread(() -> {
                        addLog("ERROR",
                                "Google Docs API вернул HTTP "
                                        + status + ".");

                        if (status == 401 || googleAccessToken == null || googleAccessToken.isEmpty()) {
                            connectGoogle();
                            Toast.makeText(this, "Ожидание подключения Google" , Toast.LENGTH_SHORT).show();

                        } else if (status == 403) {
                            addLog("WARNING",
                                    "Нет доступа к документу или API не включён.");
                        } else if (status == 404) {
                            addLog("WARNING",
                                    "Документ не найден или недоступен.");
                        }
                    });
                    return;
                }

                JSONObject document =
                        new JSONObject(response.toString());

                StringBuilder text = new StringBuilder();
                collectDocumentContent(document, text);

                Map<String, String> deviceConfigs = extractDeviceConfigs(text.toString());

// Сохраняем прежнюю синхронизацию общего списка
                List<String> links = extractLinks(text.toString());
                saveConfigs(links);

// Сравниваем конфигурации отдельно по номерам устройств
                runOnUiThread(() ->
                        compareAndSaveDeviceConfigs(deviceConfigs, true));

                runOnUiThread(() -> {
                        addLog("INFO",
                                        "Документ загружен. Найдено конфигураций: "
                                                + links.size());
                    });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    addLog("ERROR",
                            "Ошибка чтения Google Docs: "
                                    + e.getMessage());
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void collectDocumentContent(
            Object node,
            StringBuilder output
    ) throws JSONException {

        if (node instanceof JSONObject) {
            JSONObject object = (JSONObject) node;

            if (object.has("textRun")) {
                JSONObject textRun = object.getJSONObject("textRun");

                if (textRun.has("content")) {
                    output.append(textRun.getString("content"))
                            .append('\n');
                }

                JSONObject textStyle =
                        textRun.optJSONObject("textStyle");

                if (textStyle != null) {
                    JSONObject link = textStyle.optJSONObject("link");

                    if (link != null) {
                        String url = link.optString("url", "");

                        if (!url.isEmpty()) {
                            output.append(url).append('\n');
                        }
                    }
                }

                return;
            }

            JSONArray keys = object.names();

            if (keys != null) {
                for (int i = 0; i < keys.length(); i++) {
                    String key = keys.getString(i);
                    collectDocumentContent(object.get(key), output);
                }
            }

        } else if (node instanceof JSONArray) {
            JSONArray array = (JSONArray) node;

            for (int i = 0; i < array.length(); i++) {
                collectDocumentContent(array.get(i), output);
            }
        }
    }

    private void connectGoogle() {
        addLog("INFO", "Начато подключение Google.");

        AuthorizationRequest request =
                AuthorizationRequest.builder()
                        .setRequestedScopes(Arrays.asList(new Scope(DOCS_SCOPE))).build();

        Identity.getAuthorizationClient(this)
                .authorize(request)
                .addOnSuccessListener(result -> {
                    if (result.hasResolution()) {
                        IntentSenderRequest senderRequest =
                                new IntentSenderRequest.Builder(
                                        result.getPendingIntent()
                                                .getIntentSender()).build();

                        authorizationLauncher.launch(senderRequest);
                    } else {
                        handleAuthorizationSuccess(result);
                    }
                })
                .addOnFailureListener(error ->
                        addLog("ERROR",
                                "Ошибка подключения Google: "
                                        + error.getMessage()));
    }
    private void handleAuthorizationSuccess(AuthorizationResult result) {
        String accessToken = result.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            addLog("ERROR", "Google не предоставил токен доступа");
            return;
        }

        googleAccessToken = accessToken;

        preferences.edit()
                .putBoolean(KEY_GOOGLE_CONNECTED, true)
                .putString("google_access_token", accessToken)
                .apply();

        addLog("INFO", "Google подключён, токен получен.");
        validateDocumentUrl();
    }

    private boolean isGoogleConnected() {
        return preferences.getBoolean(KEY_GOOGLE_CONNECTED, false);
    }
    private void saveDocumentUrl(String url) {
        String value = url.trim();

        if (!value.startsWith("https://")
                && !value.startsWith("http://")) {
            value = "https://docs.google.com/document/d/"
                    + value + "/edit";
        }

        preferences.edit()
                .putString("saved_document_url", value)
                .apply();
    }
    private String loadDocumentUrl() {
        return preferences.getString("saved_document_url", "");
    }
    private List<String> extractLinks(String text) {
        LinkedHashSet<String> links =
                new LinkedHashSet<>();

        Pattern pattern = Pattern.compile(
                        "(?i)(?:vless|vmess|trojan|ss|vpn|https)://[^\\s<>\"']+");

        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String link = matcher.group().trim();

            if (!link.isEmpty()) {
                links.add(link);
            }
        }

        return new ArrayList<>(links);
    }
    private Map<String, String> extractDeviceConfigs(String text) {
        Map<String, String> devices = new LinkedHashMap<>();

        Pattern pattern = Pattern.compile(
                        "(?is)(?:устройство|подписка)\\s*([\\p{L}\\d]+)\\s*:\\s*(.*?)(?=(?:устройство|подписка)\\s*[\\p{L}\\d]+\\s*:|$)");

        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String deviceNumber = matcher.group(1);
            String block = matcher.group(2);

            List<String> links = extractLinks(block);

            if (!links.isEmpty()) {
                devices.put(deviceNumber, links.get(0));
            }
        }

        return devices;
    }
    private void compareAndSaveDeviceConfigs(Map<String, String> newDevices, boolean send) {

        String savedJson = preferences.getString(
                KEY_DEVICE_CONFIGS_JSON, "{}");

        Map<String, String> oldDevices = new LinkedHashMap<>();
        JSONObject statuses = new JSONObject();

        try {
            JSONObject saved = new JSONObject(savedJson);

            Iterator<String> keys = saved.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                oldDevices.put(key, saved.getString(key));
            }
        } catch (Exception e) {
            addLog("ERROR", "Не удалось прочитать сохранённые устройства.");
            return;
        }

        int added = 0;
        int changed = 0;
        int unchanged = 0;
        int removed = 0;

        for (Map.Entry<String, String> entry
                : newDevices.entrySet()) {

            String device = entry.getKey();
            String config = entry.getValue();
            String oldConfig = oldDevices.get(device);
            String status;

            if (oldConfig == null) {
                status = "Новый";
                added++;
            } else if (oldConfig.equals(config)) {
                status = "Без Изменений";
                unchanged++;
            } else {
                status = "Изменён";
                changed++;
            }

            try {
                statuses.put(config, status);
            } catch (JSONException e) {
                addLog("ERROR", "Не удалось сохранить статус конфигурации.");
            }
        }

        for (String device : oldDevices.keySet()) {
            if (!newDevices.containsKey(device)) {
                removed++;
            }
        }

        JSONObject result = new JSONObject();

        for (Map.Entry<String, String> entry
                : newDevices.entrySet()) {
            try {
                result.put(
                        String.valueOf(entry.getKey()),
                        entry.getValue());
            } catch (JSONException e) {
                addLog("ERROR", "Не удалось сохранить устройство.");
                return;
            }
        }

        preferences.edit()
                .putString(KEY_DEVICE_CONFIGS_JSON, result.toString())
                .apply();

        preferences.edit()
                .putString(KEY_CONFIG_STATUSES, statuses.toString())
                .apply();

        addLog("INFO",
                "Конфигурации: новых: " + added
                        + ", изменённых: " + changed
                        + ", без изменений: " + unchanged
                        + ", удалённых: " + removed);

        if (send) sendConfigsToVpnClient();
    }

    private void checkLatestVersion() {
        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL("https://api.github.com/repos/aracamba/vpintaly/releases/latest");

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setRequestProperty("Vpintaly", "Java-App");

                int status = connection.getResponseCode();

                if (status < 200 || status >= 300) {
                    runOnUiThread(() -> {
                        addLog("ERROR",
                                "Ошибка получения последней версии приложения. Код ошибки: "
                                        + status + ".");
                    });
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = reader.readLine()) != null) {
                    content.append(inputLine);
                }
                reader.close();
                connection.disconnect();

                JSONObject releases = new JSONObject(content.toString());

                String latest = (String) releases.get("tag_name");
                String versionName = "";
                try {
                    PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);

                    if (info.versionName != null) {
                        versionName = info.versionName;
                    }
                } catch (Exception e) {
                }
                if (!latest.equals(versionName)) {
                    runOnUiThread(() -> update(latest));
                }

            } catch (Exception e) {
                runOnUiThread(() -> addLog("ERROR", "Ошибка получения последней версии приложения: "));
            }
        });
    }

    private void update(String latest){
        AlertDialog.Builder bilder = new AlertDialog.Builder(this)
                .setTitle("Обнаружена новая версия Vpintaly("+ latest + ")")
                .setMessage("Обновить Vpintaly?")
                .setPositiveButton("Обновить", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Aracamba/Vpintaly/releases/download/" + latest + "/vpintaly."+ latest +".apk"));
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                    }
                })
                .setNegativeButton("Позже", null);
        AlertDialog dialog = bilder.create();
        dialog.show();
    }

    private GradientDrawable makeBackground(
            int fillColor, int strokeColor, int radiusDp) {

        GradientDrawable background = new GradientDrawable();
        background.setColor(fillColor);
        background.setCornerRadius(dp(radiusDp));
        background.setStroke(dp(1), strokeColor);
        return background;
    }

    private int dp(int value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }
}