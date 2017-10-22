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
            this.SHA_256_CHECKSUM = "989850b8a02fc4528e7e04644d936a6946f67166159ec0a79b04b6e8ef89e072";
        } else {
            this.SHA_256_CHECKSUM = "cad53d7a09f73482c0aab473d5d13f125c0921ea18a4bd648d33f534e0e981c7";
        }
    }

    public String getSha256CheckSum() {
        return SHA_256_CHECKSUM;
    }

    public File getFile() {
        return FILE;
    }
}
