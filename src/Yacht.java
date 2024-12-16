import javax.swing.*;
import java.io.Serializable;

public class Yacht implements Serializable {
    public final static int MODE_LOGIN = 0x1;                   // 0001
    public final static int MODE_LOGOUT = 0x2;                  // 0010
    public final static int MODE_TX_STRING = 0x10;              // 0001 0000
    public final static int MODE_TX_FILE = 0x20;                // 0010 0000
    public final static int MODE_TX_IMAGE = 0x40;               // 0100 0000
    public final static int MODE_CREATE_NORMAL_ROOM = 0x80;     // 1000 0000
    public final static int MODE_CREATE_SECRET_ROOM = 0x100;    // 0001 0000 0000
    public final static int MODE_ENTER_ROOM = 0x200;            // 0010 0000 0000
    public final static int MODE_REQUEST_ROOM_LIST = 0x400;     // 0100 0000 0000
    public final static int MODE_ROOM_LIST = 0x800;             // 1000 0000 0000
    public final static int MODE_QUIT_ROOM = 0x1000;            // 0001 0000 0000 0000
    public final static int MODE_TX_STRING_ROOM = 0x2000;       // 0010 0000 0000 0000
    public final static int MODE_TX_STRING_ROOM_FIRST = 0x4000; // 0100 0000 0000 0000
    public final static int MODE_TX_STRING_SCORE = 0x8000;      // 1000 0000 0000 0000
    public final static int MODE_GAME_START = 0x10000;          // 0001 0000 0000 0000 0000
    public final static int MODE_HOW_MANY_PEOPLE = 0x20000;     // 0010 0000 0000 0000 0000
    public final static int MODE_SHOW_ROLLING_DICE = 0x40000;   // 0100 0000 0000 0000 0000
    public final static int MODE_MOVE_DICE = 0x80000;           // 1000 0000 0000 0000 0000
    public final static int MODE_SURRENDER = 0x100000;          // 0001 0000 0000 0000 0000 0000

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
        if (code == MODE_CREATE_NORMAL_ROOM) {
            this.roomTitle = message; // message를 roomTitle로 사용
        } else if (code == MODE_LOGIN) {
            this.message = message; // message를 message 사용
        } else if (code == MODE_LOGOUT) {
            this.message = message; // message를 message 사용
        } else if (code == MODE_ENTER_ROOM) {
            this.message = message; // message를 message 사용
        } else if (code == MODE_REQUEST_ROOM_LIST) {
            this.message = ""; // message를 message 사용
        } else if (code == MODE_QUIT_ROOM) {
            this.message = message; // message를 message 사용
        } else if (code == MODE_HOW_MANY_PEOPLE) {
            this.message = message; // message를 message 사용
        }
    }

    public Yacht(String userID, int code, String message, String passWord) {
        this(userID, code, message, null, 0, passWord);
        if (code == MODE_CREATE_SECRET_ROOM) {
            this.roomTitle = message;
            this.passWord = passWord;
        } else if (code == MODE_TX_STRING_ROOM) {
            this.roomTitle = message;
            this.message = passWord;
        } else if (code == MODE_TX_STRING_ROOM_FIRST) {
            this.roomTitle = message;
            this.message = passWord;
        } else if (code == MODE_GAME_START) {
            this.roomTitle = message;
            this.message = passWord;
        } else if (code == MODE_TX_STRING_SCORE) {
            this.message = message; // message를 message 사용
            this.roomTitle = passWord;
        } else if (code == MODE_SHOW_ROLLING_DICE) {
            this.roomTitle = message; // message를 message 사용
            this.message = passWord;
        } else if (code == MODE_MOVE_DICE) {
            this.roomTitle = message; // message를 message 사용
            this.message = passWord;
        } else if (code == MODE_SURRENDER) {
            this.roomTitle = message; // message를 message 사용
            this.message = passWord;
        }
    }

    public Yacht(String userID, int code, ImageIcon image) {
        this(userID, code, null, image, 0, null, null);
    }

    public Yacht(String userID, int code, String filename, long size) {
        this(userID, code, filename, null, size, null, null);
    }
}
