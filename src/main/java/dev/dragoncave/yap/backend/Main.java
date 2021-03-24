package dev.dragoncave.yap.backend;

public class Main {

    private static final String DATABASE_URL = "jdbc:sqlite:database.db";

    public static void main(String[] args) {
        try {
            DatabaseManager instance = DatabaseManager.getInstance();
            System.out.println(instance.getUserJson(2));
            System.out.println(instance.getGroupJson(1));
            System.out.println(instance.getEntryJson(5));
            System.out.println(System.getenv("DATABASE_TYPE"));

            instance.createUser("samantharose", "horny", System.currentTimeMillis(), System.currentTimeMillis(), "yes@yes.horny");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
