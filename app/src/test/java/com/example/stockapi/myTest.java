package com.example.stockapi;

import android.util.Log;

import com.orhanobut.logger.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

import java.io.IOException;

public class myTest {
    private static final String TAG = "myTest";

    @Test
    public void jsoupGetText() {
        System.out.println("Test");
        String url = "https://tw.stock.yahoo.com/d/s/dividend_" + "0050" + ".html";
        try {
            final Document document = Jsoup.connect(url).get();
//            System.out.println(document.outerHtml());
            int count = 1;

            for (Element row : document.select(" td > table:nth-child(2) > tbody > tr")) {
                if(row.text().contains("2018")){
                    System.out.println(row.cssSelector());
                    System.out.println("["+count+"]"+"現金:"+ row.getElementsByIndexEquals(2).text() + " 股票股利:" + row.getElementsByIndexEquals(5).text());
                    count++;
                }
            }
            System.out.println("END");
        } catch (IOException ie) {
            Log.d(TAG, "jsoupGetText: ", ie);
        }
    }

}
