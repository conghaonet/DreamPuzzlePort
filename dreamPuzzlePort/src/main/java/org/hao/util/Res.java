package org.hao.util;

import java.util.ListResourceBundle;

public class Res extends ListResourceBundle {
    static final Object[][] contents = new String[][]{
	{ "r", "r" },
	{ "bufbitlen_size_must_0", "bufbitlen size must >= 0" },
	{ "rw", "rw" },
	{ "BufferedRandomAccess", "BufferedRandomAccessFile Copy & Write File: " },
	{ "FileSize_", "    FileSize: " },
	{ "_KB_", " (KB)    " },
	{ "Spend_", "Spend: " },
	{ "_s_", "(s)" },
	{ "DataBufferedios_Copy", "DataBufferedios Copy & Write File: " }};
    public Object[][] getContents() {
        return contents;
    }
}