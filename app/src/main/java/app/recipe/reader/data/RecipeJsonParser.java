package app.recipe.reader.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RecipeJsonParser {

    public static String serialize(Recipe recipe, List<RecipeStep> steps) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("title", recipe.getTitle());
        json.put("cookingTimeMinutes", recipe.getCookingTimeMinutes());
        if (recipe.getCategoryId() != null) {
            json.put("categoryId", recipe.getCategoryId());
        }

        JSONArray stepsArray = new JSONArray();
        for (RecipeStep step : steps) {
            JSONObject stepJson = new JSONObject();
            stepJson.put("stepNumber", step.getStepNumber());
            stepJson.put("stepDescription", step.getStepDescription());
            stepsArray.put(stepJson);
        }
        json.put("steps", stepsArray);

        return json.toString();
    }

    public static ParsedRecipe deserialize(String jsonString) throws JSONException {
        JSONObject json = new JSONObject(jsonString);
        
        String title = json.getString("title");
        int cookingTimeMinutes = json.getInt("cookingTimeMinutes");
        Integer categoryId = json.has("categoryId") ? json.getInt("categoryId") : null;

        Recipe recipe = new Recipe(title, cookingTimeMinutes, categoryId);

        List<RecipeStep> steps = new ArrayList<>();
        JSONArray stepsArray = json.getJSONArray("steps");
        for (int i = 0; i < stepsArray.length(); i++) {
            JSONObject stepJson = stepsArray.getJSONObject(i);
            int stepNumber = stepJson.getInt("stepNumber");
            String stepDescription = stepJson.getString("stepDescription");
            steps.add(new RecipeStep(0, stepNumber, stepDescription));
        }

        return new ParsedRecipe(recipe, steps);
    }

    public static class ParsedRecipe {
        public Recipe recipe;
        public List<RecipeStep> steps;

        public ParsedRecipe(Recipe recipe, List<RecipeStep> steps) {
            this.recipe = recipe;
            this.steps = steps;
        }
    }
}
