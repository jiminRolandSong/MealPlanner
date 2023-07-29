package mealplanner;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.sql.*;


public class Main {
   static Random random = new Random();

    static Scanner scanner = new Scanner(System.in);


    public static String cateChecker(){
        ArrayList<String> available = new ArrayList<>();
        available.add("breakfast");
        available.add("lunch");
        available.add("dinner");
        String category = scanner.nextLine();
        while (true){
            if (available.contains(category)) return category;
            else{
                System.out.println("Wrong meal category! Choose from: breakfast, lunch, dinner.");
                category = scanner.nextLine();
            }
        }
    }
    public static String mealChecker(){
        String meal = scanner.nextLine();
        boolean onlyLetters = meal.trim().matches("[a-zA-Z\\s]+");
        while(!onlyLetters){
            System.out.println("Wrong format. Use letters only!");
            meal = scanner.nextLine();
            onlyLetters = meal.matches("[a-zA-Z]+");
        }
        return meal;
    }
    public static String[] ingChecker(){
        while (true) {
            String ing = scanner.nextLine();
            String[] ingredients = ing.split(",\\s?");
            boolean onlyLetters = true;
            for (String i : ingredients) {
                if (!i.trim().matches("[a-zA-Z\\s?]+") || i.isEmpty()){
                    onlyLetters = false;
                    System.out.println("Wrong format. Use letters only!");
                    break;
                }
            }
            if (onlyLetters) {
                return ingredients;
            }
        }
    }

    public static void add(Statement statement) throws SQLException {
        int mealId = random.nextInt(1000);
        System.out.println("Which meal do you want to add (breakfast, lunch, dinner)?");
        String category = cateChecker();
        System.out.println("Input the meal's name:");
        String name = mealChecker();
        System.out.println("Input the ingredients:");
        String[] ingredients = ingChecker();
        System.out.println("The meal has been added!");
        statement.executeUpdate(String.format("insert into meals (category,meal,meal_id) values ('%s','%s',%d)",category,name,mealId));
        for (String ing : ingredients ) {
            int ingId = random.nextInt(100000);
            statement.executeUpdate(String.format("insert into ingredients(ingredient,ingredient_id,meal_id) values ('%s', %d, %d)", ing, ingId, mealId));
        }
    }

    public static void show(Statement statement) throws SQLException {
        System.out.println("Which category do you want to print (breakfast, lunch, dinner)?");
        String cate = cateChecker();

        List<meal> meals = new ArrayList<>();
        List<String> cateList = new ArrayList<>();
        try (ResultSet ms = statement.executeQuery("SELECT * FROM meals")) {
            while (ms.next()) {
                int meal_id = ms.getInt("meal_id");
                String category = ms.getString("category");
                cateList.add(category);
                String mealName = ms.getString("meal");
                meals.add(new meal(category, mealName, meal_id));
            }
        }

        for (meal m : meals) {
            int id = m.meal_id;
            List<String> ingredientsList = new ArrayList<>();
            try (ResultSet is = statement.executeQuery("SELECT * FROM ingredients WHERE meal_id = " + id)) {
                while (is.next()) {
                    ingredientsList.add(is.getString("ingredient"));
                }
            }
            m.ingredients = ingredientsList;
        }
        if (cateList.contains(cate)){
            System.out.println("Category: " + cate);
        }else{
            System.out.println("No meals found.");
        }

        for (meal m : meals) {
            if(Objects.equals(m.category, cate)) {
                System.out.println("Name: " + m.name);
                System.out.println("Ingredients:");
                for (String ingredient : m.ingredients) {
                    System.out.println(ingredient);
                }
            }
        }
    }

    public static void plan(Statement statement) throws SQLException{
        statement.executeUpdate("TRUNCATE TABLE plan");
        String[] week = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        String[] day = {"breakfast", "lunch", "dinner"};
        Map<String, Integer> mealIdMap = new HashMap<>();
        Map<String, Map<String,String>> weekList = new HashMap<>();

        for (String d : week) {
            System.out.println(d);
            Map<String, String> dayMap = new HashMap<>();
            for (String when : day) {
                List<String> names = new ArrayList<>();
                try (ResultSet ms = statement.executeQuery("SELECT * FROM meals" )) {
                    while (ms.next()) {
                        int meal_id = ms.getInt("meal_id");
                        String category = ms.getString("category");
                        String mealName = ms.getString("meal");
                        if (Objects.equals(category, when)){
                            names.add(mealName);
                            mealIdMap.put(mealName, meal_id);
                        }
                    }
                }
                Collections.sort(names);
                for (String name : names){
                    System.out.println(name);
                }
                System.out.printf("Choose the %s for %s from the list above:\n",when, d);
                String choice = scanner.nextLine();
                while(!names.contains(choice)){
                    System.out.println("This meal doesn’t exist. Choose a meal from the list above.");
                    choice = scanner.nextLine();
                }
                dayMap.put(when, choice);
            }
            weekList.put(d, dayMap);
            System.out.printf("Yeah! We planned the meals for %s.\n",d);
        }
        for (String d: week){
            Map<String, String> today = weekList.get(d);
            System.out.println(d);
            for (String when: day){
                String now = today.get(when);
                System.out.println(when + ": " + now);
                statement.executeUpdate(String.format("insert into plan (option, category, meal_id) values ('%s', '%s', '%d')", d, when, mealIdMap.get(now)));
            }
        }
    }
    public static void save(Statement statement) throws SQLException, IOException {
        System.out.println("Input a filename:");
        String name = scanner.nextLine();
        File file = new File(name);

        List<Integer> meals = new ArrayList<>();
        try (ResultSet ms = statement.executeQuery("SELECT * FROM plan")) {
            while (ms.next()) {
                int meal_id = ms.getInt("meal_id");
                meals.add(meal_id);
            }
        }
        ArrayList<List<String>>  ings = new ArrayList<>();
        for (int m : meals) {
            int id = m;
            List<String> ingredientsList = new ArrayList<>();
            try (ResultSet is = statement.executeQuery("SELECT * FROM ingredients WHERE meal_id = " + id)) {
                while (is.next()) {
                    ingredientsList.add(is.getString("ingredient"));
                }
            }
            ings.add(ingredientsList);
        }
        HashMap<String, Integer> grocery = new HashMap<>();
        for (List<String> set : ings){
            for(String ing : set){
                if(grocery.keySet().contains(ing)) {
                    grocery.replace(ing, grocery.get(ing), grocery.get(ing) + 1);
                } else{
                    grocery.put(ing, 1);
                }
            }
        }
        try (FileWriter writer = new FileWriter(file)){
            for (String n: grocery.keySet()){
                if (grocery.get(n) == 1){
                    writer.write(n +"\n");
                } else{
                    writer.write(n + " x" + grocery.get(n) +"\n");
                }
            }
        } catch (IOException e){
        }
        System.out.println("Saved!");
    }


    public static void main(String[] args) throws SQLException, IOException {
        String DB_URL = "jdbc:postgresql:meals_db";
        String USER = "postgres";
        String PASS = "1111";

        Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
        connection.setAutoCommit(true);

        Statement statement = connection.createStatement();
        statement.executeUpdate("create table if not exists meals (" +
                "category varchar(1024)," +
                "meal varchar(1024)," +
                "meal_id integer" +
                ")");

        statement.executeUpdate("create table if not exists ingredients (" +
                "ingredient varchar(1024)," +
                "ingredient_id integer," +
                "meal_id integer" +
                ")");

        statement.executeUpdate("create table if not exists plan (" +
                "option varchar(1024)," +
                "category varchar(1024)," +
                "meal_id integer" +
                ")");
        Scanner scanner = new Scanner(System.in);
        System.out.println("What would you like to do (add, show, plan, save, exit)?");
        String action = scanner.nextLine();
        while (true){
            if (Objects.equals(action, "add")) {
                add(statement);
            } else if (Objects.equals(action, "show")){
                show(statement);
            } else if (Objects.equals(action,"exit")) {
                break;
            } else if (Objects.equals(action, "plan")){
                plan(statement);
            } else if (Objects.equals(action, "save")) {
                try (ResultSet is = statement.executeQuery("SELECT * FROM plan")) {
                    if (is.next()) {
                        save(statement);
                    } else {
                        System.out.println("Unable to save. Plan your meals first.");
                    }
                }
            }
            System.out.println("What would you like to do (add, show, plan, save, exit)?");
            action = scanner.nextLine();
        }
        System.out.println("Bye!");
        statement.close();
        connection.close();
    }
}