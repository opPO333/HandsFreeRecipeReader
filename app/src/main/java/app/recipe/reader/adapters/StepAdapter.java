package app.recipe.reader.adapters;

import app.recipe.reader.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import app.recipe.reader.data.RecipeStep;

public class StepAdapter extends RecyclerView.Adapter<StepAdapter.StepViewHolder> {

    private List<RecipeStep> steps = new ArrayList<>();

    public void setSteps(List<RecipeStep> steps) {
        this.steps = steps;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.step_item, parent, false);
        return new StepViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StepViewHolder holder, int position) {
        RecipeStep step = steps.get(position);
        holder.stepNumber.setText(String.valueOf(step.getStepNumber()));
        holder.stepDescription.setText(step.getStepDescription());
    }

    @Override
    public int getItemCount() {
        return steps.size();
    }

    static class StepViewHolder extends RecyclerView.ViewHolder {
        TextView stepNumber;
        TextView stepDescription;

        public StepViewHolder(@NonNull View itemView) {
            super(itemView);
            stepNumber = itemView.findViewById(R.id.step_number);
            stepDescription = itemView.findViewById(R.id.step_description);
        }
    }
}
