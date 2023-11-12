package utils;


public class ApiPathUtil {

    public  static String CACHED_GET = "cachedGet";
    public  static String CACHED_PAGINATED_GET = "cachedPaginatedGet";
    public  static String CUSTOM_GET = "customGet";
    public  static String PROXY_GET = "proxyGet";
    public static String HEALTH_CHECK_PATH = "/healthcheck";

    public enum VIEW_PATH {
        PATH_VIEW_BOTTOM_FORK("/view/bottom/:N/forks"),
        PATH_VIEW_BOTTOM_ISSUE("/view/bottom/:N/open_issues"),
        PATH_VIEW_BOTTOM_STAR ("/view/bottom/:N/stars"),
        PATH_VIEW_BOTTOM_LAST_UPDATED ("/view/bottom/:N/last_updated");

        public final String value;

        VIEW_PATH(String value) {
            this.value = value;
        }
    }

    public enum PROXY_PATH_REFRESH {
        PATH_HOME("/"),
        PATH_ORG_NETFLIX("/orgs/Netflix");

        public final String value;

        PROXY_PATH_REFRESH(String value) {
            this.value = value;
        }
    }

    public enum PROXY_PATH_PAGINATED_REFRESH {
        PATH_ORG_NETFLIX_MEMBERS("/orgs/Netflix/members"),
        PATH_ORG_NETFLIX_REPOS ("/orgs/Netflix/repos");

        public final String value;

        PROXY_PATH_PAGINATED_REFRESH(String value) {
            this.value = value;
        }
    }

}
