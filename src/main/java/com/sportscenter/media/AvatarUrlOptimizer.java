package com.sportscenter.media;

public final class AvatarUrlOptimizer {
    private AvatarUrlOptimizer() {}

    public static String optimize(String url) {
        if (url == null || url.isBlank() || !url.contains("res.cloudinary.com/") || !url.contains("/upload/")) return url;
        int upload = url.indexOf("/upload/");
        if (url.contains("f_auto")) return url;
        String tail = url.substring(upload + "/upload/".length());
        return url.substring(0, upload + "/upload/".length()) + "f_auto,q_auto/" + tail;
    }
}
