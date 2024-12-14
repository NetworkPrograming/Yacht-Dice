import java.util.ArrayList;
import java.util.List;

public class Room {
    private String title;
    private String password;
    private List<String> people; // 참가자 목록
    private int maxPeople; // 최대 참가자 수
    private int game_start_flag = 0;

    public Room(String title, String password, int maxPeople) {
        this.title = title;
        this.password = password;
        this.maxPeople = maxPeople;
        this.people = new ArrayList<>();
        this.game_start_flag = 0;
    }

    public String getTitle() {
        return title;
    }

    public String getPassword() {
        return password;
    }

    public List<String> getPeople() {
        return people;
    }

    public int getMaxPeople() {
        return maxPeople;
    }

    public int setGame_start_flag(int flag) {
        game_start_flag = flag;
        return game_start_flag;
    }

    public int getGame_start_flag() {
        return game_start_flag;
    }

    public boolean addPeople(String userID) {
        if (people.size() < maxPeople) {
            people.add(userID);
            return true;
        }
        return false; // 최대 인원 초과시
    }

    public void removePeople(String userID) {
        people.remove(userID);
    }
}