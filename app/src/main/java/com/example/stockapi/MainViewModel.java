package com.example.stockapi;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

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
    public final ObservableField<String> tvCstockDividend = new ObservableField<>();
    public final ObservableField<String> tvFirst = new ObservableField<>();
    public final ObservableField<String> tvEnd = new ObservableField<>();

    private Context mContext;

    private SourceApi sourceApi;


    //判断獲取完成
    boolean getFrist = false;
    boolean getEnd = false;
    boolean cashDividendParsed = false;
    boolean stockDividendParsed = false;


    private Gson gson = new Gson();

    public MainViewModel(@NonNull Application application) {
        super(application);
        application.getAssets();
        sourceApi = new SourceApi(application);
    }

    public void getPrice(String YY, int type, String number) {

        boolean isTWSE = (sourceApi.getTWSElist().contains(number));

        sourceApi.getPrice(YY, type, isTWSE, number, new SourceApi.HttpCallback() {
            @Override
            public void onFailure(@NotNull IOException e) {
                Log.d(TAG, "onFailure: ", e);
//                runOnUiThread(() -> {
//                    hideLoading();
//                    Toast.makeText(MainActivity.this, "無法聯繫資料來源", Toast.LENGTH_SHORT).show();
//                });

            }

            @Override
            public void onSuccess(@NotNull String result) {
                Log.d(TAG, "onResponse: " + result);
                Stock stock;
                try {
                    String stat = new JSONObject(result).optString("stat");
                    if (TextUtils.isEmpty(stat) || !stat.equals("OK")) {
//                        mContext.runOnUiThread(() -> {
//                            Toast.makeText(mContext, "取得資料失敗 : " + stat, Toast.LENGTH_SHORT).show();
//                            hideLoading();
//                        });
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
//                runOnUiThread(() -> {
                if (type == FIRS_TPRICE) {
                    tvFirst.set(stock.getFirstPrice());
                    getFrist = true;
                    setResult(stock.getFirstPrice(), stock.getEndPrice());
                } else {
                    tvEnd.set(stock.getEndPrice());
                    getEnd = true;
                    setResult(stock.getFirstPrice(), stock.getEndPrice());
                }

                if (getEnd && getFrist) {
                    hideLoading();
                }
//                });
            }
        });
    }

    public void GetDividend(String number, String year) {

        int CashDIndex = 2;
        int StockDIndex = 5;
        AtomicReference<Double> cashDividend = new AtomicReference<>(0.0d);
        AtomicReference<Double> stockDividend = new AtomicReference<>(0.0d);

        sourceApi.getDividend(number, year, data -> {
            for (Element row : data.select("td > table:nth-child(2) > tbody > tr")) {
                if (row.text().contains(year)) {
//                    Logger.d(row.cssSelector());
//                    Logger.d(row.text());
                    try {
                        cashDividend.set(ParseCashDividend(row.getElementsByIndexEquals(CashDIndex).text()));
                        stockDividend.set(ParseStockDividend(row.getElementsByIndexEquals(StockDIndex).text()));

//                        runOnUiThread(() -> {
                        tvCashDividend.set(row.getElementsByIndexEquals(CashDIndex).text());
                        tvCstockDividend.set(row.getElementsByIndexEquals(StockDIndex).text());
//                        });

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
//                        runOnUiThread(() -> {
                        tvCashDividend.set(cd);
                        tvCstockDividend.set(sd);
//                        });
                        Logger.d("現金:" + cashDividend + " 股票股利:" + stockDividend);
                    }
                }
            }
        });
    }


    private void setResult(String sFirstPrice, String sEndPrice) {
        if (getFrist && getEnd) {
            double firstPrice = Double.parseDouble(sFirstPrice);
            double endPrice = Double.parseDouble(sEndPrice);
            double res = (endPrice - firstPrice) / firstPrice * 100;
            BigDecimal bd = new BigDecimal(res);
            res = bd.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            result.set(res + "%");
        }
    }

    public double ParseCashDividend(String cashDividend) {
        double Parsed = Double.parseDouble(cashDividend);
        cashDividendParsed = true;
        return Parsed;
    }

    public double ParseStockDividend(String stockDividend) {
        double Parsed = Double.parseDouble(stockDividend);
        stockDividendParsed = true;
        return Parsed;
    }


    /**
     * 顯示讀取圖，同時初始化
     */
    public void showLoading() {
        btnRequest.set(false);
        cashDividendParsed = false;
        stockDividendParsed = false;
        getFrist = false;
        getEnd = false;
        tvCashDividend.set("");
        tvCstockDividend.set("");
        tvFirst.set("");
        tvEnd.set("");
        result.set("");
        isLoading.set(true);
    }

    /**
     * 隱藏讀取圖
     */
    public void hideLoading() {
        btnRequest.set(true);
        isLoading.set(false);
    }


}
