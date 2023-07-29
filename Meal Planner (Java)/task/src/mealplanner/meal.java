package mealplanner;
import java.util.*;

public class meal {
    String category;
    String name;
    int meal_id;
    List<String> ingredients;

    public meal(String category, String name, int meal_id) {
        this.category = category;
        this.name = name;
        this.meal_id = meal_id;
    }

    public meal(String category, String name, int meal_id, List<String> ingredients) {
        this.category = category;
        this.name = name;
        this.meal_id = meal_id;
        this.ingredients = ingredients;
    }
}
