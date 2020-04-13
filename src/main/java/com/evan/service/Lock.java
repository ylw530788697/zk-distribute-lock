package com.evan.service;

/**
 * @author evanYang
 * @version 1.0
 * @date 2020/04/13 09:31
 */
public interface Lock {
    boolean lock() throws Exception;

    boolean unlock();
}
