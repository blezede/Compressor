package com.blezede.compressor;

import java.util.List;

/**
 * com.blezede.compressor
 * Time: 2019/3/27 13:43
 * Description:
 */
public interface CompressListener {

    /**
     * Fired when a compression returns successfully, override to handle in your own code
     */
    void onSuccess(String dest);

    /**
     * Fired when a compression returns failed, override to handle in your own code
     */
    void onFiled(String src) ;
}
