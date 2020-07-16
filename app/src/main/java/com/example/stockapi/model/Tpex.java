package com.example.stockapi.model;

public class Tpex implements Stock{


    private String stkNo;
    private String stkName;
    private String showListPriceNote;
    private String showListPriceLink;
    private String reportDate;
    private int iTotalRecords;
    private String[][] aaData;

    @Override
    public String getFirstPrice() {
        return aaData[0][3];
    }

    @Override
    public String getEndPrice() {
        return aaData[aaData.length - 1][6];
    }

    public String getStkNo() {
        return stkNo;
    }

    public void setStkNo(String stkNo) {
        this.stkNo = stkNo;
    }

    public String getStkName() {
        return stkName;
    }

    public void setStkName(String stkName) {
        this.stkName = stkName;
    }

    public String getShowListPriceNote() {
        return showListPriceNote;
    }

    public void setShowListPriceNote(String showListPriceNote) {
        this.showListPriceNote = showListPriceNote;
    }

    public String getShowListPriceLink() {
        return showListPriceLink;
    }

    public void setShowListPriceLink(String showListPriceLink) {
        this.showListPriceLink = showListPriceLink;
    }

    public String getReportDate() {
        return reportDate;
    }

    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }

    public int getiTotalRecords() {
        return iTotalRecords;
    }

    public void setiTotalRecords(int iTotalRecords) {
        this.iTotalRecords = iTotalRecords;
    }

    public String[][] getAaData() {
        return aaData;
    }

    public void setAaData(String[][] aaData) {
        this.aaData = aaData;
    }
}
