package io.sourceforge.uniqueoid;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * <p>Created by MontolioV on 07.06.17.
 */
public enum TestMatEnum {
    ROOT_CONTROL            ("io/sourceforge/uniqueoid/test_dir/Firefox_wallpaper.png", true),
    ROOT_COPY_BAD           ("io/sourceforge/uniqueoid/test_dir/Firefox_wallpaper_copy_bad.png", false),
    INNER_COPY_GOOD         ("io/sourceforge/uniqueoid/test_dir/inner/Firefox_wallpaper_copy_good.png", true),
    INNER2_COPY_GOOD        ("io/sourceforge/uniqueoid/test_dir/inner2/Firefox_wallpaper_copy_good.png", true),
    INNER2_COPY_BAD         ("io/sourceforge/uniqueoid/test_dir/inner2/Firefox_wallpaper_copy_bad.png", false),
    INNER2_INNER_COPY_GOOD  ("io/sourceforge/uniqueoid/test_dir/inner2/inner/Firefox_wallpaper_copy_good.png", true),
    INNER2_INNER_COPY_BAD   ("io/sourceforge/uniqueoid/test_dir/inner2/inner/Firefox_wallpaper_copy_bad.png", false),;


    private final String CHECKSUM;
    private final File FILE;

    TestMatEnum(String relativelyToClasspath, boolean isExactCopy) {
        this.FILE = getFile(relativelyToClasspath);
        if (isExactCopy) {
            this.CHECKSUM = "989850b8a02fc4528e7e04644d936a6946f67166159ec0a79b04b6e8ef89e072" + "_" +
                            "69ec50a94cc4f7cba88116ca22f234b68681ba7a" + "_" +
                            "718200";
        } else {
            this.CHECKSUM = "cad53d7a09f73482c0aab473d5d13f125c0921ea18a4bd648d33f534e0e981c7" + "_" +
                            "cec0ee282ea1d2fe428ecd762f8d397bf22d83b5" + "_" +
                            "1046743";
        }
    }

    public String getCheckSum() {
        return CHECKSUM;
    }

    public File getFile() {
        return FILE;
    }

    private File getFile(String relativelyToClasspath) {
        URI uri = null;
        try {
            uri = getClass().getClassLoader().getResource(relativelyToClasspath).toURI();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return new File(uri);
    }
}
