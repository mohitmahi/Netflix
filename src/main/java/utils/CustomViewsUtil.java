package utils;

public class CustomViewsUtil {

    public enum CUSTOM_VIEWS {
        forks("forks", "Bottom_N_forks"),
        open_issues("open_issues", "Bottom_N_open_issues"),
        stars ("stargazers_count", "Bottom_N_stars"),
        last_updated ("updated_at", "Bottom_N_last_updated");

        public final String viewName;
        public final String setName;

        CUSTOM_VIEWS(String viewName, String setName) {
            this.viewName = viewName;
            this.setName = setName;
        }
    }
}
