import javax.swing.*;
import java.io.Serializable;

public class Yacht implements Serializable {
    public final static int MODE_LOGIN = 0x1;        // 0001
    public final static int MODE_LOGOUT = 0x2;       // 0010
    public final static int MODE_TX_STRING = 0x10;   // 0001 0000
    public final static int MODE_TX_FILE = 0x20;     // 0010 0000
    public final static int MODE_TX_IMAGE = 0x40;    // 0100 0000
    public final static int MODE_TX_ROOMNAME = 0x80;  // 1000 0000
    public final static int MODE_TX_PASSWORD = 0x100; // 0001 0000 0000

    String userID;
    int mode;
    String message;
    ImageIcon image;
    long size;
    String roomTitle; // 새로운 필드
    String passWord;  // 새로운 필드

    // 모든 필드를 초기화하는 생성자
    public Yacht(String userID, int code, String message, ImageIcon image, long size, String roomTitle, String passWord) {
        this.userID = userID;
        this.mode = code;
        this.message = message;
        this.image = image;
        this.size = size;
        this.roomTitle = roomTitle;
        this.passWord = passWord;
    }

    // roomTitle과 passWord를 포함한 생성자 추가
    public Yacht(String userID, int code, String message, ImageIcon image, long size, String password) {
        this(userID, code, message, image, size, null, password);
    }

    public Yacht(String userID, int code) {
        this(userID, code, null, null, 0, null, null);
    }

    public Yacht(String userID, int code, String message) {
        this(userID, code, message, null, 0, null); // roomTitle과 passWord는 null로 초기화
        if (code == MODE_TX_ROOMNAME) {
            this.roomTitle = message; // message를 roomTitle로 사용
        }
        else if (code == MODE_LOGIN) {
            this.message = message; // message를 message 사용
        }
        else if (code == MODE_LOGOUT) {
            this.message = message; // message를 message 사용
        }
    }

    public Yacht(String userID, int code, String message, String passWord) {
        this(userID, code, message, null, 0, passWord); // roomTitle과 passWord는 null로 초기화
        if (code == MODE_TX_PASSWORD) {
            this.roomTitle = message;
            this.passWord = passWord; // message를 passWord로 사용
        }
    }

    public Yacht(String userID, int code, ImageIcon image) {
        this(userID, code, null, image, 0, null, null);
    }

    public Yacht(String userID, int code, String filename, long size) {
        this(userID, code, filename, null, size, null, null);
    }


}
