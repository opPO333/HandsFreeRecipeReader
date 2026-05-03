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
import com.google.android.material.search.SearchView;
import com.google.android.material.search.SearchBar;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import app.recipe.reader.data.AppDatabase;
import app.recipe.reader.data.RecipeWithCategory;

public class MainActivity extends AppCompatActivity {

    private RecipeAdapter adapter;
    private AppDatabase db;
    private List<RecipeWithCategory> allRecipes = new ArrayList<>();
    private String currentSearchQuery = "";
    private boolean currentFilterFavs = false;
    private String currentCategory = "All";

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

        SearchView searchView = findViewById(R.id.search_view);
        SearchBar searchBar = findViewById(R.id.search_bar);
        searchView.setupWithSearchBar(searchBar);
        searchView.getEditText().addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                applyFilters();
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        
        searchView.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            searchBar.setText(searchView.getText());
            searchView.hide();
            return false;
        });

        Chip chipFavorites = findViewById(R.id.chip_favorites);
        chipFavorites.setOnCheckedChangeListener((buttonView, isChecked) -> {
            currentFilterFavs = isChecked;
            applyFilters();
        });

        categoryDropdown.setOnItemClickListener((parent, view, position, id) -> {
            currentCategory = (String) parent.getItemAtPosition(position);
            applyFilters();
        });
    }

    private void applyFilters() {
        List<RecipeWithCategory> filtered = new ArrayList<>();
        String query = currentSearchQuery.toLowerCase().trim();
        for (RecipeWithCategory item : allRecipes) {
            if (currentFilterFavs && !item.recipe.isFavorite()) continue;
            
            if (!currentCategory.equals("All")) {
                String catName = item.category != null ? item.category.getName() : "Uncategorized";
                if (!catName.equals(currentCategory)) continue;
            }
            
            if (!query.isEmpty()) {
                if (!item.recipe.getTitle().toLowerCase().startsWith(query)) continue;
            }
            
            filtered.add(item);
        }
        adapter.setRecipes(filtered);
    }

    private void loadRecipes() {
        db.recipeDao().getAllRecipesWithCategory().observe(this, recipes -> {
            allRecipes = recipes;
            applyFilters();
        });
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.add_recipe);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddRecipeActivity.class);
            startActivity(intent);
        });
    }
}
