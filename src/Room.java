import java.util.ArrayList;
import java.util.List;

public class Room {
    private String title;
    private String password;
    private List<String> participants; // 참가자 목록
    private int maxParticipants; // 최대 참가자 수

    public Room(String title, String password, int maxParticipants) {
        this.title = title;
        this.password = password;
        this.maxParticipants = maxParticipants;
        this.participants = new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public String getPassword() {
        return password;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public boolean addParticipant(String userID) {
        if (participants.size() < maxParticipants) {
            participants.add(userID);
            return true;
        }
        return false; // 최대 인원 초과
    }

    public void removeParticipant(String userID) {
        participants.remove(userID);
    }
}