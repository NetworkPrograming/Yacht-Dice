import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class ImgService {
    public static JButton loadImage(String filePath){
        BufferedImage image;
        JButton imageContainer;
        try{
            InputStream inputStream = ImgService.class.getResourceAsStream(filePath);
            image = ImageIO.read(inputStream);
            imageContainer = new JButton(new ImageIcon(image));
            return imageContainer;
        }catch(Exception e){
            System.out.println("Error: " + e);
            return null;
        }
    }

    public static void updateImage(JButton imageContainer, String filePath){
        BufferedImage image;
        try{
            InputStream inputStream = ImgService.class.getResourceAsStream(filePath);
            image = ImageIO.read(inputStream);
            imageContainer.setIcon(new ImageIcon(image));
        }catch(Exception e){
            System.out.println("Error: " + e);
        }
    }
}

