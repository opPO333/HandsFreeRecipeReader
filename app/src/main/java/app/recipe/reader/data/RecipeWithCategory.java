package app.recipe.reader.data;

import androidx.room.Embedded;
import androidx.room.Relation;

public class RecipeWithCategory {
    @Embedded
    public Recipe recipe;

    @Relation(
            parentColumn = "categoryId",
            entityColumn = "id"
    )
    public Category category;
}
