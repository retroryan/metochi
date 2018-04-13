package metochi;

import java.util.Optional;

public class EnvVars {
    //this is the name of the node that is used to load the config file - i.e. one,two, three
    public static final String NODE_NAME = Optional.ofNullable(System.getenv("NODE_NAME")).orElse("one");
}
