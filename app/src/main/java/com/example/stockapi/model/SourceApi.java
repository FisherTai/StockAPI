package com.example.stockapi.model;

public class SourceApi {
    public static final int FIRSTPRICE = 0;
    public static final int ENDPRICE = 1;

    private String number = "";
    private String year = "";

    private String url = "";
    private final String twseUrl = "https://www.twse.com.tw/exchangeReport/STOCK_DAY?response=json&date=";
    private final String tpexUrl = "https://www.tpex.org.tw/web/stock/aftertrading/daily_trading_info/st43_result.php?d=";

    private void getPrice(String YY, int type, boolean isTWSE, String number, String year) {

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

    }
}
