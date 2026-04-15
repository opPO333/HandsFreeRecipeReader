package app.recipe.reader.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.SET_NULL;

@Entity(
        tableName = "recipes",
        foreignKeys = @ForeignKey(
                entity = Category.class,
                parentColumns = "id",
                childColumns = "categoryId",
                onDelete = SET_NULL
        ),
        indices = {@Index("categoryId")}
)
public class Recipe {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String title;
    private int cookingTimeMinutes;
    private Integer categoryId;
    private boolean isFavorite;

    public Recipe(String title, int cookingTimeMinutes, Integer categoryId) {
        this.title = title;
        this.cookingTimeMinutes = cookingTimeMinutes;
        this.categoryId = categoryId;
        this.isFavorite = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getCookingTimeMinutes() {
        return cookingTimeMinutes;
    }

    public void setCookingTimeMinutes(int cookingTimeMinutes) {
        this.cookingTimeMinutes = cookingTimeMinutes;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }
}
