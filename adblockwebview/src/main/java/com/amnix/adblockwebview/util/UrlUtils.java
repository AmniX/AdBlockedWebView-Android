package com.amnix.adblockwebview.util;

import java.net.MalformedURLException;
import java.net.URL;

public class UrlUtils {
    public static String getHost(String url) throws MalformedURLException {
        return new URL(url).getHost();
    }
}
