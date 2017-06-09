package com.tests;

import java.io.File;

/**
 * <p>Created by MontolioV on 07.06.17.
 */
public enum TestMatEnum {
    ROOT_CONTROL            (new File("test_mat/Firefox_wallpaper.png"), true),
    ROOT_COPY_BAD           (new File("test_mat/Firefox_wallpaper_copy_bad.png"), false),
    INNER_COPY_GOOD         (new File("test_mat/inner/Firefox_wallpaper_copy_good.png"), true),
    INNER2_COPY_GOOD        (new File("test_mat/inner2/Firefox_wallpaper_copy_good.png"), true),
    INNER2_COPY_BAD         (new File("test_mat/inner2/Firefox_wallpaper_copy_bad.png"), false),
    INNER2_INNER_COPY_GOOD  (new File("test_mat/inner2/inner/Firefox_wallpaper_copy_good.png"), true),
    INNER2_INNER_COPY_BAD   (new File("test_mat/inner2/inner/Firefox_wallpaper_copy_bad.png"), false);

    private final String sha256CheckSum;
    private final File file;

    TestMatEnum(File file, boolean isExactCopy) {
        this.file = file;
        if (isExactCopy) {
            this.sha256CheckSum = "7cd376319c008f8580c0c54f09a6a76fe3465e68daaf3539f168b88cb3bb7f91";
        } else {
            this.sha256CheckSum = "bc24bb4e2ebb6e9072787a657710905c5fc38d3044557022f2aa60076975377b";
        }
    }

    public String getSha256CheckSum() {
        return sha256CheckSum;
    }

    public File getFile() {
        return file;
    }
}
