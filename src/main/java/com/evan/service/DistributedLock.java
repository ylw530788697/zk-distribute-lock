package com.evan.service;

import java.util.concurrent.TimeUnit;

/**
 * @author evanYang
 * @version 1.0
 * @date 2020/04/13 09:37
 */
public interface DistributedLock {
    /**
     * 获取锁，如果没有得到就等待
     */

    public void acquire() throws Exception;

    /**
     *             * 获取锁，直到超时
     * <p>
     *             * @param time超时时间
     * <p>
     *             * @param unit time参数的单位
     * <p>
     *             * @return是否获取到锁
     * <p>
     *             * @throws Exception
     * <p>
     *            
     */
    public boolean acquire(long time, TimeUnit unit) throws Exception;


    /**
     *              * 释放锁
     * <p>
     *              * @throws Exception
     * <p>
     *             
     */
    public void release() throws Exception;

}
