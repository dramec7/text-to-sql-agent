package com.holin.service;

import com.holin.config.Agent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author holin
 * @date 2025/12/21
 */
@Service
public class ChatService {

    @Autowired
    private Agent agent;

    public String query(String question) {
        return agent.query(question);
    }
}
