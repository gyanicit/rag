package com.icit.rag.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rag/api/v1/")
public class RagController {

    @GetMapping("greeting")
    public String sayHello(){
        return "hello";
    }
}
