package com.blezede.compressor;

import java.io.IOException;
import java.io.InputStream;

/**
 * com.blezede.compressor
 * Time: 2019/3/27 11:21
 * Description:
 */
public interface InputStreamProvider {

    InputStream open() throws IOException;

    String getPath();
}
