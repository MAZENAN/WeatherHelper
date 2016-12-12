package com.demo.yufan.myweatherhelper.fragment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.demo.yufan.myweatherhelper.R;
import com.demo.yufan.myweatherhelper.db.City;
import com.demo.yufan.myweatherhelper.db.County;
import com.demo.yufan.myweatherhelper.db.Province;
import com.demo.yufan.myweatherhelper.global.MyUrl;
import com.demo.yufan.myweatherhelper.util.HttpUtil;
import com.demo.yufan.myweatherhelper.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import activity.ActivityWeather;
import activity.MainActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Administrator on 2016/12/11.
 */

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private int currentLevel;


    private Button btnBack;
    private TextView tvTitle;
    private ListView lvArea;
    private ProgressDialog mProgressDialog;

    private List<String> datas = new ArrayList<>();
    private ArrayAdapter<String> mAdapter;
    private List<Province> mProvinces;
    private List<City> mCities;
    private List<County> mCounties;

    private Province selectedProvince;
    private City selectedCity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        btnBack = (Button) view.findViewById(R.id.btn_back);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        lvArea = (ListView) view.findViewById(R.id.lv_area);
        mAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, datas);
        lvArea.setAdapter(mAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        lvArea.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = mProvinces.get(position);
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = mCities.get(position);
                    queryCounties();
                } else if (currentLevel == LEVEL_COUNTY) {
                    String weatherId = mCounties.get(position).getWeatherId();
                    if(getActivity() instanceof MainActivity){
                        Intent intent= new Intent(getActivity(), ActivityWeather.class);
                        intent.putExtra("weather_id",weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if(getActivity() instanceof ActivityWeather){
                        ActivityWeather activityWeather = (ActivityWeather) getActivity();
                        activityWeather.mDrawer.closeDrawers();
                        activityWeather.sw.setRefreshing(true);
                        activityWeather.requestWeather(weatherId);
                    }

                }
            }
        });
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_CITY) {
                    queryProvinces();
                } else if (currentLevel == LEVEL_COUNTY) {
                    queryCities();
                }
            }
        });
        queryProvinces();
    }

    /*
    可能会在子线程中被调用
     */
    private void queryProvinces() {
        //初始化界面
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                tvTitle.setText("中国");
                btnBack.setVisibility(View.GONE);
            }
        });

        //先从数据库加载
        mProvinces = DataSupport.findAll(Province.class);
        //如果有数据
        if (mProvinces.size() > 0) {
            datas.clear();
            for (Province province : mProvinces) {
                datas.add(province.getProvinceName());
            }
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.notifyDataSetChanged();
                    lvArea.setSelection(0);
                }
            });
            currentLevel = LEVEL_PROVINCE;
        }
        //如果没有数据,就从服务器上查询
        else {
            queryFromServer(MyUrl.SERVER, "province");
        }

    }

    private void queryCounties() {
//初始化界面
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                tvTitle.setText(selectedCity.getCityName());
                btnBack.setVisibility(View.VISIBLE);
            }
        });

        //先从数据库加载数据
        mCounties = DataSupport.where("cityid=?", String.valueOf(selectedCity.getId())).find(County.class);
        if (mCounties.size() > 0) {
            datas.clear();
            for (County county : mCounties) {
                datas.add(county.getCountyName());
            }
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.notifyDataSetChanged();
                    lvArea.setSelection(0);
                }
            });

            currentLevel = LEVEL_COUNTY;
        }else {
            queryFromServer(MyUrl.SERVER + "/" + selectedProvince.getProvinceCode() + "/"
                    + selectedCity.getCityCode(), "county");
        }

    }

    private void queryCities() {
        //初始化界面
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                btnBack.setVisibility(View.VISIBLE);
                tvTitle.setText(selectedProvince.getProvinceName());
            }
        });

        //先从数据库加载数据
        mCities = DataSupport.where("provinceid=?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (mCities.size() > 0) {
            datas.clear();
            for (City city : mCities) {
                datas.add(city.getCityName());
            }
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.notifyDataSetChanged();
                    lvArea.setSelection(0);
                }
            });

            currentLevel = LEVEL_CITY;
        } else {
            queryFromServer(MyUrl.SERVER + "/" + selectedProvince.getProvinceCode(), "city");
        }

    }

    /*
    从服务器查询数据
     */
    private void queryFromServer(String server, final String type) {
        showDialog();
        HttpUtil.sendHttpRequest(server, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //加载失败，切换到主线程关闭对话框
                closeDialog();
                Toast.makeText(getContext(), "查询失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String data = response.body().string();
                boolean result = false;
                //解析数据,并且存储到数据库
                if ("province".equals(type)) {
                    result = Utility.handleProvinceResponse(data);
                } else if ("city".equals(type)) {
                    result = Utility.handleCityResponse(data, selectedProvince.getId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountyRespose(data, selectedCity.getId());
                }
                if (result) {
                    //解析成功
                    closeDialog();
                    if ("province".equals(type)) {
                        queryProvinces();
                    } else if ("city".equals(type)) {
                        queryCities();
                    } else if ("county".equals(type)) {
                        queryCounties();
                    }
                }
            }
        });
    }


    /*
    显示对话框
     */
    private void showDialog() {

        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog == null) {
                    mProgressDialog = new ProgressDialog(getContext());
                    mProgressDialog.setMessage("正在查询...");
                    mProgressDialog.setCanceledOnTouchOutside(false);
                }
                mProgressDialog.show();
            }
        });

    }

    /*
    关闭对话框
     */
    private void closeDialog() {
        if (mProgressDialog != null) {
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    mProgressDialog.dismiss();
                }
            });

        }
    }

    private void runOnUIThread(Runnable r) {
        getActivity().runOnUiThread(r);
    }
}
