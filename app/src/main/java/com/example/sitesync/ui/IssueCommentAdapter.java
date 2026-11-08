package com.example.sitesync.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sitesync.databinding.ItemIssueCommentBinding;
import com.example.sitesync.models.IssueComment;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for rendering technical replies and clarification comments on an RFI.
 */
public class IssueCommentAdapter extends RecyclerView.Adapter<IssueCommentAdapter.CommentViewHolder> {

    private final List<IssueComment> comments = new ArrayList<>();

    public void setComments(List<IssueComment> list) {
        comments.clear();
        if (list != null) {
            comments.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemIssueCommentBinding binding = ItemIssueCommentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new CommentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        holder.bind(comments.get(position));
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        private final ItemIssueCommentBinding binding;

        public CommentViewHolder(@NonNull ItemIssueCommentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(IssueComment comment) {
            binding.tvCommentAuthor.setText(comment.getAuthorName());
            binding.tvCommentRole.setText(comment.getAuthorRole());
            binding.tvCommentTimestamp.setText(comment.getTimestamp());
            binding.tvCommentMessage.setText(comment.getMessage());
        }
    }
}
