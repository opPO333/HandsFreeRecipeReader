package app.recipe.reader.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(
    tableName = "recipe_steps",
    foreignKeys = @ForeignKey(
        entity = Recipe.class,
        parentColumns = "id",
        childColumns = "recipeId",
        onDelete = CASCADE
    ),
    indices = {
        @Index(value = {"recipeId", "stepNumber"}, unique = true)
    }
)
public class RecipeStep {
    @PrimaryKey(autoGenerate = true)
    private int stepId;

    private int recipeId;
    private int stepNumber;
    private String stepDescription;

    public RecipeStep(int recipeId, int stepNumber, String stepDescription) {
        this.recipeId = recipeId;
        this.stepNumber = stepNumber;
        this.stepDescription = stepDescription;
    }

    public int getStepId() {
        return stepId;
    }

    public void setStepId(int stepId) {
        this.stepId = stepId;
    }

    public int getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(int recipeId) {
        this.recipeId = recipeId;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(int stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getStepDescription() {
        return stepDescription;
    }

    public void setStepDescription(String stepDescription) {
        this.stepDescription = stepDescription;
    }
}
