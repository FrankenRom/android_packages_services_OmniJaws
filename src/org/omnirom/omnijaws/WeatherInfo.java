/*
 * Copyright (C) 2012 The AOKP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omnirom.omnijaws;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;

public class WeatherInfo {
    private static final DecimalFormat sNoDigitsFormat = new DecimalFormat("0");

    private Context mContext;

    private String id;
    private String city;
    private String condition;
    private int conditionCode;
    private float temperature;
    private float humidity;
    private float wind;
    private int windDirection;
    private long timestamp;
    private ArrayList<DayForecast> forecasts;
    private boolean metric;

    public WeatherInfo(Context context, String id,
            String city, String condition, int conditionCode, float temp,
            float humidity, float wind, int windDir,
            boolean metric, ArrayList<DayForecast> forecasts, long timestamp) {
        this.mContext = context.getApplicationContext();
        this.id = id;
        this.city = city;
        this.condition = condition;
        this.conditionCode = conditionCode;
        this.humidity = humidity;
        this.wind = wind;
        this.windDirection = windDir;
        this.timestamp = timestamp;
        this.temperature = temp;
        this.forecasts = forecasts;
        this.metric = metric;
    }

    public static class WeatherLocation {
        public String id;
        public String city;
        public String postal;
        public String countryId;
        public String country;
    }
    
    public static class DayForecast {
        public final float low, high;
        public final int conditionCode;
        public final String condition;

        public DayForecast(float low, float high, String condition, int conditionCode) {
            this.low = low;
            this.high = high;
            this.condition = condition;
            this.conditionCode = conditionCode;
        }

        public String getFormattedLow() {
            return getFormattedValue(low, "\u00b0");
        }

        public String getFormattedHigh() {
            return getFormattedValue(high, "\u00b0");
        }

        public String getCondition(Context context) {
            return WeatherInfo.getCondition(context, conditionCode, condition);
        }

        public int getConditionCode() {
            return conditionCode;
        }
    }

    public String getId() {
        return id;
    }

    public String getCity() {
        return city;
    }

    public String getCondition() {
        return getCondition(mContext, conditionCode, condition);
    }

    public int getConditionCode() {
        return conditionCode;
    }

    private static String getCondition(Context context, int conditionCode, String condition) {
        final Resources res = context.getResources();
        final int resId = res.getIdentifier("weather_" + conditionCode, "string", context.getPackageName());
        if (resId != 0) {
            return res.getString(resId);
        }
        return condition;
    }

    public Date getTimestamp() {
        return new Date(timestamp);
    }

    private static String getFormattedValue(float value, String unit) {
        if (Float.isNaN(value)) {
            return "-";
        }
        String formatted = sNoDigitsFormat.format(value);
        if (formatted.equals("-0")) {
            formatted = "0";
        }
        return formatted + unit;
    }

    public String getFormattedLow() {
        return forecasts.get(0).getFormattedLow();
    }

    public String getFormattedHigh() {
        return forecasts.get(0).getFormattedHigh();
    }

    public String getFormattedHumidity() {
        return getFormattedValue(humidity, "%");
    }

    public String getFormattedWindSpeed() {
        if (wind < 0) {
            return "-1";
        }
        return getFormattedValue(wind, metric?"mph":"kph");
    }

    public String getWindDirection() {
        return String.valueOf(windDirection);
    }

    public ArrayList<DayForecast> getForecasts() {
        return forecasts;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WeatherInfo for ");
        builder.append(city);
        builder.append(" (");
        builder.append(id);
        builder.append(") @ ");
        builder.append(getTimestamp());
        builder.append(": ");
        builder.append(getCondition());
        builder.append("(");
        builder.append(conditionCode);
        builder.append("), temperature ");
        builder.append(temperature);
        builder.append(", low ");
        builder.append(getFormattedLow());
        builder.append(", high ");
        builder.append(getFormattedHigh());
        builder.append(", humidity ");
        builder.append(getFormattedHumidity());
        builder.append(", wind ");
        builder.append(getFormattedWindSpeed());
        builder.append(" at ");
        builder.append(getWindDirection());
        if (forecasts.size() > 0) {
            builder.append(", forecasts:");
        }
        for (int i = 0; i < forecasts.size(); i++) {
            DayForecast d = forecasts.get(i);
            if (i != 0) {
                builder.append(";");
            }
            builder.append(" day ").append(i + 1).append(": ");
            builder.append("high ").append(d.getFormattedHigh());
            builder.append(", low ").append(d.getFormattedLow());
            builder.append(", ").append(d.condition);
            builder.append("(").append(d.conditionCode).append(")");
        }
        return builder.toString();
    }

    public String toSerializedString() {
        StringBuilder builder = new StringBuilder();
        builder.append(id).append('|');
        builder.append(city).append('|');
        builder.append(condition).append('|');
        builder.append(conditionCode).append('|');
        builder.append(temperature).append('|');
        builder.append(humidity).append('|');
        builder.append(wind).append('|');
        builder.append(windDirection).append('|');
        builder.append(metric).append('|');
        builder.append(timestamp).append('|');
        serializeForecasts(builder);
        return builder.toString();
    }

    private void serializeForecasts(StringBuilder builder) {
        builder.append(forecasts.size());
        for (DayForecast d : forecasts) {
            builder.append(';');
            builder.append(d.high).append(';');
            builder.append(d.low).append(';');
            builder.append(d.condition).append(';');
            builder.append(d.conditionCode);
        }
    }

    public static WeatherInfo fromSerializedString(Context context, String input) {
        if (input == null) {
            return null;
        }

        String[] parts = input.split("\\|");
        if (parts == null || parts.length != 11) {
            return null;
        }

        int conditionCode, windDirection;
        long timestamp;
        float temperature, humidity, wind;
        boolean metric;
        String[] forecastParts = parts[10].split(";");
        int forecastItems;
        ArrayList<DayForecast> forecasts = new ArrayList<DayForecast>();

        // Parse the core data
        try {
            conditionCode = Integer.parseInt(parts[3]);
            temperature = Float.parseFloat(parts[4]);
            humidity = Float.parseFloat(parts[5]);
            wind = Float.parseFloat(parts[6]);
            windDirection = Integer.parseInt(parts[7]);
            metric = Boolean.parseBoolean(parts[8]);
            timestamp = Long.parseLong(parts[9]);
            forecastItems = forecastParts == null ? 0 : Integer.parseInt(forecastParts[0]);
        } catch (NumberFormatException e) {
            return null;
        }

        if (forecastItems == 0 || forecastParts.length != 4 * forecastItems + 1) {
            return null;
        }

        // Parse the forecast data
        try {
            for (int item = 0; item < forecastItems; item ++) {
                int offset = item * 4 + 1;
                DayForecast day = new DayForecast(
                        /* low */ Float.parseFloat(forecastParts[offset + 1]),
                        /* high */ Float.parseFloat(forecastParts[offset]),
                        /* condition */ forecastParts[offset + 2],
                        /* conditionCode */ Integer.parseInt(forecastParts[offset + 3]));
                if (!Float.isNaN(day.low) && !Float.isNaN(day.high) /*&& day.conditionCode >= 0*/) {
                    forecasts.add(day);
                }
            }
        } catch (NumberFormatException ignored) {
        }

        if (forecasts.isEmpty()) {
            return null;
        }

        return new WeatherInfo(context,
                /* id */ parts[0], /* city */ parts[1], /* condition */ parts[2],
                conditionCode, temperature,
                humidity, wind, windDirection, metric,
                /* forecasts */ forecasts, timestamp);
    }
}
