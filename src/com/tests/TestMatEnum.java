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

    private final String SHA_256_CHECKSUM;
    private final File FILE;

    TestMatEnum(File file, boolean isExactCopy) {
        this.FILE = file;
        if (isExactCopy) {
            this.SHA_256_CHECKSUM = "7cd376319c008f8580c0c54f09a6a76fe3465e68daaf3539f168b88cb3bb7f91";
        } else {
            this.SHA_256_CHECKSUM = "bc24bb4e2ebb6e9072787a657710905c5fc38d3044557022f2aa60076975377b";
        }
    }

    public String getSha256CheckSum() {
        return SHA_256_CHECKSUM;
    }

    public File getFile() {
        return FILE;
    }
}
