package org.salex.raspberry.oled;

import cn.hutool.core.date.ChineseDate;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.img.FontUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.json.JSONUtil;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.wiringpi.I2C;
import ink.dwx.Align;
import ink.dwx.Content;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.salex.raspberry.oled.dht11.DHT11;
import org.salex.raspberry.oled.dht11.DHT11Result;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.*;

@Slf4j
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
                        log.info("make 1");
                        List<Content> page1 = getPage1();
                        log.info("make 2 {}", JSONUtil.toJsonStr(page1));
                        List<Content> page2 = getPage2();
                        log.info("make 3 {}", JSONUtil.toJsonStr(page2));
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
            log.info(JSONUtil.toJsonStr(screen));
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
        Date dongZhi = DateUtil.parse("2022-12-22");
        if (DateUtil.date().before(dongZhi)) {
            return "";
        }
        Long betweenDay = DateUtil.between(dongZhi, DateUtil.date(), DateUnit.DAY);
        if (betweenDay >= 81) {
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
            int fontSize = 14;
            log.info("0");
            Content ip = new Content(display.getGraphics2D(),
                    "IP:" + getLocalHostLANAddress().getHostAddress(),
                    FontUtil.createSansSerifFont(fontSize), 0, 0);
            log.info("1");
            list.add(ip);

            ChineseDate date = new ChineseDate(DateUtil.date());
            String shu9Text = date.getChineseMonthName() + date.getChineseDay();
            String shu9 = shu9();
            log.info("2");
            if (StringUtils.isNotBlank(shu9)) {
                shu9Text += " " + shu9;
            }
            log.info("3");
            list.add(new Content(display.getGraphics2D(),
                    shu9Text,
                    FontUtil.createSansSerifFont(fontSize), Align.ALIGN_LEFT, Align.ALIGN_BOTTOM));
            log.info("4");
//            DHT11 dht11 = new DHT11(7);
            log.info("5");
//            DHT11Result result = dht11.read();
            log.info("6");
//            if (result.isValid()) {
//                log.info("7");
//                tv = result.getTemperature();
//                hv = result.getHumidity();
////                System.out.println(tv + " " + hv);
//            }
            log.info("8");
            Content t = new Content(display.getGraphics2D(),
                    "温度:" + tv,
                    FontUtil.createSansSerifFont(fontSize),
                    0,
                    (int) (ip.getStrRect().getHeight()));
            log.info("9");
            list.add(t);
            log.info("10");
            Content h = new Content(display.getGraphics2D(),
                    "湿度:" + hv,
                    FontUtil.createSansSerifFont(fontSize),
                    0,
                    (int) (ip.getStrRect().getHeight() + t.getStrRect().getHeight()));
            log.info("11");
            list.add(h);
            log.info("12");
            log.info(JSONUtil.toJsonStr(list));
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
                    FontUtil.createSansSerifFont(20), Align.ALIGN_CENTER, Align.ALIGN_CENTER);
            list.add(time);
            log.info(JSONUtil.toJsonStr(list));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
