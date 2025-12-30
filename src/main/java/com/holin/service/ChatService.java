package com.holin.service;

import com.holin.config.Agent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author holin
 */
@Service
public class ChatService {

    @Autowired
    private Agent agent;

    public String query(String userId, String question) {
        return agent.query(userId, question);
    }
}
