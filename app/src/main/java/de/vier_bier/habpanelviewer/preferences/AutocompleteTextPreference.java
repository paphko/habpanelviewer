package de.vier_bier.habpanelviewer.preferences;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

/**
 * EditTextPreference with auto completion.
 */
class AutocompleteTextPreference extends EditTextPreference {
    private final AutoCompleteTextView mTextView;

    public AutocompleteTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mTextView = new AutoCompleteTextView(context, attrs);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        EditText editText = mTextView;
        editText.setText(getText());

        ViewParent oldParent = editText.getParent();
        if (oldParent != view) {
            if (oldParent != null) {
                ((ViewGroup) oldParent).removeView(editText);
            }
            onAddEditTextToDialogView(view, editText);
        }
    }

    @Override
    protected void showDialog(android.os.Bundle state) {
        try {
            super.showDialog(state);
        } catch (NullPointerException e) {
            // Handle NPE when WindowInsetsController is null on certain devices/versions
            if (e.getMessage() != null && e.getMessage().contains("WindowInsetsController")) {
                // Fallback: create and show dialog manually without relying on showDialog
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(getTitle());

                View view = new View(getContext());
                onBindDialogView(view);
                builder.setView(view);

                builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    onDialogClosed(true);
                });
                builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    onDialogClosed(false);
                });

                AlertDialog dialog = builder.create();
                if (state != null) {
                    dialog.onRestoreInstanceState(state);
                }
                dialog.show();
            } else {
                throw e;
            }
        }
    }

    /**
     * Returns the {@link AutoCompleteTextView} widget that will be shown in the
     * dialog.
     *
     * @return The {@link AutoCompleteTextView} widget that will be shown in the
     *         dialog.
     */
    public AutoCompleteTextView getEditText() {
        return mTextView;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            String value = getEditText().getText().toString();
            if (callChangeListener(value)) {
                setText(value);
            }
        }
    }
}
