package com.trc.upgrade.util;


import com.trc.upgrade.AppCheckUtil;

import org.junit.Before;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

/**
 * JiangyeLin on 2018/11/07
 * 版本号校验测试
 */
public class VersionCompareUtilTest {

    @Before
    public void setUp() {
    }

    @Test
    public void regexCompare() {
        Pattern pattern = Pattern.compile("[1-9]\\d*(\\.\\d+)*(-[0-9a-zA-Z.]*)?");
        assertEquals(true, pattern.matcher("1.0.0").matches());
        assertEquals(true, pattern.matcher("1.1.1.0").matches());
        assertEquals(true, pattern.matcher("1.0.11.11").matches());
        assertEquals(true, pattern.matcher("1.1.1.0-alpha").matches());
        assertEquals(true, pattern.matcher("1.1.1.0-alpha.1").matches());
        assertEquals(true, pattern.matcher("1.1.1.0-alpha.beta").matches());
        assertEquals(true, pattern.matcher("1.1.1.0-alpha.beta.1").matches());
        assertEquals(true, pattern.matcher("50").matches());

        assertEquals(false, pattern.matcher("1.1.1.0--alpha").matches());
        assertEquals(false, pattern.matcher("1.1.1.0alpha").matches());
        assertEquals(false, pattern.matcher("0.1.1.0").matches());
        assertEquals(false, pattern.matcher("1.1.1.0  rc1").matches());
        assertEquals(false, pattern.matcher("hahah").matches());
        assertEquals(false, pattern.matcher(" ").matches());
    }

    /**
     * 比较版本
     * 范例：1.0.0-alpha < 1.0.0-alpha.1 < 1.0.0-alpha.beta < 1.0.0-beta < 1.0.0-beta.2 < 1.0.0-beta.11 < 1.0.0-rc.1 < 1.0.0。
     * <p>
     * 0    一致 || 版本号不合法提前返回
     * -1   当前版本<新版本
     * 1    当前版本>新版本
     */
    @Test
    public void compareVersion() {
        assertEquals(0, AppCheckUtil.compareVersion("1.0.0", "1.0.0"));
        assertEquals(-1, AppCheckUtil.compareVersion("9.9.9", "10.0.0"));
        assertEquals(-1, AppCheckUtil.compareVersion("9.9.9", "9.10.8"));
        assertEquals(-1, AppCheckUtil.compareVersion("2.3.9", "2.3.10"));
        assertEquals(-1, AppCheckUtil.compareVersion("1.0.0", "1.0.0.0"));
        assertEquals(-1, AppCheckUtil.compareVersion("1.0.0", "1.0.0.8"));

        assertEquals(1, AppCheckUtil.compareVersion("1.0.0", "1.0.0-alpha"));
        assertEquals(1, AppCheckUtil.compareVersion("1.0.0.2-alpha", "1.0.0-alpha"));
        assertEquals(-1, AppCheckUtil.compareVersion("1.0.0-alpha", "1.0.0-alpha.1"));
        assertEquals(-1, AppCheckUtil.compareVersion("1.0.0-alpha", "1.0.0-    alpha.1"));
        assertEquals(-1, AppCheckUtil.compareVersion("1.0.0-alpha.1", "1.0.0-alpha.beta"));
        assertEquals(-1, AppCheckUtil.compareVersion("1.0.0-beta", "1.0.0-beta.2"));
        assertEquals(-1, AppCheckUtil.compareVersion("1.0.0-beta.2", "1.0.0-beta.11"));
        assertEquals(-1, AppCheckUtil.compareVersion("1.0.0-beta.11", "1.0.0-rc.1"));
        assertEquals(-1, AppCheckUtil.compareVersion("1.0.0-rc.1", "1.0.0"));

        assertEquals(0, AppCheckUtil.compareVersion("1.0.0 rc.1", "1.0.0"));
        assertEquals(-1, AppCheckUtil.compareVersion("10", "20"));
        assertEquals(0, AppCheckUtil.compareVersion("1.0.0", null));
        assertEquals(0, AppCheckUtil.compareVersion(null, "1.0.0"));
    }
}