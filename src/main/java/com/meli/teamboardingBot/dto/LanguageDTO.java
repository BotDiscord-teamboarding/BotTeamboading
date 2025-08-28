package com.meli.teamboardingBot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
