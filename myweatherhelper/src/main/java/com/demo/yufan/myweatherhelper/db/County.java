package com.demo.yufan.myweatherhelper.db;

import org.litepal.crud.DataSupport;

/**
 * Created by Administrator on 2016/12/11.
 */

public class County extends DataSupport{
    private int id;
    private String countyName;
    private int cityId;
    private String weatherId;

    public void setCityId(int cityId) {
        this.cityId = cityId;
    }

    public void setCountyName(String countyName) {
        this.countyName = countyName;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setWeatherId(String weatherId) {
        this.weatherId = weatherId;
    }

    public int getCityId() {
        return cityId;
    }

    public String getCountyName() {
        return countyName;
    }

    public int getId() {
        return id;
    }

    public String getWeatherId() {
        return weatherId;
    }
}
