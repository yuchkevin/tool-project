package com.yuch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ToolProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToolProjectApplication.class, args);
        System.err.println("项目启动成功！");
    }

}
