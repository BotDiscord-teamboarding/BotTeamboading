package com.meli.teamboardingBot.service;

import com.meli.teamboardingBot.client.ClientBoarding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class SquadLogService {

    @Autowired
    private ClientBoarding clientBoarding;

    public String getSquads() {
        return  clientBoarding.getSquads();
    }

    public String getSquadLogTypes() {
        return clientBoarding.getSquadLogTypes();
    }

    public String getSquadCategories() {
        return clientBoarding.getSquadCategories();
    }

    public ResponseEntity<String> createSquadLog(String payload) {

        return  clientBoarding.createSquadLog(payload);
    }
}