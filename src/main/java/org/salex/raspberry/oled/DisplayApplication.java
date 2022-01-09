package org.salex.raspberry.oled;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.wiringpi.I2C;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

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
//            display.setPixel(10,10, true);
            display.displayString("中abcd国");
        } catch (I2CFactory.UnsupportedBusNumberException | IOException e) {
            e.printStackTrace();
        }
    }
}
