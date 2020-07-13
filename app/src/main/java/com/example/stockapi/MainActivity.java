package com.example.stockapi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;

import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;


import org.jetbrains.annotations.NotNull;


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
    String twseUrl = "https://www.twse.com.tw/exchangeReport/STOCK_DAY?response=json&date=";
    String tpexUrl  = "https://www.tpex.org.tw/web/stock/aftertrading/daily_trading_info/st43_result.php?d=";
    private static final String TAG = "MainActivity";
    private TextView first;
    private TextView end;
    private TextView result;
    EditText editNumber;
    EditText editYear;

    boolean getFrist = false;
    boolean getEnd = false;

    String number = "";
    Gson gson = new Gson();

    final int FIRSTPRICE = 0;
    final int ENDPRICE = 1;

    private String TPEXlist;
    private String TWSElist;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findView();

        TPEXlist = readAssetsJson(this,"TPEX.txt");
        TWSElist = readAssetsJson(this,"TWSE.txt");

        findViewById(R.id.button).setOnClickListener(v -> {
            number = editNumber.getText().toString();
            boolean isTWSE = (TWSElist.contains(number));
            getPrice(editYear.getText().toString(), FIRSTPRICE,isTWSE);
            getPrice(editYear.getText().toString(), ENDPRICE,isTWSE);
        });


    }

    private void findView() {
        first = findViewById(R.id.firstPrice);
        end = findViewById(R.id.endPrice);
        result = findViewById(R.id.result);
        editYear = findViewById(R.id.editYear);
        editNumber = findViewById(R.id.editNumber);
    }


    private void getPrice(String YY, int type,boolean isTWSE) {

        getFrist = false;
        getEnd = false;
        String url = "";

        if (isTWSE && type == FIRSTPRICE) {
            url = twseUrl + YY + "0110" + "&stockNo=" + number;
        } else if ( isTWSE && type == ENDPRICE) {
            url = twseUrl + YY + "1231" + "&stockNo=" + number;
        } else if(!isTWSE && type == FIRSTPRICE) {
            YY = String.valueOf(Integer.parseInt(YY)-1911);
            url = tpexUrl + YY + "/01" + "&stkno=" + number;
        } else if(!isTWSE && type == ENDPRICE) {
            YY = String.valueOf(Integer.parseInt(YY)-1911);
            url = tpexUrl + YY + "/12" + "&stkno=" + number;
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

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String result = response.body().string();
                Log.d(TAG, "onResponse: " + result);

                Stock stock;

                if(isTWSE) {
                     stock = gson.fromJson(result, Twse.class);
                }else {
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
                });
            }
        });
    }


    private void setResult() {
        if (getFrist && getEnd) {
            double firstPrice = Double.parseDouble(first.getText().toString());
            double endPrice = Double.parseDouble(end.getText().toString());
            double res = (endPrice - firstPrice) / firstPrice*100;
            BigDecimal bd   =   new BigDecimal(res);
            res  =  bd.setScale(2,   BigDecimal.ROUND_HALF_UP).doubleValue();



            result.setText(res+"%");
        }
    }




    private String readAssetsJson(Context context,String fileName) {
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
}