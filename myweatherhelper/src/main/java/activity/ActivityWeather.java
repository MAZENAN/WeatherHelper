package activity;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.demo.yufan.myweatherhelper.R;
import com.demo.yufan.myweatherhelper.global.MyUrl;
import com.demo.yufan.myweatherhelper.gson.WeatherInfo;
import com.demo.yufan.myweatherhelper.util.HttpUtil;
import com.demo.yufan.myweatherhelper.util.Utility;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Administrator on 2016/12/12.
 */

public class ActivityWeather extends AppCompatActivity {
    private ScrollView mSc;
    private TextView mTvCity;
    private TextView tvUpTime;
    private TextView tvDegree;
    private TextView tvWeatherInfo;
    private LinearLayout mLloforecast;
    private TextView mTvAqi;
    private TextView mTVpM25;
    private TextView mTvComfort;
    private TextView mTvCarWash;
    private TextView mTvSport;
    private ImageView pic;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT>=21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            setContentView(R.layout.activity_weather);
        }
        initView();
    }

    private void initView() {
        pic = (ImageView) findViewById(R.id.img_bac);
        mSc = (ScrollView) findViewById(R.id.sc_weather_layout);
        mTvCity = (TextView) findViewById(R.id.tv_weather_city);
        tvUpTime = (TextView) findViewById(R.id.tv_weather_update_time);
        tvDegree = (TextView) findViewById(R.id.tv_weather_degree);
        tvWeatherInfo = (TextView) findViewById(R.id.tv_weather_info);
        mLloforecast = (LinearLayout) findViewById(R.id.ll_weather_forecast);
        mTvAqi = (TextView) findViewById(R.id.tv_weather_aqi);
        mTVpM25 = (TextView) findViewById(R.id.tv_weather_pm25);
        mTvComfort = (TextView) findViewById(R.id.tv_sugg_comfort);
        mTvCarWash = (TextView) findViewById(R.id.tv_sugg_car_wash);
        mTvSport = (TextView) findViewById(R.id.tv_sugg_sport);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String weather = preferences.getString("weather", null);
        if (weather != null) {
            //解析天气
            WeatherInfo weatherInfo = Utility.handleWeatherResponse(weather);
            showWeatherInfo(weatherInfo);
        } else {
            //查询天气
            String weather_id = getIntent().getStringExtra("weather_id");
            mSc.setVisibility(View.INVISIBLE);
            requestWeather(weather_id);
        }
        String bing_pic = preferences.getString("bing_pic", null);
        if (bing_pic != null) {
            Glide.with(this).load(bing_pic).into(pic);
        } else {
            loadBingPic();
        }
    }

    private void loadBingPic() {
        HttpUtil.sendHttpRequest(MyUrl.PIC, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String picUrl = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ActivityWeather.this).edit();
                editor.putString("bing_pic", picUrl);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(ActivityWeather.this).load(picUrl).into(pic);
                    }
                });
            }
        });
    }

    private void requestWeather(String weatherId) {
        String weatherUrl = MyUrl.WEATHER_SERVER + "cityid=" + weatherId + "&key=" + MyUrl.WEATHER_KEY;
        HttpUtil.sendHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ActivityWeather.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final WeatherInfo weatherInfo = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weatherInfo != null && "ok".equals(weatherInfo.getHeWeather().get(0).getStatus())) {
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ActivityWeather.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            showWeatherInfo(weatherInfo);
                        } else {
                            Toast.makeText(ActivityWeather.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        loadBingPic();
    }

    private void showWeatherInfo(WeatherInfo weatherInfo) {
        List<WeatherInfo.HeWeatherBean> heWeather = weatherInfo.getHeWeather();
        WeatherInfo.HeWeatherBean heWeatherBean = heWeather.get(0);
        WeatherInfo.HeWeatherBean.BasicBean basic = heWeatherBean.getBasic();
        WeatherInfo.HeWeatherBean.AqiBean aqi = heWeatherBean.getAqi();
        List<WeatherInfo.HeWeatherBean.DailyForecastBean> daily_forecast = heWeatherBean.getDaily_forecast();
        List<WeatherInfo.HeWeatherBean.HourlyForecastBean> hourly_forecast = heWeatherBean.getHourly_forecast();
        WeatherInfo.HeWeatherBean.NowBean now = heWeatherBean.getNow();
        WeatherInfo.HeWeatherBean.SuggestionBean suggestion = heWeatherBean.getSuggestion();
        String city = basic.getCity();
        WeatherInfo.HeWeatherBean.BasicBean.UpdateBean update = basic.getUpdate();
        String upTime = update.getLoc();
        String tmp = now.getTmp() + "℃";
        String info = now.getCond().getTxt();

        mTvCity.setText(city);
        tvUpTime.setText(upTime);
        tvDegree.setText(tmp);
        tvWeatherInfo.setText(info);
        for (WeatherInfo.HeWeatherBean.DailyForecastBean dailyForecastBean : daily_forecast) {
            View view = LayoutInflater.from(this).inflate(R.layout.weather_forecast_item, null, false);
            TextView tvDate = (TextView) view.findViewById(R.id.tv_weather_date);
            TextView minDegree = (TextView) view.findViewById(R.id.tv_weather_min_degree);
            TextView maxDegree = (TextView) view.findViewById(R.id.tv_weather_max_degree);
            TextView weaInfo = (TextView) view.findViewById(R.id.tv_weather_info);

            tvDate.setText(dailyForecastBean.getDate());
            minDegree.setText(dailyForecastBean.getTmp().getMin());
            maxDegree.setText(dailyForecastBean.getTmp().getMax());
            weaInfo.setText(dailyForecastBean.getCond().getTxt_d());
            mLloforecast.addView(view);
        }
        if (aqi != null) {
            mTvAqi.setText(aqi.getCity().getAqi());
            mTVpM25.setText(aqi.getCity().getPm25());
        }
        String comfort = "舒适度：" + suggestion.getComf().getTxt();
        String carWash = "洗车指数" + suggestion.getCw().getTxt();
        String sport = "运动指数" + suggestion.getSport().getTxt();
        mTvComfort.setText(comfort);
        mTvCarWash.setText(carWash);
        mTvSport.setText(sport);
        mSc.setVisibility(View.VISIBLE);
    }
}
