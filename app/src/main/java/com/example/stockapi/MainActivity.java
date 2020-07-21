package com.example.stockapi;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.example.stockapi.databinding.ActivityMainBinding;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;

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
            String number = binding.editNumber.getText().toString().trim();
            String year = binding.editYear.getText().toString().trim();

            if(TextUtils.isEmpty(number) || TextUtils.isEmpty(year)){
                binding.resultCommon.setText("資料不得空白");
                return;
            }else if(Integer.parseInt(year)<2012){
                binding.resultCommon.setText("查詢年份必須為2012年以後");
                return;
            }

            viewModel.showLoading();
                //頻繁請求會被證交所擋IP，需要延遲操作
            new Handler().postDelayed(() -> {
                viewModel.getPrice(year, FIRS_TPRICE, number);
                viewModel.getPrice(year, END_PRICE, number);
            }, 2500);

            new Thread(() ->viewModel.GetDividend(number, year)).start();
        });
    }

}