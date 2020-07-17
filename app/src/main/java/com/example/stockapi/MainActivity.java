package com.example.stockapi;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

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

import com.example.stockapi.databinding.ActivityMainBinding;
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

import static com.example.stockapi.model.SourceApi.END_PRICE;
import static com.example.stockapi.model.SourceApi.FIRS_TPRICE;

public class MainActivity extends AppCompatActivity {

    //Databinding
    private MainViewModel viewModel;
    private ActivityMainBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        viewModel = new MainViewModel(getApplication());
//        這裡使用DataBindingUtil的setContentView
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setViewModel(viewModel);

//        初始化LOG套件
        Logger.addLogAdapter(new AndroidLogAdapter());

        binding.btnRequest.setOnClickListener(v -> {
            viewModel.showLoading();
            String number = binding.editNumber.getText().toString();
            String year = binding.editYear.getText().toString();
            //頻繁請求會被證交所擋IP，需要延遲操作
            new Handler().postDelayed(() -> {
                viewModel.getPrice(year, FIRS_TPRICE, number);
                viewModel.getPrice(year, END_PRICE, number);
            }, 2500);

            new Thread(() ->viewModel.GetDividend(number, year)).start();
        });
    }

}