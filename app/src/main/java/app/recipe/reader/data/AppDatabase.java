package app.recipe.reader.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Recipe.class, RecipeStep.class, Category.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public abstract RecipeDao recipeDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "recipe_database")
                            .addCallback(sRoomDatabaseCallback)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private final static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            databaseExecutor.execute(() -> {
                RecipeDao dao = INSTANCE.recipeDao();

                dao.insertCategory(new Category("Snacks"));
                dao.insertCategory(new Category("Drinks"));
                long saladsId = dao.insertCategory(new Category("Salads"));
                long soupsId = dao.insertCategory(new Category("Soups"));
                long bakingId = dao.insertCategory(new Category("Baking"));
                dao.insertCategory(new Category("Vegetarian"));
                dao.insertCategory(new Category("Seafood"));
                dao.insertCategory(new Category("Meat"));
                dao.insertCategory(new Category("Appetizers"));
                dao.insertCategory(new Category("Sauces"));

                long recipeId1 = dao.insertRecipe(new Recipe("Greek Salad", 15, (int) saladsId));
                dao.insertSteps(Arrays.asList(
                    new RecipeStep((int) recipeId1, 1, "Chop tomatoes, cucumbers, and red onion."),
                    new RecipeStep((int) recipeId1, 2, "Add Kalamata olives and sliced feta cheese."),
                    new RecipeStep((int) recipeId1, 3, "Drizzle with olive oil and sprinkle with dried oregano.")
                ));

                long recipeId2 = dao.insertRecipe(new Recipe("Tomato Soup", 30, (int) soupsId));
                dao.insertSteps(Arrays.asList(
                    new RecipeStep((int) recipeId2, 1, "Sauté onions and garlic in a pot."),
                    new RecipeStep((int) recipeId2, 2, "Add canned tomatoes and vegetable broth."),
                    new RecipeStep((int) recipeId2, 3, "Simmer for 20 minutes and blend until smooth.")
                ));

                long recipeId3 = dao.insertRecipe(new Recipe("Simple Brownies", 45, (int) bakingId));
                dao.insertSteps(Arrays.asList(
                    new RecipeStep((int) recipeId3, 1, "Melt butter and mix with sugar and cocoa powder."),
                    new RecipeStep((int) recipeId3, 2, "Add eggs and flour, stir until combined."),
                    new RecipeStep((int) recipeId3, 3, "Bake at 180°C for 25 minutes.")
                ));

                long testId = dao.insertRecipe(new Recipe("test", 1000000, null));
                dao.insertSteps(Arrays.asList(
                    new RecipeStep((int) testId, 1, "testtesttesttesttesttesttesttesttesttesttesttest"),
                    new RecipeStep((int) testId, 2, "test"),
                    new RecipeStep((int) testId, 3, "test"),
                    new RecipeStep((int) testId, 4, "test"),
                    new RecipeStep((int) testId, 5, "test"),
                    new RecipeStep((int) testId, 6, "test"),
                    new RecipeStep((int) testId, 7, "test"),
                    new RecipeStep((int) testId, 8, "test"),
                    new RecipeStep((int) testId, 9, "test"),
                    new RecipeStep((int) testId, 10, "test"),
                    new RecipeStep((int) testId, 11, "test"),
                    new RecipeStep((int) testId, 12, "test"),
                    new RecipeStep((int) testId, 13, "test")
                ));
            });
        }
    };
}
