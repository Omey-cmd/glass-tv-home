package com.gathin.tvhome;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class WeatherActivity extends Activity {

    private TextView weatherLocation;
    private ImageView weatherMainIcon;
    private TextView weatherMainTemp;
    private TextView weatherMainCondition;
    private TextView weatherMainSub;
    private TextView weatherDetail1;
    private TextView weatherDetail2;

    private TextView aqiValue;
    private TextView aqiStatus;
    private TextView aqiStation;
    private View aqiBar;
    private View aqiIndicator;
    private TextView aqiStationsSub;
    private TextView aqiModelSub;
    private TextView aqiAdvisory;

    private LinearLayout hourlyContainer;
    private LinearLayout dailyContainer;
    private TextView weatherFooter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        weatherLocation = findViewById(R.id.weatherLocation);
        weatherMainIcon = findViewById(R.id.weatherMainIcon);
        weatherMainTemp = findViewById(R.id.weatherMainTemp);
        weatherMainCondition = findViewById(R.id.weatherMainCondition);
        weatherMainSub = findViewById(R.id.weatherMainSub);
        weatherDetail1 = findViewById(R.id.weatherDetail1);
        weatherDetail2 = findViewById(R.id.weatherDetail2);

        aqiValue = findViewById(R.id.aqiValue);
        aqiStatus = findViewById(R.id.aqiStatus);
        aqiStation = findViewById(R.id.aqiStation);
        aqiBar = findViewById(R.id.aqiBar);
        aqiIndicator = findViewById(R.id.aqiIndicator);
        aqiStationsSub = findViewById(R.id.aqiStationsSub);
        aqiModelSub = findViewById(R.id.aqiModelSub);
        aqiAdvisory = findViewById(R.id.aqiAdvisory);

        hourlyContainer = findViewById(R.id.hourlyContainer);
        dailyContainer = findViewById(R.id.dailyContainer);
        weatherFooter = findViewById(R.id.weatherFooter);

        populateUi();

        // Background sync to ensure freshest data
        WeatherAirQuality.syncIfDue(this, this::populateUi);
    }

    private void populateUi() {
        WeatherAirQuality.FullWeather w = WeatherAirQuality.getFullWeather(this);
        if (w == null) return;

        weatherLocation.setText(w.location);
        weatherMainTemp.setText(w.temp + "°");
        weatherMainCondition.setText(w.condition);
        weatherMainSub.setText(String.format(Locale.ROOT, "Feels like %d°  •  H %d° L %d°",
                w.apparentTemp, w.highTemp, w.lowTemp));
        weatherMainIcon.setImageResource(getIconDrawable(w.icon));

        weatherDetail1.setText(String.format(Locale.ROOT, "Humidity %d%%  •  Wind %d km/h  •  UV %d",
                w.humidity, w.windSpeed, w.uvIndex));
        weatherDetail2.setText(String.format(Locale.ROOT, "Rain chance today %d%%  •  Sunrise %s  •  Sunset %s",
                w.rainChanceToday, w.sunrise, w.sunset));

        // Air Quality
        aqiValue.setText(String.valueOf(w.aqi));
        aqiValue.setTextColor(w.aqiColor);
        aqiStatus.setText(w.aqiStatus);
        aqiStatus.setTextColor(w.aqiColor);
        aqiStation.setText("API  •  " + w.location + "  •  PM2.5");

        // Scale bar indicator position
        aqiBar.post(() -> {
            int barWidth = aqiBar.getWidth();
            int indWidth = aqiIndicator.getWidth();
            if (barWidth > 0) {
                float fraction = Math.min(1.0f, Math.max(0.0f, (w.aqi - 10f) / 250f));
                float transX = fraction * (barWidth - indWidth);
                aqiIndicator.setTranslationX(transX);
            }
        });

        // Nearby reference stations
        if (w.location.contains("MELAKA")) {
            aqiStationsSub.setText("Alor Gajah 97  •  Bukit Rambai 110");
        } else {
            aqiStationsSub.setText("Johor Bahru " + w.aqi);
        }

        aqiModelSub.setText(String.format(Locale.ROOT, "Model US AQI %d  •  PM2.5 %.1f  •  PM10 %.1f µg/m³",
                w.aqi, w.pm25, w.pm10));

        // Health advisory based on AQI
        if (w.aqi <= 50) {
            aqiAdvisory.setText("Air quality is satisfactory. Normal outdoor activities can be enjoyed.");
        } else if (w.aqi <= 100) {
            aqiAdvisory.setText("Normal activities. People with heart or lung conditions should limit long outdoor exertion.");
        } else if (w.aqi <= 150) {
            aqiAdvisory.setText("Sensitive groups should reduce prolonged or heavy outdoor exertion.");
        } else {
            aqiAdvisory.setText("Everyone should reduce prolonged outdoor exertion. Keep windows closed.");
        }

        // Hourly row (Next 12 Hours)
        hourlyContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < w.hourly.size(); i++) {
            WeatherAirQuality.HourlyItem item = w.hourly.get(i);
            View v = inflater.inflate(R.layout.item_weather_hourly, hourlyContainer, false);
            TextView timeView = v.findViewById(R.id.hourlyTime);
            ImageView iconView = v.findViewById(R.id.hourlyIcon);
            TextView tempView = v.findViewById(R.id.hourlyTemp);
            TextView rainView = v.findViewById(R.id.hourlyRain);

            timeView.setText(item.time);
            iconView.setImageResource(getIconDrawable(item.icon));
            tempView.setText(item.temp + "°");
            rainView.setText("↑ " + item.rainChance + "%");

            setupFocusAnimation(v);
            hourlyContainer.addView(v);

            // Set initial focus to the first item for instant D-pad navigation
            if (i == 0) {
                v.requestFocus();
            }
        }

        // 7 Days row
        dailyContainer.removeAllViews();
        for (int i = 0; i < w.daily.size(); i++) {
            WeatherAirQuality.DailyItem item = w.daily.get(i);
            View v = inflater.inflate(R.layout.item_weather_daily, dailyContainer, false);
            TextView dayView = v.findViewById(R.id.dailyDay);
            ImageView iconView = v.findViewById(R.id.dailyIcon);
            TextView tempView = v.findViewById(R.id.dailyTemp);
            TextView rainView = v.findViewById(R.id.dailyRain);

            dayView.setText(item.day);
            iconView.setImageResource(getIconDrawable(item.icon));
            tempView.setText(item.maxTemp + "° / " + item.minTemp + "°");
            rainView.setText("↑ " + item.rainChance + "%");

            setupFocusAnimation(v);
            dailyContainer.addView(v);
        }

        weatherFooter.setText(String.format(Locale.ROOT,
                "Updated %s  •  API: Jabatan Alam Sekitar (APIMS)  •  Weather & Forecast: Open-Meteo",
                w.updatedTime));
    }

    private void setupFocusAnimation(View v) {
        v.setOnFocusChangeListener((view, hasFocus) -> {
            float scale = hasFocus ? 1.06f : 1.0f;
            float elev = hasFocus ? 8f : 0f;
            view.animate().scaleX(scale).scaleY(scale).translationZ(elev).setDuration(150).start();
        });
    }

    private int getIconDrawable(String icon) {
        if (icon == null) return R.drawable.ic_weather_partly_cloudy;
        if (icon.contains("☀")) return R.drawable.ic_weather_sun;
        if (icon.contains("⛅")) return R.drawable.ic_weather_partly_cloudy;
        if (icon.contains("🌧")) return R.drawable.ic_weather_rain;
        if (icon.contains("⛈")) return R.drawable.ic_weather_thunder;
        if (icon.contains("☁") || icon.contains("🌫")) return R.drawable.ic_weather_cloud;
        return R.drawable.ic_weather_partly_cloudy;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
