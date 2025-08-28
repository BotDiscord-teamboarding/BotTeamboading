package com.meli.teamboardingBot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LanguageDTO {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
}
