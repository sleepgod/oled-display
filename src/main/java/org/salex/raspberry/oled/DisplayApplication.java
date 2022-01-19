package org.salex.raspberry.oled;

import cn.hutool.core.date.ChineseDate;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class DisplayApplication {

    private static Display display;

    private static void show(Map<Integer, List<Content>> screen) {
        try {
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    int pageIndex = 0;
                    while (true) {
                        synchronized (screen) {
                            int pageSize = screen.size();
                            if (pageSize > 0) {
                                int index = pageIndex % pageSize;
//                                System.out.println("index:" + index);
                                display.displayString(screen.get(index));
                            }
                            pageIndex++;
                            if (pageIndex >= pageSize) {
                                pageIndex = 0;
                            }
                        }
                        ThreadUtil.safeSleep(5 * 1000);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void make(Map<Integer, List<Content>> screen) {
        try {
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        List<Content> page1 = getPage1();
                        List<Content> page2 = getPage2();
                        synchronized (screen) {
                            screen.put(0, page2);
                            screen.put(1, page1);
                        }
                        ThreadUtil.safeSleep(5 * 1000);
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
            Map<Integer, List<Content>> screen = new HashMap<>();
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

    private static double tv = 0;
    private static double hv = 0;

    private static List<Content> getPage1() {
        List<Content> list = new ArrayList<>();
        try {
            int fontSize = 13;
            Content ip = new Content(display.getGraphics2D(),
                    "IP:" + getLocalHostLANAddress().getHostAddress(),
                    FontUtil.createSansSerifFont(fontSize), 0, 0);
            list.add(ip);

            ChineseDate date = new ChineseDate(DateUtil.date());
            String shu9Text = date.getChineseMonthName() + date.getChineseDay();
            String shu9 = shu9();
            if (StringUtils.isNotBlank(shu9)) {
                shu9Text += " " + shu9;
            }
            list.add(new Content(display.getGraphics2D(),
                    shu9Text,
                    FontUtil.createSansSerifFont(fontSize), Align.ALIGN_LEFT, Align.ALIGN_BOTTOM));

            DHT11 dht11 = new DHT11(7);
            DHT11Result result = dht11.read();
            if (result.isValid()) {
                tv = result.getTemperature();
                hv = result.getHumidity();
            }
            Content t = new Content(display.getGraphics2D(),
                    "温度:" + tv,
                    FontUtil.createSansSerifFont(fontSize),
                    0,
                    (int) (ip.getStrRect().getHeight()));
            list.add(t);

            Content h = new Content(display.getGraphics2D(),
                    "湿度:" + hv,
                    FontUtil.createSansSerifFont(fontSize),
                    0,
                    (int) (ip.getStrRect().getHeight() + t.getStrRect().getHeight()));
            list.add(h);
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
                    FontUtil.createSansSerifFont(18), Align.ALIGN_CENTER, Align.ALIGN_CENTER);
            list.add(time);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
