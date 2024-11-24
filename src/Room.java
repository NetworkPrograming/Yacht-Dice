import java.util.ArrayList;
import java.util.List;

public class Room {
    private String title;
    private String password;
    private List<String> people; // 참가자 목록
    private int maxPeople; // 최대 참가자 수

    public Room(String title, String password, int maxPeople) {
        this.title = title;
        this.password = password;
        this.maxPeople = maxPeople;
        this.people = new ArrayList<>();
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