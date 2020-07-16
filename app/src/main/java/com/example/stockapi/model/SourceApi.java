package com.example.stockapi.model;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.example.stockapi.MainActivity;
import com.google.gson.Gson;
import com.orhanobut.logger.Logger;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SourceApi {
    private static final String TAG = "SourceApi";

    HashMap<String, String> MapData = new HashMap<>();




    //控制要獲取的是哪個價格
    public static final int FIRS_TPRICE = 0;
    public static final int END_PRICE = 1;

    //本地的上市上櫃清單
    private String TPEXlist;
    private String TWSElist;

    //使用者輸入的資料
    private String number = "";
    private String year = "";


    private String url = "";
    private final String twseUrl = "https://www.twse.com.tw/exchangeReport/STOCK_DAY?response=json&date=";
    private final String tpexUrl = "https://www.tpex.org.tw/web/stock/aftertrading/daily_trading_info/st43_result.php?d=";



    public SourceApi(Context context){
        getStockList(context);
    }


    //取得上市上櫃股票清單
    public void getStockList(Context context) {
        TPEXlist = readAssetsJson(context, "TPEX.txt");
        TWSElist = readAssetsJson(context, "TWSE.txt");
        MapData.put("TPEXlist", TWSElist);
        MapData.put("TWSElist", TWSElist);
    }

    public String getTPEXlist() {
        return TPEXlist;
    }

    public String getTWSElist() {
        return TWSElist;
    }

    public void getPrice(String year, int type, boolean isTWSE, String number, HttpCallback callback) {

        if (isTWSE && type == FIRS_TPRICE) {
            url = twseUrl + year + "0110&stockNo=" + number;
        } else if (isTWSE && type == END_PRICE) {
            url = twseUrl + year + "1201&stockNo=" + number;
        } else if (!isTWSE && type == FIRS_TPRICE) {
            year = String.valueOf(Integer.parseInt(year) - 1911);
            url = tpexUrl + year + "/01&stkno=" + number;
        } else if (!isTWSE && type == END_PRICE) {
            year = String.valueOf(Integer.parseInt(year) - 1911);
            url = tpexUrl + year + "/12&stkno=" + number;
        } else {
            return;
        }

        OkHttpClient client = new OkHttpClient().newBuilder().build();
        final Request request = new Request.Builder().url(url).build();
        Log.d("AAA", "getPrice: " + request.url());
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String result = response.body().string();
                callback.onSuccess(result);
            }
        });

    }


    public void getDividend(String num, String year, SourceNetDataCallback callback) {

        String url = "https://tw.stock.yahoo.com/d/s/dividend_" + num + ".html";
        try {
            final Document document = Jsoup.connect(url).get();
//            Logger.d(document.outerHtml()); //印出整個HTML頁面
            callback.SourceNetDataCallback(document);
        } catch (IOException ie) {
            Log.d(TAG, "jsoupGetText: ", ie);
        }
    }




    //取得本地asset資料
    private String readAssetsJson(Context context, String fileName) {
        AssetManager assetManager = context.getAssets();

        try {
            InputStream is = assetManager.open(fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder stringBuilder = new StringBuilder();
            String str;
            while ((str = br.readLine()) != null) {
                stringBuilder.append(str);
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public interface SourceNetDataCallback {
        void SourceNetDataCallback(Document data);
    }


    public interface HttpCallback {

        void onFailure(@NotNull IOException e);

        void onSuccess(@NotNull String result);
    }

}

