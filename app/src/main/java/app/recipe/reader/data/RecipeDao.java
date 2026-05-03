package app.recipe.reader.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

import androidx.lifecycle.LiveData;

@Dao
public interface RecipeDao {
    @Transaction
    @Query("SELECT * FROM recipes")
    LiveData<List<RecipeWithCategory>> getAllRecipesWithCategory();

    @Query("SELECT * FROM recipes")
    List<Recipe> getAllRecipes();

    @Query("SELECT * FROM recipes WHERE categoryId = :categoryId")
    List<Recipe> getRecipesByCategory(Integer categoryId);

    @Query("SELECT * FROM recipe_steps WHERE recipeId = :recipeId ORDER BY stepNumber ASC")
    List<RecipeStep> getStepsForRecipe(int recipeId);

    @Insert
    long insertRecipe(Recipe recipe);

    @Insert
    void insertSteps(List<RecipeStep> steps);

    @Update
    void updateRecipe(Recipe recipe);

    @Delete
    void deleteRecipe(Recipe recipe);

    @Query("DELETE FROM recipes WHERE id = :recipeId")
    void deleteRecipeById(int recipeId);

    @Insert
    long insertCategory(Category category);

    @Query("SELECT * FROM categories")
    LiveData<List<Category>> getAllCategories();

    @Query("SELECT * FROM categories WHERE id = :id")
    Category getCategoryById(int id);
}
