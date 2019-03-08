package com.example.lofi;

public class Song {

    protected String trackID;
    protected float v;
    protected float a;
    protected String artist;
    protected String title;
    protected float t;

    public Song(String[] fields) {
        this.trackID = fields[0];
        this.v = Float.parseFloat(fields[1]);
        this.a = Float.parseFloat(fields[2]);
        this.artist = fields[3];
        this.title = fields[4];
        this.t = Float.parseFloat(fields[5]);
    }
}
