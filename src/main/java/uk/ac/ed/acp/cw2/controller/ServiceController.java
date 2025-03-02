package uk.ac.ed.acp.cw2.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.acp.cw2.data.RuntimeEnvironment;

@RestController()
public class ServiceController {

    private final RuntimeEnvironment environment;

    public ServiceController(RuntimeEnvironment environment) {
        this.environment = environment;
    }


    @GetMapping("/")
    public String index() {
        StringBuilder currentEnv = new StringBuilder();
        currentEnv.append("<ul>");
        System.getenv().keySet().forEach(key -> currentEnv.append("<li>").append(key).append(" = ").append(System.getenv(key)).append("</li>"));
        currentEnv.append("</ul>");

        return "<html><body>" +
                "<h1>Welcome from ACP CW2</h1>" +
                "<h2>Environment variables </br><div> " + currentEnv.toString() + "</div></h2>" +
                "</body></html>";
    }

    @GetMapping("/uuid")
    public String uuid() {
        return "s12345678";
    }

    @GetMapping("/cache")
    public String cache() {
        return "cache";
    }

}
