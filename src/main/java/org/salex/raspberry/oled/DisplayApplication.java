package org.salex.raspberry.oled;

import cn.hutool.core.date.ChineseDate;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.format.DatePrinter;
import cn.hutool.core.img.FontUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.wiringpi.I2C;
import ink.dwx.Align;
import ink.dwx.Content;
import org.apache.commons.lang3.StringUtils;
import org.salex.raspberry.oled.dht11.DHT11;
import org.salex.raspberry.oled.dht11.DHT11Result;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
public class DisplayApplication {

    private static Display display;

    private static void show(List<List<Content>> screen) {
        try {
            AtomicInteger pageIndex = new AtomicInteger();
            pageIndex.set(0);

            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        synchronized (screen) {
                            int pageSize = screen.size();
                            if (pageSize > 0) {
                                display.displayString(screen.get(pageSize % pageIndex.getAndIncrement()));
                            }
                        }
                        ThreadUtil.safeSleep(30 * 1000);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void make(List<List<Content>> screen) {
        try {
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        List<Content> page1 = getPage1();
                        List<Content> page2 = getPage2();
                        synchronized (screen) {
                            screen.add(page2);
                            screen.add(page1);
                        }
                        ThreadUtil.safeSleep(30 * 1000);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            display = new Display(Constants.LCD_WIDTH_128, Constants.LCD_HEIGHT_64, GpioFactory.getInstance(), I2CFactory.getInstance(I2C.CHANNEL_1), 0x3c);
            display.begin();

            List<List<Content>> screen = new ArrayList<>();
            make(screen);
            show(screen);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Scanner scanner = new Scanner(System.in);
        while (true) {
            String opsn = scanner.next();
            if ("q".equals(opsn)) {
                break;
            }
            ThreadUtil.sleep(3 * 1000);
        }
    }

    private static final String[] NUM = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十", "十一", "十二"};

    public static String shu9() {
        Long betweenDay = DateUtil.between(DateUtil.parse("2021-12-21"), DateUtil.date(), DateUnit.DAY);
        if (betweenDay > 81) {
            return "";
        }
        return NUM[(betweenDay.intValue() / 9 + 1)] + "九第" + NUM[(betweenDay.intValue() % 9 + 1)] + "天";
    }

    // 正确的IP拿法，即优先拿site-local地址
    private static InetAddress getLocalHostLANAddress() throws UnknownHostException {
        try {
            InetAddress candidateAddress = null;
            // 遍历所有的网络接口
            for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); ) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                // 在所有的接口下再遍历IP
                for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {// 排除loopback类型地址
                        if (inetAddr.isSiteLocalAddress()) {
                            // 如果是site-local地址，就是它了
                            return inetAddr;
                        } else if (candidateAddress == null) {
                            // site-local类型的地址未被发现，先记录候选地址
                            candidateAddress = inetAddr;
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                return candidateAddress;
            }
            // 如果没有发现 non-loopback地址.只能用最次选的方案
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            return jdkSuppliedAddress;
        } catch (Exception e) {
            UnknownHostException unknownHostException = new UnknownHostException(
                    "Failed to determine LAN address: " + e);
            unknownHostException.initCause(e);
            throw unknownHostException;
        }
    }

    private static List<Content> getPage1() {
        List<Content> list = new ArrayList<>();
        try {
            Content ip = new Content(display.getGraphics2D(), "IP:" + getLocalHostLANAddress().getHostAddress(), FontUtil.createSansSerifFont(16), 0, 0);
            list.add(ip);

            ChineseDate date = new ChineseDate(DateUtil.date());
            String shu9Text = date.getChineseMonthName() + date.getChineseDay();
            String shu9 = shu9();
            if (StringUtils.isNotBlank(shu9)) {
                shu9Text += " " + shu9;
            }
            list.add(new Content(display.getGraphics2D(), shu9Text, FontUtil.createSansSerifFont(16), Align.ALIGN_LEFT, Align.ALIGN_BOTTOM));

            DHT11 dht11 = new DHT11(7);
            DHT11Result result = dht11.read();
            if (result.isValid()) {
                Content t = new Content(display.getGraphics2D(),
                        "温度:" + result.getTemperature(),
                        FontUtil.createSansSerifFont(16),
                        0,
                        (int) ip.getStrRect().getHeight());
                list.add(t);

                Content h = new Content(display.getGraphics2D(),
                        "湿度:" + result.getHumidity(),
                        FontUtil.createSansSerifFont(16),
                        0,
                        (int) t.getStrRect().getHeight());
                list.add(h);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return list;
    }

    private static List<Content> getPage2() {
        List<Content> list = new ArrayList<>();
        try {
            Content time = new Content(display.getGraphics2D(),
                    DateUtil.format(DateUtil.date(), "MM-dd HH:mm"),
                    FontUtil.createSansSerifFont(64), Align.ALIGN_CENTER, Align.ALIGN_CENTER);
            list.add(time);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    private static void t1() {
        try {
            final GpioController gpio = GpioFactory.getInstance();
            I2CBus i2c = I2CFactory.getInstance(I2C.CHANNEL_1);
            Display display = new Display(Constants.LCD_WIDTH_128, Constants.LCD_HEIGHT_64, gpio, i2c, 0x3c);
            display.begin();
            display.clear();
            display.displayString(0, "IP:" + getLocalHostLANAddress().getHostAddress());

            DHT11 dht11 = new DHT11(7);
            while (true) {
                DHT11Result result = dht11.read();
                if (result.isValid()) {
                    ChineseDate date = new ChineseDate(DateUtil.date());

                    String pressText = date.getChineseMonthName() + date.getChineseDay();
                    String shu9 = shu9();
                    if (StringUtils.isNotBlank(shu9)) {
                        pressText += " " + shu9;
                    }

                    display.displayString(0, "IP:" + getLocalHostLANAddress().getHostAddress(),
                            "温度:" + result.getTemperature(),
                            "湿度:" + result.getHumidity(),
                            pressText);
                }
                TimeUnit.SECONDS.sleep(30);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
