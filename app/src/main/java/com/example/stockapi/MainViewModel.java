package com.example.stockapi;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.lifecycle.AndroidViewModel;

import com.example.stockapi.model.SourceApi;
import com.example.stockapi.model.Stock;
import com.example.stockapi.model.Tpex;
import com.example.stockapi.model.Twse;
import com.google.gson.Gson;
import com.orhanobut.logger.Logger;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import static com.example.stockapi.model.SourceApi.FIRS_TPRICE;

public class MainViewModel extends AndroidViewModel {

    private static final String TAG = "MainViewModel";

    public final ObservableField<String> result = new ObservableField<>();
    public final ObservableBoolean isLoading = new ObservableBoolean(false);
    public final ObservableBoolean btnRequest = new ObservableBoolean(true);
    public final ObservableField<String> tvCashDividend = new ObservableField<>();
    public final ObservableField<String> tvStockDividend = new ObservableField<>();
    public final ObservableField<String> tvFirst = new ObservableField<>();
    public final ObservableField<String> tvEnd = new ObservableField<>();
    public final ObservableField<String> tvCommon = new ObservableField<>();

    private SourceApi sourceApi;

    // 儲存獲取的資料
    AtomicReference<Double> cashDividend = new AtomicReference<>(0.0d);
    AtomicReference<Double> stockDividend = new AtomicReference<>(0.0d);
    String firstPrice = "";
    String endPrice = "";


    //判断獲取完成
    boolean getFrist = false;
    boolean getEnd = false;
    boolean cashDividendParsed = false;
    boolean stockDividendParsed = false;


    private Gson gson = new Gson();

    MainViewModel(@NonNull Application application) {
        super(application);
        application.getAssets();
        sourceApi = new SourceApi(getApplication());
    }

    void getPrice(String YY, int type, String number) {

        boolean isTWSE = (sourceApi.getTWSElist().contains(number));

        sourceApi.getPrice(YY, type, isTWSE, number, new SourceApi.HttpCallback() {
            @Override
            public void onFailure(@NotNull IOException e) {
                Log.d(TAG, "onFailure: ", e);
                hideLoading();
                tvCommon.set("無法聯繫資料來源");
            }

            @Override
            public void onSuccess(@NotNull String result) {
                Log.d(TAG, "onResponse: " + result);
                Stock stock;
                try {
                    String stat = new JSONObject(result).optString("stat");
                    if (TextUtils.isEmpty(stat) || !stat.equals("OK")) {
                        hideLoading();
                        stat = TextUtils.isEmpty(stat) ? "資料錯誤，請檢查輸入資料" : stat;
                        tvCommon.set(stat);
                        return;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (isTWSE) {
                    stock = gson.fromJson(result, Twse.class);
                } else {
                    stock = gson.fromJson(result, Tpex.class);
                }

                if (type == FIRS_TPRICE) {
                    firstPrice = stock.getFirstPrice();
                    tvFirst.set(firstPrice);
                    getFrist = true;
                    setResult(firstPrice, endPrice,cashDividend.get(),stockDividend.get());
                } else {
                    endPrice = stock.getEndPrice();
                    tvEnd.set(stock.getEndPrice());
                    getEnd = true;
                    setResult(firstPrice, endPrice,cashDividend.get(),stockDividend.get());
                }

                if (getEnd && getFrist) {
                    hideLoading();
                }
            }
        });
    }

    void GetDividend(String number, String year) {

        int CashDIndex = 2;
        int StockDIndex = 5;


        sourceApi.getDividend(number, data -> {
            for (Element row : data.select("td > table:nth-child(2) > tbody > tr")) {
                if (row.text().contains(year)) {
//                    Logger.d(row.cssSelector());
//                    Logger.d(row.text());
                    try {
                        cashDividend.set(ParseCashDividend(row.getElementsByIndexEquals(CashDIndex).text()));
                        stockDividend.set(ParseStockDividend(row.getElementsByIndexEquals(StockDIndex).text()));


                        tvCashDividend.set(row.getElementsByIndexEquals(CashDIndex).text());
                        tvStockDividend.set(row.getElementsByIndexEquals(StockDIndex).text());

                        Logger.d("現金:" + cashDividend + " 股票股利:" + stockDividend);
                    } catch (NumberFormatException nfe) {
                        Logger.d("不正常數值，進行修正");
                        String dividend = "";
                        if (!cashDividendParsed) {
                            dividend = row.getElementsByIndexEquals(CashDIndex).text();
                            dividend = dividend.substring(dividend.length() - 5);
                            cashDividend.set(ParseCashDividend(dividend));
                        }
                        if (!stockDividendParsed) {
                            dividend = row.getElementsByIndexEquals(StockDIndex).text();
                            dividend = dividend.substring(dividend.length() - 5);
                            stockDividend.set(ParseCashDividend(dividend));
                        }
                        final String cd = String.valueOf(cashDividend.get());
                        final String sd = String.valueOf(stockDividend.get());

                        tvCashDividend.set(cd);
                        tvStockDividend.set(sd);

                        Logger.d("現金:" + cashDividend + " 股票股利:" + stockDividend);
                    }
                }
            }
        });
    }


    private void setResult(String sFirstPrice, String sEndPrice,double cashDividend , double stockDividend) {
        if (getFrist && getEnd && cashDividendParsed && stockDividendParsed) {
            double firstPrice = Double.parseDouble(sFirstPrice);
            double endPrice = Double.parseDouble(sEndPrice);
            double res = (endPrice + cashDividend - firstPrice) / firstPrice * 100;
            BigDecimal bd = new BigDecimal(res);
            res = bd.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            result.set(res + "%");
        }
    }

    private double ParseCashDividend(String cashDividend) {
        double Parsed = Double.parseDouble(cashDividend);
        cashDividendParsed = true;
        return Parsed;
    }

    private double ParseStockDividend(String stockDividend) {
        double Parsed = Double.parseDouble(stockDividend);
        stockDividendParsed = true;
        return Parsed;
    }


    /**
     * 顯示讀取圖，同時初始化
     */
    void showLoading() {
        btnRequest.set(false);
        cashDividendParsed = false;
        stockDividendParsed = false;
        getFrist = false;
        getEnd = false;
        tvCashDividend.set("");
        tvStockDividend.set("");
        tvFirst.set("");
        tvEnd.set("");
        result.set("");
        tvCommon.set("");
        isLoading.set(true);
    }

    /**
     * 隱藏讀取圖
     */
    private void hideLoading() {
        btnRequest.set(true);
        isLoading.set(false);
    }

}
