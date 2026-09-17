package com.metalsratepkr.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private TextView gold24View, gold22View, gold24GramView;
    private TextView silverTolaView, silverGramView;
    private TextView status;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final double GRAMS_PER_TROY_OUNCE = 31.1034768;
    private static final double GRAMS_PER_TOLA = 11.6638125;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildInterface();
        getRates();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getRates();
                handler.postDelayed(this, 5 * 60 * 1000);
            }
        }, 5 * 60 * 1000);
    }

    private void buildInterface() {

        ScrollView scroll = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(30, 35, 30, 35);
        root.setBackgroundColor(Color.rgb(18, 18, 18));

        TextView title = new TextView(this);
        title.setText("GoldSilverPkr");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("Live Gold & Silver Rates in Pakistan");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 8, 0, 25);
        root.addView(subtitle, matchWrap());

        status = new TextView(this);
        status.setText("Updating rates...");
        status.setTextColor(Color.GREEN);
        status.setTextSize(15);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 0, 0, 25);
        root.addView(status, matchWrap());

        gold24View = addRateCard(root, "Gold 24K — Per Tola");
        gold22View = addRateCard(root, "Gold 22K — Per Tola");
        gold24GramView = addRateCard(root, "Gold 24K — Per Gram");

        silverTolaView = addRateCard(root, "Silver — Per Tola");
        silverGramView = addRateCard(root, "Silver — Per Gram");

        Button refresh = new Button(this);
        refresh.setText("Refresh Rates");
        refresh.setTextSize(16);

        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getRates();
            }
        });

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        buttonParams.setMargins(0, 25, 0, 20);
        root.addView(refresh, buttonParams);

        TextView note = new TextView(this);
        note.setText(
                "Rates are calculated from international gold/silver spot prices " +
                "converted to PKR. Local Sarafa and jeweller rates may differ."
        );
        note.setTextColor(Color.GRAY);
        note.setTextSize(13);
        note.setGravity(Gravity.CENTER);
        root.addView(note, matchWrap());

        TextView footer = new TextView(this);
        footer.setText("GoldSilverPkr © 2026");
        footer.setTextColor(Color.GRAY);
        footer.setTextSize(13);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, 25, 0, 10);
        root.addView(footer, matchWrap());

        scroll.addView(root);
        setContentView(scroll);
    }

    private TextView addRateCard(LinearLayout root, String label) {

        TextView card = new TextView(this);

        card.setText(label + "\nLoading...");
        card.setTextColor(Color.WHITE);
        card.setTextSize(19);
        card.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.setPadding(25, 25, 25, 25);
        card.setBackgroundColor(Color.rgb(35, 35, 35));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 0, 0, 15);

        root.addView(card, params);

        return card;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private void getRates() {

        status.setText("● Updating live rates...");

        executor.execute(new Runnable() {
            @Override
            public void run() {

                try {

                    double goldUsd = getPrice(
                            "https://api.gold-api.com/price/XAU"
                    );

                    double silverUsd = getPrice(
                            "https://api.gold-api.com/price/XAG"
                    );

                    double usdPkr = getExchangeRate(
                            "https://api.frankfurter.dev/v2/rate/usd/pkr"
                    );

                    double goldPkrOz = goldUsd * usdPkr;
                    double silverPkrOz = silverUsd * usdPkr;

                    double gold24Gram =
                            goldPkrOz / GRAMS_PER_TROY_OUNCE;

                    double gold24Tola =
                            gold24Gram * GRAMS_PER_TOLA;

                    double gold22Gram =
                            gold24Gram * (22.0 / 24.0);

                    double gold22Tola =
                            gold22Gram * GRAMS_PER_TOLA;

                    double silverGram =
                            silverPkrOz / GRAMS_PER_TROY_OUNCE;

                    double silverTola =
                            silverGram * GRAMS_PER_TOLA;

                    final String g24 =
                            "Gold 24K — Per Tola\n" +
                            formatPKR(gold24Tola);

                    final String g22 =
                            "Gold 22K — Per Tola\n" +
                            formatPKR(gold22Tola);

                    final String g24g =
                            "Gold 24K — Per Gram\n" +
                            formatPKR(gold24Gram);

                    final String sTola =
                            "Silver — Per Tola\n" +
                            formatPKR(silverTola);

                    final String sGram =
                            "Silver — Per Gram\n" +
                            formatPKR(silverGram);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                            gold24View.setText(g24);
                            gold22View.setText(g22);
                            gold24GramView.setText(g24g);
silverTolaView.setText(sTola);
silverGramView.setText(sGram);

                            status.setText("● Live rates updated");
                        }
                    });

                } catch (Exception e) {

                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                            status.setText("Unable to update rates");

                            Toast.makeText(
                                    MainActivity.this,
                                    "Please check your internet connection.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    });
                }
            }
        });
    }

    private double getPrice(String address) throws Exception {

        String response = download(address);
        JSONObject json = new JSONObject(response);

        return json.getDouble("price");
    }

    private double getExchangeRate(String address) throws Exception {

        String response = download(address);
        JSONObject json = new JSONObject(response);

        return json.getDouble("rate");
    }

    private String download(String address) throws Exception {

        URL url = new URL(address);

        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        connection.getInputStream()
                )
        );

        StringBuilder result = new StringBuilder();

        String line;

        while ((line = reader.readLine()) != null) {
            result.append(line);
        }

        reader.close();
        connection.disconnect();

        return result.toString();
    }

    private String formatPKR(double value) {

        return String.format(
                Locale.US,
                "PKR %,.0f",
                value
        );
    }

    @Override
    protected void onDestroy() {

        handler.removeCallbacksAndMessages(null);
        executor.shutdownNow();

        super.onDestroy();
    }
            }                          
