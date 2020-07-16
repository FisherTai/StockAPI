package com.example.stockapi.model;

public class Twse implements Stock{

    private String stat;
    private String date;
    private String title;
    private String[] fields;
    private String[][] data;
    private String[] notes;

    public String getStat() {
        return stat;
    }

    public void setStat(String stat) {
        this.stat = stat;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String[] getFields() {
        return fields;
    }

    public void setFields(String[] fields) {
        this.fields = fields;
    }

    @Override
    public String getFirstPrice() {
        return data[0][3];
    }
    @Override
    public String getEndPrice() {
        return data[data.length - 1][6];
    }


    public String[][] getData() {
        return data;
    }

    public void setData(String[][] data) {
        this.data = data;
    }

    public String[] getNotes() {
        return notes;
    }

    public void setNotes(String[] notes) {
        this.notes = notes;
    }

}
