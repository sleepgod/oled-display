package org.salex.raspberry.oled;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.wiringpi.I2C;
import org.salex.raspberry.oled.dht11.DHT11;
import org.salex.raspberry.oled.dht11.DHT11Result;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class DisplayApplication {
    public static void main(String[] args) {
        // create gpio controller
        final GpioController gpio = GpioFactory.getInstance();
        I2CBus i2c;
        Display display;
        try {
            i2c = I2CFactory.getInstance(I2C.CHANNEL_1);
            display = new Display(128, 64, gpio, i2c, 0x3c);
            display.begin();
            display.clear();
//            display.displayString("IP:" + getLocalHostLANAddress().getHostAddress());

            DHT11 dht11 = new DHT11(7);
            while (true) {
                DHT11Result result = dht11.read();
                if (result.isValid()) {
                    System.out.println("Last valid input: " + new Date());
                    System.out.printf("Temperature: %.1f C\n", result.getTemperature());
                    System.out.printf("Humidity:    %.1f %%\n", result.getHumidity());
                    display.displayString("IP:" + getLocalHostLANAddress().getHostAddress()
                            + "温度:" + result.getTemperature()
                            + "湿度:" + result.getHumidity());
                }
                TimeUnit.SECONDS.sleep(30);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
}
