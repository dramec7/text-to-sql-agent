package com.holin.controller;

import com.holin.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author holin
 * @date 2025/12/21
 */
@RestController
@RequestMapping("/chat")
public class AgentController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/query")
    public String query(@RequestParam String userId, @RequestBody String query){
        return chatService.query(userId, query);
    }
}
