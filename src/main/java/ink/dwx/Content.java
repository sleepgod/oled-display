package ink.dwx;

import lombok.Data;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * Created by wenxuan.ding on 2022/1/18 5:08 下午.
 */
@Data
public class Content {
    private Font font;
    private Point point;
    private String str;

    public Content(Graphics2D g2d, String str, Font font, int x, int y) {
        this.font = font;
        g2d.setFont(font);
        Rectangle2D rectangle2D = font.getStringBounds(str, g2d.getFontRenderContext());
        this.point = new Point((int) (x + 0 - rectangle2D.getY()), (int) (y + 0 - rectangle2D.getY()));
        this.str = str;
    }

    public Content(Graphics2D g2d, String str, Font font, Align xAlign, Align yAlign) {
        this.font = font;
        g2d.setFont(font);
        Rectangle2D rectangle2D = font.getStringBounds(str, g2d.getFontRenderContext());

        int x = (int) (0 - rectangle2D.getY());
        int y = (int) (0 - rectangle2D.getY());
        switch (xAlign) {
            case ALIGN_LEFT: {
                x = 0;
                break;
            }
            case ALIGN_RIGHT: {
            }
        }
        this.point = new Point(x, y);
        this.str = str;
    }
}
