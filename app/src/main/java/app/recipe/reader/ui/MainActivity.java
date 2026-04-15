package app.recipe.reader.ui;

import app.recipe.reader.R;
import app.recipe.reader.adapters.RecipeAdapter;
import app.recipe.reader.data.Category;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import app.recipe.reader.data.AppDatabase;
import app.recipe.reader.data.RecipeWithCategory;

public class MainActivity extends AppCompatActivity {

    private RecipeAdapter adapter;
    private AppDatabase db;
    private List<RecipeWithCategory> allRecipes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getInstance(this);

        setupRecyclerView();
        setupFilters();
        setupFab();
        loadRecipes();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recipes_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new RecipeAdapter(new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onRecipeClick(RecipeWithCategory item) {
                Intent intent = new Intent(MainActivity.this, RecipeDetailActivity.class);
                intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, item.recipe.getId());
                intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_TITLE, item.recipe.getTitle());
                String categoryName = item.category != null ? item.category.getName() : "Uncategorized";
                intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_INFO, categoryName + " • " + item.recipe.getCookingTimeMinutes() + " min");
                startActivity(intent);
            }

            @Override
            public void onFavoriteClick(int position) {
                RecipeWithCategory item = adapter.getItem(position);
                item.recipe.setFavorite(!item.recipe.isFavorite());
                AppDatabase.databaseExecutor.execute(() -> {
                    db.recipeDao().updateRecipe(item.recipe);
                });
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupFilters() {
        AutoCompleteTextView categoryDropdown = findViewById(R.id.category_dropdown);
        
        db.recipeDao().getAllCategories().observe(this, categories -> {
            List<String> categoryNames = new ArrayList<>();
            categoryNames.add("All");
            for (Category category : categories) {
                categoryNames.add(category.getName());
            }
            
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    categoryNames
            );
            categoryDropdown.setAdapter(arrayAdapter);
        });
    }

    private void loadRecipes() {
        db.recipeDao().getAllRecipesWithCategory().observe(this, recipes -> {
            allRecipes = recipes;
            adapter.setRecipes(allRecipes);
        });
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.add_recipe);
        fab.setOnClickListener(v -> {
            Toast.makeText(this, "Add recipe coming soon!", Toast.LENGTH_SHORT).show();
        });
    }
}
