package com.example.musicstreaming;

public class Song {
    private String songName, songFileURL, songImageURL;

    public Song(String songName, String songFileURL, String songImageURL) {
        this.songName = songName;
        this.songFileURL = songFileURL;
        this.songImageURL = songImageURL;
    }

   //firebase requires default constructor
    public Song(){}

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public String getSongFileURL() {
        return songFileURL;
    }

    public void setSongFileURL(String songFileURL) {
        this.songFileURL = songFileURL;
    }

    public String getSongImageURL() {
        return songImageURL;
    }

    public void setSongImageURL(String songImageURL) {
        this.songImageURL = songImageURL;
    }
}
