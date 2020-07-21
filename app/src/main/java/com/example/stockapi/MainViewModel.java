package com.example.stockapi;

import android.app.Application;
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
    private AtomicReference<Double> cashDividend = new AtomicReference<>(0.0d);
    private AtomicReference<Double> stockDividend = new AtomicReference<>(0.0d);
    private String firstPrice = "";
    private String endPrice = "";


    //判断獲取完成
    private boolean getFrist = false;
    private boolean getEnd = false;
    private boolean cashDividendParsed = false;
    private boolean stockDividendParsed = false;


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
                    if (isTWSE && !stat.equals("OK")) {
                        hideLoading();
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
                    setResult(firstPrice, endPrice, cashDividend.get(), stockDividend.get());
                } else {
                    endPrice = stock.getEndPrice();
                    tvEnd.set(stock.getEndPrice());
                    getEnd = true;
                    setResult(firstPrice, endPrice, cashDividend.get(), stockDividend.get());
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
                        cashDividendSet(ParseCashDividend(row.getElementsByIndexEquals(CashDIndex).text()));
                        stockDividendSet(ParseStockDividend(row.getElementsByIndexEquals(StockDIndex).text()));
                        Log.d(TAG, "cashDividendSet: " + cashDividend.get());
                        //輸入前設定格式
                        tvCashDividend.set(String.valueOf(new BigDecimal(cashDividend.get()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
                        tvStockDividend.set(String.valueOf(new BigDecimal(stockDividend.get()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));

                        Log.d(TAG, "現金:" + row.getElementsByIndexEquals(CashDIndex).text() +
                                " 股票股利:" + row.getElementsByIndexEquals(StockDIndex).text());
                    } catch (NumberFormatException nfe) {
                        Log.d(TAG, "不正常數值，進行修正");
                        String dividend = "";
                        if (!cashDividendParsed) {
                            dividend = row.getElementsByIndexEquals(CashDIndex).text();
                            dividend = dividend.substring(dividend.length() - 5);

                            cashDividendSet(ParseCashDividend(dividend));
                        }
                        if (!stockDividendParsed) {
                            dividend = row.getElementsByIndexEquals(StockDIndex).text();
                            dividend = dividend.substring(dividend.length() - 5);

                            stockDividendSet(ParseStockDividend(dividend));
                        }
                        final String cd = String.valueOf(new BigDecimal(cashDividend.get()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
                        final String sd = String.valueOf(new BigDecimal(stockDividend.get()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
                        tvCashDividend.set(cd);
                        tvStockDividend.set(sd);

                        Log.d(TAG, "GetDividend: " + "現金:" + cashDividend + " 股票股利:" + stockDividend);
                    }
                }
            }
        });
    }

    /**
     * 計算報酬率:(收盤價+現金股利-開盤價)/開盤價 + (收盤價*100*現金股利)
     *
     * @param sFirstPrice   開盤價:String
     * @param sEndPrice     收盤價:String
     * @param cashDividend  現金配息
     * @param stockDividend 股利配息
     */
    private void setResult(String sFirstPrice, String sEndPrice, double cashDividend, double stockDividend) {
        if (getFrist && getEnd && cashDividendParsed && stockDividendParsed) {
            double firstPrice = Double.parseDouble(sFirstPrice);
            double endPrice = Double.parseDouble(sEndPrice);
            double res = ((endPrice + cashDividend - firstPrice) + (endPrice * stockDividend * 0.1)) / (firstPrice) * 100;
            BigDecimal bd = new BigDecimal(res);
            res = bd.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            result.set(res + "%");
        }
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
        cashDividend = new AtomicReference<>(0.0d);
        stockDividend = new AtomicReference<>(0.0d);
    }

    /**
     * 隱藏讀取圖
     */
    private void hideLoading() {
        btnRequest.set(true);
        isLoading.set(false);
    }

    /**
     * 轉換成Double格式。
     * 使用此方法時，需捕捉NumberFormatException進行處理
     */
    private double ParseCashDividend(String cashDividend) throws NumberFormatException {
        double Parsed = Double.parseDouble(cashDividend);
        cashDividendParsed = true;
        return Parsed;
    }

    private double ParseStockDividend(String stockDividend) throws NumberFormatException {
        double Parsed = Double.parseDouble(stockDividend);
        stockDividendParsed = true;
        return Parsed;
    }

    /**
     * 考慮到同年多次配息的狀況，累加結果
     *
     * @param Temp
     */
    private void cashDividendSet(double Temp) {
        cashDividend.set(cashDividend.get() + Temp);
    }

    private void stockDividendSet(double Temp) {
        stockDividend.set(stockDividend.get() + Temp);
    }
}
