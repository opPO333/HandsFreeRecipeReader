package app.recipe.reader.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import app.recipe.reader.R;
import app.recipe.reader.data.AppDatabase;
import app.recipe.reader.data.Category;
import app.recipe.reader.data.Recipe;
import app.recipe.reader.data.RecipeStep;

public class AddRecipeActivity extends AppCompatActivity {

    private AppDatabase db;
    private List<Category> allCategories = new ArrayList<>();

    private TextInputEditText editTitle;
    private TextInputEditText editTime;
    private AutoCompleteTextView dropdownCategory;
    private LinearLayout stepsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        db = AppDatabase.getInstance(this);

        editTitle = findViewById(R.id.edit_title);
        editTime = findViewById(R.id.edit_time);
        editTime.setText("0");
        dropdownCategory = findViewById(R.id.dropdown_category);
        stepsContainer = findViewById(R.id.steps_container);

        setupToolbar();
        setupCategories();
        setupSteps();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_save) {
                saveRecipe();
                return true;
            }
            return false;
        });
    }

    private void setupCategories() {
        db.recipeDao().getAllCategories().observe(this, categories -> {
            allCategories = categories;
            List<String> categoryNames = new ArrayList<>();
            categoryNames.add("None");
            for (Category c : categories) {
                categoryNames.add(c.getName());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categoryNames);
            dropdownCategory.setAdapter(adapter);
            if (!categoryNames.isEmpty()) {
                dropdownCategory.setText(categoryNames.get(0), false);
            }
        });
    }

    private void setupSteps() {
        findViewById(R.id.btn_add_step).setOnClickListener(v -> {
            if (stepsContainer.getChildCount() > 0) {
                View lastStep = stepsContainer.getChildAt(stepsContainer.getChildCount() - 1);
                TextInputEditText editDesc = lastStep.findViewById(R.id.edit_step_desc);
                if (TextUtils.isEmpty(editDesc.getText().toString().trim())) {
                    editDesc.setError("Fill this step first!");
                    editDesc.requestFocus();
                    return;
                }
            }
            addStepView("");
        });
        addStepView("");
    }

    private void addStepView(String initialText) {
        View stepView = LayoutInflater.from(this).inflate(R.layout.item_add_step, stepsContainer, false);
        
        TextInputEditText editDesc = stepView.findViewById(R.id.edit_step_desc);
        editDesc.setText(initialText);

        ImageButton btnDelete = stepView.findViewById(R.id.btn_delete_step);
        btnDelete.setOnClickListener(v -> {
            stepsContainer.removeView(stepView);
            updateStepNumbers();
        });

        stepsContainer.addView(stepView);
        updateStepNumbers();
    }

    private void updateStepNumbers() {
        for (int i = 0; i < stepsContainer.getChildCount(); i++) {
            View stepView = stepsContainer.getChildAt(i);
            TextView stepNumber = stepView.findViewById(R.id.step_number);
            stepNumber.setText(String.valueOf(i + 1));
        }
    }

    private void saveRecipe() {
        String title = editTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            editTitle.setError("Title is required!");
            return;
        }

        String timeStr = editTime.getText().toString().trim();
        int time = 0;
        if (!TextUtils.isEmpty(timeStr)) {
            try {
                time = Integer.parseInt(timeStr);
            } catch (NumberFormatException e) {
                editTime.setError("Invalid time!");
                return;
            }
        }

        String categoryName = dropdownCategory.getText().toString();
        Integer categoryId = null;
        if (!categoryName.equals("None")) {
            for (Category c : allCategories) {
                if (c.getName().equals(categoryName)) {
                    categoryId = c.getId();
                    break;
                }
            }
        }

        List<String> stepsText = new ArrayList<>();
        for (int i = 0; i < stepsContainer.getChildCount(); i++) {
            View stepView = stepsContainer.getChildAt(i);
            TextInputEditText editDesc = stepView.findViewById(R.id.edit_step_desc);
            String desc = editDesc.getText().toString().trim();
            if (!TextUtils.isEmpty(desc)) {
                stepsText.add(desc);
            }
        }

        if (stepsText.isEmpty()) {
            Toast.makeText(this, "Please add at least one step!", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer finalCategoryId = categoryId;
        int finalTime = time;
        AppDatabase.databaseExecutor.execute(() -> {
            Recipe recipe = new Recipe(title, finalTime, finalCategoryId);
            long recipeId = db.recipeDao().insertRecipe(recipe);

            List<RecipeStep> steps = new ArrayList<>();
            for (int i = 0; i < stepsText.size(); i++) {
                steps.add(new RecipeStep((int) recipeId, i + 1, stepsText.get(i)));
            }
            db.recipeDao().insertSteps(steps);

            runOnUiThread(() -> {
                Toast.makeText(this, "Recipe saved!", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}
