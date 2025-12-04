package com.meli.teamboardingBot.adapters.out.batch.impl;

import com.meli.teamboardingBot.adapters.out.batch.TextParser;
import com.meli.teamboardingBot.core.domain.batch.BatchLogEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class IntelligentTextParsingService implements TextParser {
    
    private static final DateTimeFormatter BRAZILIAN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    
    private static final Pattern INTELLIGENT_PATTERN = Pattern.compile(
        "([\\w\\s]+?)\\s*-\\s*([\\w\\s]+?)\\s*-\\s*([\\w\\s]+?)\\s*-\\s*([\\w\\s,]+?)\\s*-\\s*(\\d{2}-\\d{2}-\\d{4})(?:\\s*-\\s*(\\d{2}-\\d{2}-\\d{4}))?(?:\\s*-\\s*(.+))?",
        Pattern.MULTILINE | Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern NATURAL_PATTERN = Pattern.compile(
        "(?i)([\\w\\s]+?)(?:\\s+da\\s+squad\\s+|\\s+do\\s+squad\\s+|\\s+squad\\s+)?([\\w\\s]+?)(?:\\s+fazendo\\s+|\\s+fez\\s+|\\s+sobre\\s+|\\s+)?([\\w\\s]+?)(?:\\s+sobre\\s+|\\s+com\\s+|\\s+usando\\s+)?([\\w\\s,]+?)(?:\\s+em\\s+|\\s+de\\s+|\\s+para\\s+)(\\d{2}[-/]\\d{2}[-/]\\d{4})(?:\\s*(?:a|até)\\s*(\\d{2}[-/]\\d{2}[-/]\\d{4}))?",
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
            
            BatchLogEntry entry = parseLineIntelligently(line, i + 1);
            if (entry != null) {
                entries.add(entry);
                log.info("Parsed line {}: Squad='{}', Person='{}', Type='{}', Categories='{}'", 
                    i + 1, entry.getSquadName(), entry.getPersonName(), entry.getLogType(), entry.getCategories());
            } else {
                log.warn("Failed to parse line {}: '{}'", i + 1, line);
            }
        }
        
        return entries;
    }

    private BatchLogEntry parseLineIntelligently(String line, int lineNumber) {
        BatchLogEntry entry = tryStructuredFormat(line, lineNumber);
        if (entry != null) {
            return entry;
        }
        
        entry = tryNaturalFormat(line, lineNumber);
        if (entry != null) {
            return entry;
        }
        
        return tryFlexibleParsing(line, lineNumber);
    }

    private BatchLogEntry tryStructuredFormat(String line, int lineNumber) {
        Matcher matcher = INTELLIGENT_PATTERN.matcher(line);
        
        if (matcher.find()) {
            try {
                String squadName = cleanAndNormalize(matcher.group(1));
                String personName = cleanAndNormalize(matcher.group(2));
                String logType = cleanAndNormalize(matcher.group(3));
                String categoriesStr = cleanAndNormalize(matcher.group(4));
                String startDateStr = matcher.group(5).trim();
                String endDateStr = matcher.group(6);
                String description = matcher.group(7);

                List<String> categories = parseCategories(categoriesStr);
                LocalDate startDate = parseDate(startDateStr);
                LocalDate endDate = endDateStr != null ? parseDate(endDateStr.trim()) : null;

                if (description == null || description.trim().isEmpty()) {
                    description = generateDefaultDescription(logType, personName);
                }

                return new BatchLogEntry(squadName, personName, logType, categories, 
                                       description.trim(), startDate, endDate, lineNumber);
                                       
            } catch (Exception e) {
                log.error("Error parsing structured format for line {}: {}", lineNumber, e.getMessage());
                return null;
            }
        }
        
        return null;
    }

    private BatchLogEntry tryNaturalFormat(String line, int lineNumber) {
        Matcher matcher = NATURAL_PATTERN.matcher(line);
        
        if (matcher.find()) {
            try {
                String personName = cleanAndNormalize(matcher.group(1));
                String squadName = cleanAndNormalize(matcher.group(2));
                String logType = cleanAndNormalize(matcher.group(3));
                String categoriesStr = cleanAndNormalize(matcher.group(4));
                String startDateStr = normalizeDate(matcher.group(5));
                String endDateStr = matcher.group(6) != null ? normalizeDate(matcher.group(6)) : null;

                List<String> categories = parseCategories(categoriesStr);
                LocalDate startDate = parseDate(startDateStr);
                LocalDate endDate = endDateStr != null ? parseDate(endDateStr) : null;

                String description = generateDefaultDescription(logType, personName);

                return new BatchLogEntry(squadName, personName, logType, categories, 
                                       description, startDate, endDate, lineNumber);
                                       
            } catch (Exception e) {
                log.error("Error parsing natural format for line {}: {}", lineNumber, e.getMessage());
                return null;
            }
        }
        
        return null;
    }

    private BatchLogEntry tryFlexibleParsing(String line, int lineNumber) {
        String[] parts = line.split("-");
        
        if (parts.length >= 5) {
            try {
                String squadName = cleanAndNormalize(parts[0]);
                String personName = cleanAndNormalize(parts[1]);
                String logType = cleanAndNormalize(parts[2]);
                String categoriesStr = cleanAndNormalize(parts[3]);
                String startDateStr = parts[4].trim();
                
                Pattern datePattern = Pattern.compile("\\d{2}[-/]\\d{2}[-/]\\d{4}");
                
                if (datePattern.matcher(startDateStr).matches()) {
                    LocalDate startDate = parseDate(normalizeDate(startDateStr));
                    LocalDate endDate = null;
                    String description;
                    
                    if (parts.length >= 6) {
                        String potentialEndDate = parts[5].trim();
                        if (datePattern.matcher(potentialEndDate).matches()) {
                            endDate = parseDate(normalizeDate(potentialEndDate));
                            description = parts.length > 6 ? parts[6].trim() : generateDefaultDescription(logType, personName);
                        } else {
                            description = potentialEndDate;
                        }
                    } else {
                        description = generateDefaultDescription(logType, personName);
                    }
                    
                    List<String> categories = parseCategories(categoriesStr);
                    
                    return new BatchLogEntry(squadName, personName, logType, categories, 
                                           description, startDate, endDate, lineNumber);
                }
                
            } catch (Exception e) {
                log.error("Error in flexible parsing for line {}: {}", lineNumber, e.getMessage());
            }
        }
        
        return null;
    }

    private String cleanAndNormalize(String text) {
        if (text == null) return "";
        
        String normalized = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
                                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        
        String[] words = normalized.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (word.length() > 0) {
                if (result.length() > 0) result.append(" ");
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1));
            }
        }
        
        return result.toString();
    }

    private String normalizeDate(String dateStr) {
        if (dateStr == null) return null;
        
        return dateStr.replace("/", "-");
    }

    private List<String> parseCategories(String categoriesStr) {
        List<String> categories = new ArrayList<>();
        
        if (categoriesStr == null || categoriesStr.trim().isEmpty()) {
            return categories;
        }
        
        String[] separators = {",", ";", " e ", " and ", " & "};
        String normalizedStr = categoriesStr;
        
        for (String separator : separators) {
            normalizedStr = normalizedStr.replace(separator, ",");
        }
        
        String[] parts = normalizedStr.split(",");
        
        for (String part : parts) {
            String category = cleanAndNormalize(part);
            if (!category.isEmpty()) {
                categories.add(category);
            }
        }
        
        return categories;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            return LocalDate.parse(dateStr.trim(), BRAZILIAN_DATE_FORMAT);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse date '{}': {}", dateStr, e.getMessage());
            return null;
        }
    }

    private String generateDefaultDescription(String logType, String personName) {
        return String.format("Log de %s para %s", logType, personName);
    }

    @Override
    public boolean canParse(String inputText) {
        if (inputText == null || inputText.trim().isEmpty()) {
            return false;
        }
        
        String[] lines = inputText.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            
            if (INTELLIGENT_PATTERN.matcher(line).find() || 
                NATURAL_PATTERN.matcher(line).find() ||
                line.split("-").length >= 5) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public String getParserName() {
        return "Intelligent Text Parser with Fuzzy Matching";
    }

    public double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        
        String str1 = cleanAndNormalize(s1).toLowerCase();
        String str2 = cleanAndNormalize(s2).toLowerCase();
        
        if (str1.equals(str2)) return 1.0;
        if (str1.contains(str2) || str2.contains(str1)) return 0.8;
        
        int maxLength = Math.max(str1.length(), str2.length());
        if (maxLength == 0) return 1.0;
        
        return (maxLength - levenshteinDistance(str1, str2)) / (double) maxLength;
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
}
