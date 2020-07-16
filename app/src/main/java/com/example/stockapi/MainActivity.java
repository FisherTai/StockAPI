package com.example.stockapi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.stockapi.model.Stock;
import com.example.stockapi.model.Tpex;
import com.example.stockapi.model.Twse;
import com.google.gson.Gson;
import com.orhanobut.logger.AndroidLogAdapter;
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
import java.math.BigDecimal;


import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private TextView first;
    private TextView end;
    private TextView result;
    private TextView tvCashDividend;
    private TextView tvCstockDividend;
    private EditText editNumber;
    private EditText editYear;
    private ProgressBar loadingbar;
    private Button btnRequest;

    private String number = "";
    private String year = "";
    private Gson gson = new Gson();

    final int FIRSTPRICE = 0;
    final int ENDPRICE = 1;

    private String TPEXlist;
    private String TWSElist;

    boolean cashDividendParsed = false;
    boolean stockDividendParsed = false;
    boolean getFrist = false;
    boolean getEnd = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Logger.addLogAdapter(new AndroidLogAdapter());

        findView();

        TPEXlist = readAssetsJson(this, "TPEX.txt");
        TWSElist = readAssetsJson(this, "TWSE.txt");

        btnRequest.setOnClickListener(v -> {
            number = editNumber.getText().toString();
            year = editYear.getText().toString();
            boolean isTWSE = (TWSElist.contains(number));
            showLoading();
            //頻繁請求會被證交所擋IP，需要延遲操作
            new Handler().postDelayed(() -> {
                getPrice(year, FIRSTPRICE, isTWSE);
                getPrice(year, ENDPRICE, isTWSE);
            }, 3000);
            new Thread(() -> jsoupGetText(number, year)).start();
        });
    }

    private void findView() {
        tvCashDividend = findViewById(R.id.cashDividend);
        tvCstockDividend = findViewById(R.id.stockDividend);
        first = findViewById(R.id.firstPrice);
        end = findViewById(R.id.endPrice);
        result = findViewById(R.id.result);
        editYear = findViewById(R.id.editYear);
        editNumber = findViewById(R.id.editNumber);
        loadingbar = findViewById(R.id.loading_progress);
        btnRequest = findViewById(R.id.button);
    }


    private void getPrice(String YY, int type, boolean isTWSE) {
        String url = "";
        String twseUrl = "https://www.twse.com.tw/exchangeReport/STOCK_DAY?response=json&date=";
        String tpexUrl = "https://www.tpex.org.tw/web/stock/aftertrading/daily_trading_info/st43_result.php?d=";

        if (isTWSE && type == FIRSTPRICE) {
            url = twseUrl + YY + "0110&stockNo=" + number;
        } else if (isTWSE && type == ENDPRICE) {
            url = twseUrl + YY + "1201&stockNo=" + number;
        } else if (!isTWSE && type == FIRSTPRICE) {
            YY = String.valueOf(Integer.parseInt(YY) - 1911);
            url = tpexUrl + YY + "/01&stkno=" + number;
        } else if (!isTWSE && type == ENDPRICE) {
            YY = String.valueOf(Integer.parseInt(YY) - 1911);
            url = tpexUrl + YY + "/12&stkno=" + number;
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
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(MainActivity.this, "無法聯繫資料來源", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String result = response.body().string();
                Log.d(TAG, "onResponse: " + result);
                Stock stock;
                try {
                    String stat = new JSONObject(result).optString("stat");
                    if (TextUtils.isEmpty(stat) || !stat.equals("OK")) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "取得資料失敗 : " + stat, Toast.LENGTH_SHORT).show();
                            hideLoading();
                        });
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
                runOnUiThread(() -> {
                    if (type == FIRSTPRICE) {
                        first.setText(stock.getFirstPrice());
                        getFrist = true;
                        setResult();
                    } else {
                        end.setText(stock.getEndPrice());
                        getEnd = true;
                        setResult();
                    }

                    if (getEnd && getFrist) {
                        hideLoading();
                    }
                });
            }
        });

    }


    private void setResult() {
        if (getFrist && getEnd) {
            double firstPrice = Double.parseDouble(first.getText().toString());
            double endPrice = Double.parseDouble(end.getText().toString());
            double res = (endPrice - firstPrice) / firstPrice * 100;
            BigDecimal bd = new BigDecimal(res);
            res = bd.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();


            result.setText(res + "%");
        }
    }


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


    private void jsoupGetText(String num, String year) {
        int CashDIndex = 2;
        int StockDIndex = 5;
        double cashDividend = 0.0d;
        double stockDividend = 0.0d;

        String url = "https://tw.stock.yahoo.com/d/s/dividend_" + num + ".html";
        try {
            final Document document = Jsoup.connect(url).get();
//            Logger.d(document.outerHtml()); //印出整個HTML頁面
            for (Element row : document.select("td > table:nth-child(2) > tbody > tr")) {
                if (row.text().contains(year)) {
                    Logger.d(row.cssSelector());
                    Logger.d(row.text());
                    try {
                        cashDividend = ParseCashDividend(row.getElementsByIndexEquals(CashDIndex).text());
                        stockDividend = ParseStockDividend(row.getElementsByIndexEquals(StockDIndex).text());

                        runOnUiThread(() -> {
                            tvCashDividend.setText(row.getElementsByIndexEquals(CashDIndex).text());
                            tvCstockDividend.setText(row.getElementsByIndexEquals(StockDIndex).text());
                        });
                        Logger.d("現金:" + cashDividend + " 股票股利:" + stockDividend);
                    } catch (NumberFormatException nfe) {
                        Logger.d("不正常數值，進行修正");
                        String dividend = "";
                        if (!cashDividendParsed) {
                            dividend = row.getElementsByIndexEquals(CashDIndex).text();
                            dividend = dividend.substring(dividend.length() - 5);
                            cashDividend = ParseCashDividend(dividend);
                        }
                        if (!stockDividendParsed) {
                            dividend = row.getElementsByIndexEquals(StockDIndex).text();
                            dividend = dividend.substring(dividend.length() - 5);
                            stockDividend = ParseCashDividend(dividend);
                        }
                        final String cd = String.valueOf(cashDividend);
                        final String sd = String.valueOf(stockDividend);
                        runOnUiThread(() -> {
                            tvCashDividend.setText(cd);
                            tvCstockDividend.setText(sd);
                        });
                        Logger.d("現金:" + cashDividend + " 股票股利:" + stockDividend);
                    }
                }
            }
        } catch (IOException ie) {
            Log.d(TAG, "jsoupGetText: ", ie);
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
        btnRequest.setClickable(false);
        cashDividendParsed = false;
        stockDividendParsed = false;
        getFrist = false;
        getEnd = false;
        tvCashDividend.setText("");
        tvCstockDividend.setText("");
        first.setText("");
        end.setText("");
        result.setText("");
        loadingbar.setVisibility(View.VISIBLE);
    }

    /**
     * 隱藏讀取圖
     */
    public void hideLoading() {
        btnRequest.setClickable(true);
        loadingbar.setVisibility(View.GONE);
    }
}