package kr.co.budget;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.web.context.WebApplicationContext;

@SpringBootApplication
public class BudgetApplication extends SpringBootServletInitializer {
    public static void main(String[] args) {
        var context = SpringApplication.run(BudgetApplication.class, args);
        if (!(context instanceof WebApplicationContext)) {
            System.exit(SpringApplication.exit(context));
        }
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(BudgetApplication.class);
    }
}
