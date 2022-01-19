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
    private Rectangle2D strRect;

    public Content(Graphics2D g2d, String str, Font font, int x, int y) {
        this.font = font;
        g2d.setFont(font);
        strRect = font.getStringBounds(str, g2d.getFontRenderContext());
//        this.point = new Point((int) (x + 0 - strRect.getY()), (int) (y + 0 - strRect.getY()));
        this.point = new Point(x, y);
        this.str = str;
    }

    public Content(Graphics2D g2d, String str, Font font, Align xAlign, Align yAlign) {
        this.font = font;
        g2d.setFont(font);
        strRect = font.getStringBounds(str, g2d.getFontRenderContext());
        Rectangle rectangle = g2d.getClipBounds();

        int x = (int) (0 - strRect.getY());
        int y = (int) (0 - strRect.getY());
        switch (xAlign) {
            case ALIGN_LEFT: {
                x = x + 0;
                break;
            }
            case ALIGN_RIGHT: {
                x = x + (int) (rectangle.getWidth() - strRect.getWidth());
                break;
            }
            case ALIGN_CENTER: {
                x = x + (int) ((rectangle.getWidth() - strRect.getWidth()) / 2);
                break;
            }
        }
        switch (yAlign) {
            case ALIGN_TOP: {
                y = y + 0;
                break;
            }
            case ALIGN_BOTTOM: {
                y = y + (int) (rectangle.getHeight() - strRect.getHeight());
                break;
            }
            case ALIGN_CENTER: {
                y = y + (int) ((rectangle.getHeight() - strRect.getHeight()) / 2);
                break;
            }
        }
        this.point = new Point(x, y);
        this.str = str;
    }
}
