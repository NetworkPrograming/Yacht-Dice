import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Vector;

public class YachtDiceServer extends JFrame {
    private int port;
    private ServerSocket serverSocket = null;

    private String[] User_Array_server = {"","","",""};

    private Thread acceptThread = null;
    private Vector<ClientHandler> users = new Vector<ClientHandler>();

    private JPanel roomPanel;
    private JTextArea t_display;

    private List<Room> rooms = new Vector<>(); // 방 목록

    public YachtDiceServer(int port) {
        super("Yacht Game Server");
        buildGUI();

        setMinimumSize(new Dimension(700, 700)); // 최소 크기 설정
        setSize(700, 700);
        setLocation(700, 0);
        setResizable(false);
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

                msg = (Yacht) in.readObject();
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
                    printDisplay("(" + uid + ")" + " 입장. [ 현재 참가자 수 : " + users.size() + " ]");
                    msg.message = uid + " 입장. [ 현재 참가자 수 : " + users.size() + " ]";
                    sendRoomListToClients();
                    broadcasting(msg);
                }
                while ((msg = (Yacht) in.readObject()) != null) {
                    if (msg.mode == Yacht.MODE_LOGOUT) {
                        uid = msg.userID;
                        msg.message = uid + " 퇴장. [ 현재 참가자 수 : " + (users.size() - 1) + " ]";
                        broadcasting(msg);
                        break;
                    } else if (msg.mode == Yacht.MODE_TX_STRING) {
                        String message = "(" + uid + ")" + " 님의 채팅: " + msg.message;
                        printDisplay(message);
                        broadcasting(msg);
                    } else if (msg.mode == Yacht.MODE_TX_IMAGE) {
                        printDisplay(uid + ": " + msg.message);
                        broadcasting(msg);
                    } else if (msg.mode == Yacht.MODE_CREATE_NORMAL_ROOM) {
                        // 방이 존재하는지 확인
                        int check = 1;
                        for (Room room : rooms) {
                            if (room.getTitle().equals(msg.roomTitle)) {
                                String message = "일반 방이 이미 존재합니다.";
                                msg.message = "일반 방이 이미 존재합니다.";
                                printDisplay(message);
                                broadcasting(msg);
                                check = 0;
                                break;
                            }
                        }
                        if (check == 1) {
                            // 방 생성
                            Room newRoom = new Room(msg.roomTitle, msg.passWord, 4); // 최대 4명
                            rooms.add(newRoom);
                            String message = "(" + uid + ")" + " 님이 \"" + msg.message + "\" 일반 방을 생성하였습니다.";

                            createRoom(msg.roomTitle, msg.passWord);

                            printDisplay(message);
                            broadcasting(msg);

                            sendRoomListToClients();
                        }
                    } else if (msg.mode == Yacht.MODE_CREATE_SECRET_ROOM) {
                        // 방이 존재하는지 확인
                        int check = 1;
                        for (Room room : rooms) {
                            if (room.getTitle().equals(msg.roomTitle)) {
                                String message = "비밀 방이 이미 존재합니다.";
                                msg.message = "비밀 방이 이미 존재합니다.";
                                printDisplay(message);
                                broadcasting(msg);
                                check = 0;
                                break;
                            }
                        }
                        if (check == 1) {
                            // 방 생성
                            Room newRoom = new Room(msg.roomTitle, msg.passWord, 4); // 최대 4명
                            rooms.add(newRoom);
                            String message = "(" + uid + ")" + " 님이 \"" + msg.message + "\" 비밀 방을 생성하였습니다.\n";
                            message += "생성된 방의 비밀번호는 \"" + msg.passWord + "\" 입니다.";

                            createRoom(msg.roomTitle, msg.passWord);

                            printDisplay(message);
                            broadcasting(msg);

                            sendRoomListToClients();
                        }
                    } else if (msg.mode == Yacht.MODE_ENTER_ROOM) {
                        // 방을 들어가고싶으면 방의 인원 수를 먼저 확인 후에 비밀방 일반방을 구분하고 입장하기

                        Room targetRoom = null;
                        for (Room room : rooms) {
                            if (room.getTitle().equals(msg.message)) {
                                targetRoom = room;
                                break;
                            }
                        }

                        if (targetRoom != null) {
                            int i = 0;
                            // 방의 현재 참가자 수 확인
                            int currentPeopleCount = targetRoom.getPeople().size();
                            if (currentPeopleCount < targetRoom.getMaxPeople()) {
                                // 방에 참가 가능
                                targetRoom.addPeople(uid); // 참가자 추가

                                List<String> people = targetRoom.getPeople(); // 사용자 목록 가져오기
                                printDisplay("(" + targetRoom.getTitle() + ")" + " 방에 (" + uid + ") 님이 참가하였습니다.");
                                printDisplay(targetRoom.getTitle() + " 방의 참가자 목록 :");

                                if (people.isEmpty()) {
                                    printDisplay("현재 참가자가 없습니다.");
                                } else {
                                    for (String userID : people) {
                                        printDisplay("- " + userID);
                                        User_Array_server[i] = userID;
                                        i++;
                                    }
                                    printDisplay("");
                                }
                                broadcasting(msg);
                            } else {
                                // 방이 가득 찼을 경우
                                String message = "(" + targetRoom.getTitle() + ")" + " 방이 가득 차서 입장할 수 없습니다.";
                                msg.message = message;
                                printDisplay("(" + uid + ")" + " 님 " + message);
                                send(msg);
                            }
                        } else {
                            // 방이 존재하지 않는 경우
                            String message = "존재하지 않는 방입니다.";
                            printDisplay(message);
                            msg.message = message;
                            send(msg);
                        }
                    } else if (msg.mode == Yacht.MODE_QUIT_ROOM) {
                        //방 퇴장
                        Room targetRoom = null;
                        for (Room room : rooms) {
                            if (room.getTitle().equals(msg.message)) {
                                targetRoom = room;
                                break;
                            }
                        }

                        if (targetRoom != null) {
                            int i = 0;
                            // 방에서 사용자 제거
                            targetRoom.removePeople(uid);

                            // 방의 현재 참가자 수 확인
                            int currentPeopleCount = targetRoom.getPeople().size();
                            String message = "(" + uid + ")" + " 님이 (" + targetRoom.getTitle() + ") 방에서 퇴장하였습니다.";
                            printDisplay(message);

                            // 퇴장 메시지를 모든 클라이언트에게 방송
                            msg.message = message;
                            broadcasting(msg);

                            // 방의 참가자 목록 출력
                            List<String> people = targetRoom.getPeople(); // 사용자 목록 가져오기
                            if (people.isEmpty()) {
                                System.out.println("현재 (" + targetRoom.getTitle() + ") 방에 참가자가 없습니다.");
                            } else {
                                printDisplay("(" + targetRoom.getTitle() + ")" + " 방의 참가자 목록 :");
                                for (String userID : people) {
                                    printDisplay("- " + userID);
                                    User_Array_server[i] = userID;
                                    i++;
                                }
                                for (int k = i; k < 4; k++) {
                                    User_Array_server[k] = "";
                                }
                                printDisplay("");
                            }
                            // 방에 더 이상 참가자가 없다면 방 삭제
                            if (currentPeopleCount == 0) {
                                rooms.remove(targetRoom); // 방 삭제
                                printDisplay("현재 (" + targetRoom.getTitle() + ") 방에 참가자가 없어 방이 삭제되었습니다.");
                            }
                        } else {
                            String message = "존재하지 않는 방입니다.";
                            printDisplay(message);
                            msg.message = message;
                            send(msg);
                        }
                        sendRoomListToClients();
                        refresh();
                    } else if (msg.mode == Yacht.MODE_TX_STRING_ROOM) {
                        String message = "(" + uid + ")" + " 님의 채팅: " + msg.message;
                        printDisplay("(" + msg.roomTitle + ")" + "방의 " + message); // 방에서 채팅하는거 보이게 하려면 활성화
                        broadcasting(msg);
                    } else if (msg.mode == Yacht.MODE_TX_STRING_ROOM_FIRST) {
                        String result = String.join("!=!", User_Array_server);
                        result = "!=!" + result;
                        msg.message = msg.message + result;
                        //printDisplay(msg.message);
                        broadcasting(msg);
                    } else if (msg.mode == Yacht.MODE_TX_STRING_SCORE) {
                        String message = msg.message; // 클라이언트가 보낸 메시지

                        // 메시지에서 "UserNum: 0, ScoreIndex: 1, Score: 10" 형태로 값을 추출하려면
                        printDisplay("받은 메시지: " + message);

                        // message에서 필요한 값 추출
                        String[] parts = message.split(", ");
                        String userNumStr = parts[0].split(": ")[1];  // "0" -> userNum
                        String scoreIndexStr = parts[1].split(": ")[1];  // "1" -> scoreIndex
                        String scoreStr = parts[2].split(": ")[1];  // "10" -> score

                        int userNum = Integer.parseInt(userNumStr);
                        int scoreIndex = Integer.parseInt(scoreIndexStr);
                        int score = Integer.parseInt(scoreStr);
                        message = "UserNum: " + userNum + ", ScoreIndex: " + scoreIndex + ", Score: " + score;
                        msg.message = message;

                        printDisplay("userNum: " + userNum + ", scoreIndex: " + scoreIndex + ", score: " + score);

                        // 해당 값을 다른 클라이언트로 브로드캐스팅
                        broadcasting(msg);
                    }
                }
                users.removeElement(this);
                printDisplay("(" + uid + ")" + " 퇴장. [ 현재 참가자 수 : " + users.size() + " ]");
            } catch (IOException e) {
                users.removeElement(this);
                printDisplay("(" + uid + ")" + " 연결 끊김. [ 현재 참가자 수 : " + users.size() + " ]");
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

        private void sendRoomListToClients() {
            StringBuilder roomList = new StringBuilder();
            for (Room room : rooms) {
                // 방 제목과 비밀번호 존재 여부를 체크하여 문자열 생성
                roomList.append(room.getTitle()).append(room.getPassword() != null ? ":" + room.getPassword() : ":0").append(",");
                // ":0"은 비밀번호 없음
            }
            // 마지막 쉼표 제거
            if (roomList.length() > 0) {
                roomList.setLength(roomList.length() - 1);
            }
            Yacht roomListMsg = new Yacht(null, Yacht.MODE_ROOM_LIST, roomList.toString()); // 방 목록을 문자열로 전송
            broadcasting(roomListMsg);

            String[] roomTitlesArray = roomList.toString().split(","); // 쉼표로 분리하여 배열로 변환
            displayRoomList(roomTitlesArray);
        }

        private void displayRoomList(String[] roomEntries) {
            roomPanel.removeAll(); // 기존 방 버튼 제거

            for (String entry : roomEntries) {
                String[] parts = entry.split(":"); // 방 제목과 비밀번호 존재 여부를 ":"로 분리
                String title = parts[0]; // 방 제목
                boolean hasPassword = parts.length > 1 && !parts[1].equals("0"); // 비밀번호 존재 여부 체크
                String password_temp = "d";
                if (title == null || title.trim().isEmpty()) {
                    // 방 이름이 없을때 아무일도 일어나지 않아야함.
                } else {
                    JButton roomButton;
                    if (hasPassword) {
                        password_temp = parts[1];
                        roomButton = new JButton("<html><div style='text-align: center;'>" + title + "<br>비밀번호: " + password_temp + "</div></html>");
                    } else {
                        password_temp = null;
                        roomButton = new JButton(title);
                    }

                    roomButton.setPreferredSize(new Dimension(roomPanel.getParent().getWidth(), 70)); // 버튼 가로 크기 설정
                    roomButton.setMaximumSize(new Dimension(roomPanel.getParent().getWidth(), 70)); // 버튼 최대 크기 설정

                    roomPanel.add(roomButton); // 방 버튼 추가
                    roomPanel.add(Box.createVerticalStrut(50)); // 버튼 아래쪽 여백 추가
                }
            }
            roomPanel.revalidate(); // 패널 업데이트
            roomPanel.repaint(); // 화면 다시 그리기
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

        public void run() {
            receiveMessages(clientSocket);
        }
    }

    private void createRoom(String roomTitle, String password) {
        JButton roomButton;

        // 서버의 경우 비밀 방인 경우 방 이름과 비밀번호까지 같이 출력한다.
        if (password == null) {
            roomButton = new JButton(roomTitle);
        } else {
            roomButton = new JButton("<html><div style='text-align: center;'>" + roomTitle + "<br>비밀번호: " + password + "</div></html>");
        }
        roomButton.setPreferredSize(new Dimension(roomPanel.getParent().getWidth(), 70)); // 버튼 가로 크기 설정
        roomButton.setMaximumSize(new Dimension(roomPanel.getParent().getWidth(), 70)); // 버튼 최대 크기 설정

        // 방 목록 패널에 버튼 추가
        roomPanel.add(roomButton); // 버튼 추가
        roomPanel.add(Box.createVerticalStrut(50)); // 버튼 아래쪽 여백 추가

        refresh();
    }

    private void buildGUI() {
        // 상위 패널 생성
        JPanel mainPanel = new JPanel(new BorderLayout());

        // 왼쪽 패널
        JPanel leftPanel = createDisplayPanel();
        mainPanel.add(leftPanel, BorderLayout.CENTER); // 왼쪽에 추가

        // 오른쪽 패널
        JPanel rightPanel = textPanel();
        mainPanel.add(rightPanel, BorderLayout.EAST); // 오른쪽에 추가

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel textPanel() {
        JPanel p = new JPanel(new BorderLayout());

        t_display = new JTextArea();
        t_display.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(t_display);
        scrollPane.setPreferredSize(new Dimension(350, 200)); // 스크롤 패널 크기 설정

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
        roomPanel.setLayout(new BoxLayout(roomPanel, BoxLayout.Y_AXIS));

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

    private void refresh() {
        roomPanel.revalidate(); // 패널 갱신
        roomPanel.repaint(); // 패널 다시 그리기
    }

    private void printDisplay(String msg) {
        t_display.append(msg + "\n");
        t_display.setCaretPosition(t_display.getDocument().getLength());
    }

    public static void main(String[] args) {
        int port = 54321;
        YachtDiceServer server = new YachtDiceServer(port);
    }
}