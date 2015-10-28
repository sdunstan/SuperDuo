package it.jaschke.alexandria.util;

import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

public class ViewHelper {

    public void setAuthors(String authors, TextView authorsView) {
        if (StringUtils.isNotEmpty(authors)) {
            String[] authorsArr = authors.split(",");
            authorsView.setLines(authorsArr.length);
            authorsView.setText(authors.replace(",", "\n"));
        }
        else {
            authorsView.setText("");
        }
    }

    public void setCategories(String categories, TextView categoriesView) {
        if (StringUtils.isNotEmpty(categories)) {
            categoriesView.setText(categories);
        }
        else {
            categoriesView.setText("");
        }
    }

}
