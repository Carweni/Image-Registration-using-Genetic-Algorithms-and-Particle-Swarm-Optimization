import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

public class ImagePanel extends JPanel {
    private BufferedImage image;
    private String title;

    public ImagePanel(BufferedImage img, String title) {
        this.image = img;
        this.title = title;
        setPreferredSize(new Dimension(img.getWidth() + 20, img.getHeight() + 30));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 10, 10, this);
        g.setColor(Color.BLACK);
        g.drawString(title, 10, image.getHeight() + 25);
    }
}