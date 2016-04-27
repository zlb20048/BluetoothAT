package com.service.bluetooth;

import java.io.FileDescriptor;
import java.io.PrintWriter;

public class MediaInfo
{
    String name;
    String title;
    String artist;
    String album;
    int track;
    String genre;
    String composer;

    // 	for AT*CGPS
    int playActivity = BluetoothDevice.PLAYER_STATUS_STOP; // only for play, pause, stop
    int playState;
    String source;
    int position, duration;

    //MetaData Constants.
    static final int MEDIA_METADATA_NAME = 0x01;
    static final int MEDIA_METADATA_TITTLE = 0x02;
    static final int MEDIA_METADATA_ARTIST = 0x04;
    static final int MEDIA_METADATA_ALBUM = 0x08;
    static final int MEDIA_METADATA_TRACK = 0x10;
    static final int MEDIA_METADATA_GENRE = 0x20;
    static final int MEDIA_METADATA_COMPOSER = 0x40;

    void reset()
    {
        name = title = artist = album = genre = composer = null;
        track = -1;
        playActivity = BluetoothDevice.PLAYER_STATUS_STOP;
    }

    void clearMetaData()
    {
        name = title = artist = album = genre = composer = null;
        track = -1;
    }


    void updateFrom(MediaInfo mediaInfo, int mask)
    {
        if ((mask & MEDIA_METADATA_ALBUM) != 0)
        {
            this.album = mediaInfo.album;
        }
        if ((mask & MEDIA_METADATA_ARTIST) != 0)
        {
            this.artist = mediaInfo.artist;
        }
        if ((mask & MEDIA_METADATA_TRACK) != 0)
        {
            this.track = mediaInfo.track;
        }
        if ((mask & MEDIA_METADATA_COMPOSER) != 0)
        {
            this.composer = mediaInfo.composer;
        }
        if ((mask & MEDIA_METADATA_GENRE) != 0)
        {
            this.genre = mediaInfo.genre;
        }
        if ((mask & MEDIA_METADATA_TITTLE) != 0)
        {
            this.title = mediaInfo.title;
        }
    }

    @Override
    public String toString()
    {
        return "MediaInfo [name=" + name + ", title=" + title + ", artist=" + artist + ", album=" + album + ", track=" + track + ", genre=" + genre + ", composer=" + composer + "]";
    }

    boolean setPlayActivity(int status)
    {
        if (status == playActivity)
        {
            return false;
        }
        playActivity = status;
        return true;
    }

    int getPlayActivity()
    {
        return playActivity;
    }

    void dump(FileDescriptor fd, PrintWriter pw)
    {
        pw.println("Current Media Info state:");
        pw.println("    name : " + name);
        pw.println("    title : " + title);
        pw.println("    artist : " + artist);
        pw.println("    album : " + album);
        pw.println("    track : " + track);
        pw.println("    composer : " + composer);
        pw.println("    genre : " + genre);
    }
}
