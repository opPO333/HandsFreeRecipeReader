package app.recipe.reader.adapters;

import app.recipe.reader.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import app.recipe.reader.data.RecipeWithCategory;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    public interface OnRecipeClickListener {
        void onRecipeClick(RecipeWithCategory recipeWithCategory);
        void onFavoriteClick(int position);
    }

    private List<RecipeWithCategory> recipes = new ArrayList<>();
    private final OnRecipeClickListener listener;

    public RecipeAdapter(OnRecipeClickListener listener) {
        this.listener = listener;
    }

    public void setRecipes(List<RecipeWithCategory> recipes) {
        this.recipes = recipes;
        notifyDataSetChanged();
    }

    public RecipeWithCategory getItem(int position) {
        return recipes.get(position);
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recipe_item, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        RecipeWithCategory item = recipes.get(position);
        holder.titleTextView.setText(item.recipe.getTitle());
        holder.timeTextView.setText(item.recipe.getCookingTimeMinutes() + " min");

        String categoryName = item.category != null ? item.category.getName() : "Uncategorized";
        holder.categoryTextView.setText(categoryName);

        if (item.recipe.isFavorite()) {
            holder.favoriteButton.setImageResource(android.R.drawable.btn_star_big_on);
        } else {
            holder.favoriteButton.setImageResource(android.R.drawable.btn_star_big_off);
        }

        holder.favoriteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFavoriteClick(holder.getAdapterPosition());
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecipeClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView categoryTextView;
        TextView timeTextView;
        ImageButton favoriteButton;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.recipe_title);
            categoryTextView = itemView.findViewById(R.id.recipe_category);
            timeTextView = itemView.findViewById(R.id.recipe_time);
            favoriteButton = itemView.findViewById(R.id.recipe_favorite);
        }
    }
}
