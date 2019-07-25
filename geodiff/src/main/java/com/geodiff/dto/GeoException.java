package com.geodiff.dto;

public class GeoException extends  Exception {

    public GeoException(String str){
        super(str);
    }

    public GeoException(String s, Throwable t){ super(s,t);}

}
