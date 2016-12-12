package com.demo.yufan.myweatherhelper.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.demo.yufan.myweatherhelper.global.MyUrl;
import com.demo.yufan.myweatherhelper.gson.WeatherInfo;
import com.demo.yufan.myweatherhelper.util.HttpUtil;
import com.demo.yufan.myweatherhelper.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by YUfAN on 2016/12/12.
 * 自动更新的业务逻辑
 */

public class AutoUpdateService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 定时任务的业务逻辑
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWeather();
        updatePic();
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int anHour = 8 * 60 * 60 * 1000;
        long triggerAtTime = SystemClock.elapsedRealtime() + anHour;
        Intent i = new Intent(this, AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 后台更新每日一图的业务逻辑,后台请求网络,拿到数据更新缓存
     */
    private void updatePic() {
        HttpUtil.sendHttpRequest(MyUrl.PIC, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String picUrl = response.body().string();
                if (!TextUtils.isEmpty(picUrl)) {
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                    editor.putString("bing_pic", picUrl);
                    editor.apply();
                }

            }
        });
    }

    /**
     * 后台更新天气的业务逻辑,先从缓存中读取weatherId,再去请求网络,请求成功后更新缓存数据
     */
    private void updateWeather() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String weather = preferences.getString("weather", null);
        if (weather != null) {
            WeatherInfo weatherInfo = Utility.handleWeatherResponse(weather);
            String weatherId = weatherInfo.getHeWeather().get(0).getBasic().getId();
            String weatherUrl = MyUrl.WEATHER_SERVER + "cityid=" + weatherId + "&key=" + MyUrl.WEATHER_KEY;
            HttpUtil.sendHttpRequest(weatherUrl, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText = response.body().string();
                    WeatherInfo weatherInfo1 = Utility.handleWeatherResponse(responseText);
                    if (weatherInfo1 != null && "ok".equals(weatherInfo1.getHeWeather().get(0).getStatus())) {
                        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        edit.putString("weather", responseText);
                        edit.apply();
                    }
                }
            });
        }
    }
}
