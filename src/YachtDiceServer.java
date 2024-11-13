import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.Vector;

public class YachtDiceServer extends JFrame {

    private int port;
    private ServerSocket serverSocket = null;

    private Thread acceptThread = null;
    private Vector<ClientHandler> users = new Vector<ClientHandler>();
    // 방 목록을 저장할 Vector
    private Vector<JButton> roomButtons = new Vector<>();

    private JPanel roomPanel;
    private JTextArea t_display;

    public YachtDiceServer(int port) {
        super("Yacht Game Server");
        buildGUI();

        setMinimumSize(new Dimension(800, 700)); // 최소 크기 설정
        setSize(800, 700);
        setLocation(800, 0);
        setResizable(false); // 창 크기 조절 비활성화
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        this.port = port;

        acceptThread = new Thread(new Runnable() {
            @Override
            public void run() {
                startServer();
            }
        });
        acceptThread.start();
    }

    private void createRoom(String roomTitle, String password) {
        JButton roomButton;

        // 서버의 경우 비밀 방인 경우 방 이름과 비밀번호까지 같이 출력한다.
        if (password == null) {
            roomButton = new JButton(roomTitle);
        } else {
            roomButton = new JButton("<html><div style='text-align: center;'>" + roomTitle + "<br>비밀번호: " + password + "</div></html>");
        }
        roomButton.setPreferredSize(new Dimension(231, 70)); // 버튼 가로 크기 설정
        roomButton.setMaximumSize(new Dimension(231, 70)); // 버튼 최대 크기 설정하여 가로 중앙 정렬 유지

        roomButtons.add(roomButton);

        // 방 목록 패널에 버튼 추가
        roomPanel.add(roomButton); // 버튼 추가
        roomPanel.add(Box.createVerticalStrut(50)); // 버튼 아래쪽 여백 추가

        roomPanel.revalidate(); // 패널 갱신
        roomPanel.repaint(); // 패널 다시 그리기
    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;
        private ObjectOutputStream out;

        private String uid;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        private void receiveMessages(Socket cs) {
            try {
                ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(cs.getInputStream()));
                out = new ObjectOutputStream(new BufferedOutputStream(cs.getOutputStream()));


                Yacht msg;

                msg = (Yacht) in.readObject(); // 첫 번째 메시지를 읽어서 로그인 요청을 받는다.
                if (msg.mode == Yacht.MODE_LOGIN) {
                    uid = msg.userID;

                    // 중복 사용자 이름 체크
                    boolean exists = false;
                    for (ClientHandler user : users) {
                        if (user.uid != null && user.uid.equals(uid)) {
                            exists = true;
                            break;
                        }
                    }
                    if (exists) {
                        // 중복된 UID인 경우 클라이언트에 메시지를 전송하고 연결 종료
                        out.writeObject(new Yacht(uid, Yacht.MODE_LOGIN, "이미 사용중인 이름 입니다."));
                        out.flush();
                        cs.close();
                        return;
                    }

                    // UID가 중복되지 않는 경우
                    users.add(this); // 사용자 목록에 추가
                    printDisplay(uid + " 입장. [ 현재 참가자 수 : " + users.size() + " ]");
                    msg.message = uid + " 입장. [ 현재 참가자 수 : " + users.size() + " ]";
                    broadcasting(msg);
                }
                while ((msg = (Yacht) in.readObject()) != null) {
//                    if (msg.mode == Yacht.MODE_LOGIN) {
//                        uid = msg.userID;
//
//                        // 중복 사용자 이름 체크
//                        boolean exists = false;
//                        for (ClientHandler user : users) {
//                            if (user.uid != null && user.uid.equals(uid)) {
//                                exists = true;
//                                break;
//                            }
//                        }
//                        if (exists) {
//                            // 중복된 UID인 경우 클라이언트에 메시지를 전송하고 연결 종료
//                            out.writeObject(new Yacht(uid, Yacht.MODE_LOGIN, "이미 사용중인 이름 입니다."));
//                            out.flush();
//                            cs.close(); // 클라이언트 소켓 종료
//                            return; // 메서드 종료
//                        }
//
//
//                        printDisplay(uid + " 입장. [ 현재 참가자 수 : " + users.size() + " ]");
//                        msg.message = uid + " 입장. [ 현재 참가자 수 : " + users.size() + " ]";
//                        broadcasting(msg);
//                        continue;
//                    } else
                    if (msg.mode == Yacht.MODE_LOGOUT) {
                        uid = msg.userID;
                        //printDisplay(uid + " 퇴장. [ 현재 참가자 수 : " + users.size() + " ]");
                        msg.message = uid + " 퇴장. [ 현재 참가자 수 : " + (users.size() - 1) + " ]";
                        broadcasting(msg);
                        break;
                    } else if (msg.mode == Yacht.MODE_TX_STRING) {
                        String message = uid + ": " + msg.message;
                        printDisplay(message);
                        broadcasting(msg);
                    } else if (msg.mode == Yacht.MODE_TX_IMAGE) {
                        printDisplay(uid + ": " + msg.message);
                        broadcasting(msg);
                        ///////////////////////
                    } else if (msg.mode == Yacht.MODE_TX_ROOMNAME) {
                        createRoom(msg.roomTitle, msg.passWord);
                        String message = uid + "님이 \"" + msg.message + "\" 일반 방을 생성하였습니다.";
                        printDisplay(message);
                        broadcasting(msg);
                    } else if (msg.mode == Yacht.MODE_TX_PASSWORD) {
                        createRoom(msg.roomTitle, msg.passWord);
                        String message = uid + "님이 \"" + msg.message + "\" 비밀 방을 생성하였습니다.\n";
                        message += "생성된 방의 비밀번호는 \"" + msg.passWord + "\" 입니다.";
                        printDisplay(message);
                        broadcasting(msg);
                    }
//                    else if (msg.mode == Yacht.MODE_GAME_TEXT) {
//                        String message = "생성된 방의 비밀번호는 \"" + msg.message + "\" 입니다.";
//                        printDisplay(message);
//                        broadcasting(msg, roomTitle);
//                    }
                    ///////////////////////
                }
                users.removeElement(this);
                printDisplay(uid + " 퇴장. [ 현재 참가자 수 : " + users.size() + " ]");
            } catch (IOException e) {
                users.removeElement(this);
                printDisplay(uid + " 연결 끊김. [ 현재 참가자 수 : " + users.size() + " ]");
            } catch (ClassNotFoundException e) {
                printDisplay("잘못된 객체가 전달되었습니다.");
            } finally {
                try {
                    cs.close();
                } catch (IOException e) {
                    System.err.println("서버 닫기 오류 > " + e.getMessage());
                    System.exit(-1);
                }
            }
        }

        private void send(Yacht msg) {
            try {
                out.writeObject(msg);
                out.flush();
            } catch (IOException e) {
                System.err.println("클라이언트 일반 전송 오류 > " + e.getMessage());
            }
        }

        private void broadcasting(Yacht msg) {
            for (ClientHandler c : users) {
                c.send(msg);
            }
        }

//        private void broadcasting(Yacht msg, String roomname) {
//            for (ClientHandler c : users) {
//                c.send(msg);
//            }
//        }

        public void run() {
            receiveMessages(clientSocket);
        }
    }

    private void buildGUI() {
        // 상위 패널 생성
        JPanel mainPanel = new JPanel(new BorderLayout());

        // 왼쪽 패널
        JPanel leftPanel = createDisplayPanel(); // 기존의 createDisplayPanel 호출
        mainPanel.add(leftPanel, BorderLayout.CENTER); // 왼쪽에 추가

        // 오른쪽 패널
        JPanel rightPanel = (textPanel()); // 예시 패널 생성 메서드 호출
        mainPanel.add(rightPanel, BorderLayout.EAST); // 오른쪽에 추가

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel textPanel() {
        JPanel p = new JPanel(new BorderLayout());

        t_display = new JTextArea();
        t_display.setLineWrap(true); // 줄 바꿈 설정
        t_display.setWrapStyleWord(true); // 단어 단위로 줄 바꿈
        t_display.setEditable(false);


        JScrollPane scrollPane = new JScrollPane(t_display);
        scrollPane.setPreferredSize(new Dimension(450, 200)); // 스크롤 패널 크기 설정


        p.add(scrollPane, BorderLayout.CENTER);

        return p;
    }

    private JPanel createDisplayPanel() {
        JPanel p = new JPanel(new BorderLayout());

        // 버튼과 여백을 포함하는 패널 생성
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.add(Box.createVerticalStrut(50)); // 버튼 아래에 10픽셀 여백 추가

        // 버튼 패널을 상단에 추가
        p.add(buttonPanel, BorderLayout.NORTH);

        // 여백을 유지하기 위한 빈 패널 추가
        JPanel westPanel = new JPanel();
        westPanel.setPreferredSize(new Dimension(50, 50));
        p.add(westPanel, BorderLayout.WEST);

        JPanel southPanel = new JPanel();
        southPanel.setPreferredSize(new Dimension(50, 50));
        p.add(southPanel, BorderLayout.SOUTH);

        JPanel eastPanel = new JPanel();
        eastPanel.setPreferredSize(new Dimension(50, 50));
        p.add(eastPanel, BorderLayout.EAST);

        // 방 목록이 표시되는 패널
        roomPanel = new JPanel();
        roomPanel.setLayout(new BoxLayout(roomPanel, BoxLayout.Y_AXIS)); // BoxLayout으로 설정

        // 방 목록에 스크롤 가능한 패널 추가
        JScrollPane roomScrollPane = new JScrollPane(roomPanel);
        roomScrollPane.setPreferredSize(new Dimension(200, 200));
        //roomScrollPane.setBorder(null); // 테두리 제거

        // 스크롤바 숨기기
        //roomScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER); // Y축 숨기기
        roomScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // X축 숨기기

        p.add(buttonPanel, BorderLayout.NORTH);
        p.add(roomScrollPane, BorderLayout.CENTER);

        return p;
    }

    private String getLocalAddr() {
        InetAddress local = null;
        try {
            local = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            System.err.println("서버 주소 오류 : " + e.getMessage());
            System.exit(-1);
        }
        return local.getHostAddress();
    }

    private void printDisplay(String msg) {
        //System.out.println(msg);
        t_display.append(msg + "\n");
        t_display.setCaretPosition(t_display.getDocument().getLength());

    }

    private void startServer() {
        Socket clientSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            printDisplay("서버가 시작되었습니다. " + getLocalAddr());

            while (acceptThread == Thread.currentThread()) {
                clientSocket = serverSocket.accept();

                String cAddr = clientSocket.getInetAddress().getHostAddress();

                ClientHandler cHandler = new ClientHandler(clientSocket);
                //users.add(cHandler);
                cHandler.start();
            }
        } catch (SocketException e) {
            printDisplay("서버 소켓 종료");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                System.err.println("서버 닫기 오류 : " + e.getMessage());
                System.exit(-1);
            }
        }
    }

    public static void main(String[] args) {
        int port = 54321;
        YachtDiceServer server = new YachtDiceServer(port);
    }
}