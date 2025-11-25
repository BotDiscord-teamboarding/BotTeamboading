package com.meli.teamboardingBot.service.batch.impl;

import com.meli.teamboardingBot.domain.batch.BatchLogEntry;
import com.meli.teamboardingBot.service.batch.TextParser;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RegexTextParsingService implements TextParser {
    
    private static final DateTimeFormatter BRAZILIAN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    
    private static final Pattern FULL_PATTERN = Pattern.compile(
        "(?i)(?:squad\\s+)?([\\w\\s]+?)\\s*-\\s*([\\w\\s]+?)\\s*-\\s*([\\w\\s]+?)\\s*-\\s*([\\w\\s,]+?)\\s*-\\s*(\\d{2}-\\d{2}-\\d{4})(?:\\s*a\\s*(\\d{2}-\\d{2}-\\d{4}))?(?:\\s*-\\s*(.+))?",
        Pattern.MULTILINE
    );
    
    private static final Pattern SIMPLE_PATTERN = Pattern.compile(
        "([\\w\\s]+?)\\s*-\\s*([\\w\\s]+?)\\s*-\\s*([\\w\\s]+?)\\s*-\\s*([\\w\\s,]+?)\\s*-\\s*(\\d{2}-\\d{2}-\\d{4})(?:\\s*a\\s*(\\d{2}-\\d{2}-\\d{4}))?",
        Pattern.MULTILINE
    );

    @Override
    public List<BatchLogEntry> parseText(String inputText) {
        List<BatchLogEntry> entries = new ArrayList<>();
        
        if (inputText == null || inputText.trim().isEmpty()) {
            return entries;
        }

        String[] lines = inputText.split("\n");
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            
            BatchLogEntry entry = parseLine(line, i + 1);
            if (entry != null) {
                entries.add(entry);
            }
        }
        
        return entries;
    }

    private BatchLogEntry parseLine(String line, int lineNumber) {
        Matcher fullMatcher = FULL_PATTERN.matcher(line);
        Matcher simpleMatcher = SIMPLE_PATTERN.matcher(line);
        
        if (fullMatcher.find()) {
            return createEntryFromMatcher(fullMatcher, lineNumber, true);
        } else if (simpleMatcher.find()) {
            return createEntryFromMatcher(simpleMatcher, lineNumber, false);
        }
        
        return null;
    }

    private BatchLogEntry createEntryFromMatcher(Matcher matcher, int lineNumber, boolean hasDescription) {
        try {
            String squadName = matcher.group(1).trim();
            String personName = matcher.group(2).trim();
            String logType = matcher.group(3).trim();
            String categoriesStr = matcher.group(4).trim();
            String startDateStr = matcher.group(5).trim();
            String endDateStr = matcher.group(6);
            String description = hasDescription && matcher.groupCount() >= 7 ? matcher.group(7) : null;

            List<String> categories = parseCategories(categoriesStr);
            LocalDate startDate = parseDate(startDateStr);
            LocalDate endDate = endDateStr != null ? parseDate(endDateStr.trim()) : null;

            if (description == null || description.trim().isEmpty()) {
                description = String.format("Log de %s - %s", logType, personName);
            }

            return new BatchLogEntry(squadName, personName, logType, categories, 
                                   description.trim(), startDate, endDate, lineNumber);
                                   
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> parseCategories(String categoriesStr) {
        List<String> categories = new ArrayList<>();
        String[] parts = categoriesStr.split(",");
        
        for (String part : parts) {
            String category = part.trim();
            if (!category.isEmpty()) {
                categories.add(category);
            }
        }
        
        return categories;
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, BRAZILIAN_DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Override
    public boolean canParse(String inputText) {
        if (inputText == null || inputText.trim().isEmpty()) {
            return false;
        }
        
        String[] lines = inputText.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            
            if (FULL_PATTERN.matcher(line).find() || SIMPLE_PATTERN.matcher(line).find()) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public String getParserName() {
        return "Regex Text Parser";
    }
}
