package ai.humnod.genai.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.ViewHolder> {
    private final Context context;
    private final List<Integer> layouts;

    public OnboardingAdapter(Context context, List<Integer> layouts) {
        this.context = context;
        this.layouts = layouts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(viewType, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // No additional binding needed as layouts are pre-defined
    }

    @Override
    public int getItemCount() {
        return layouts.size();
    }

    @Override
    public int getItemViewType(int position) {
        return layouts.get(position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}

