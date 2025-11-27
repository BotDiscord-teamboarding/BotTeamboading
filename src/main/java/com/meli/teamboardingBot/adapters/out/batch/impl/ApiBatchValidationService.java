package com.meli.teamboardingBot.adapters.out.batch.impl;

import com.meli.teamboardingBot.adapters.out.batch.BatchValidator;
import com.meli.teamboardingBot.core.domain.batch.BatchLogEntry;
import com.meli.teamboardingBot.core.domain.batch.BatchParsingResult;
import com.meli.teamboardingBot.adapters.out.language.SquadLogService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ApiBatchValidationService implements BatchValidator {
    
    private final SquadLogService squadLogService;
    private final Map<String, Long> squadCache = new HashMap<>();
    private final Map<String, Long> typeCache = new HashMap<>();
    private final Map<String, Long> categoryCache = new HashMap<>();
    private final Map<Long, Map<String, Long>> userCache = new HashMap<>();

    public ApiBatchValidationService(SquadLogService squadLogService) {
        this.squadLogService = squadLogService;
    }

    @Override
    public BatchParsingResult validateEntries(List<BatchLogEntry> entries) {
        List<BatchLogEntry> validEntries = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        loadCacheData();

        for (BatchLogEntry entry : entries) {
            List<String> entryErrors = validateSingleEntry(entry);
            
            if (entryErrors.isEmpty()) {
                validEntries.add(entry);
            } else {
                errors.addAll(entryErrors);
            }
        }

        return new BatchParsingResult(validEntries, errors, entries.size());
    }

    private List<String> validateSingleEntry(BatchLogEntry entry) {
        List<String> errors = new ArrayList<>();

        if (!validateAndSetSquad(entry)) {
            errors.add(String.format("Linha %d: Squad '%s' não encontrada", 
                entry.getLineNumber(), entry.getSquadName()));
        }

        if (entry.getSquadId() != null && !validateAndSetUser(entry)) {
            errors.add(String.format("Linha %d: Pessoa '%s' não encontrada na squad '%s'", 
                entry.getLineNumber(), entry.getPersonName(), entry.getSquadName()));
        }

        if (!validateAndSetType(entry)) {
            errors.add(String.format("Linha %d: Tipo '%s' não encontrado", 
                entry.getLineNumber(), entry.getLogType()));
        }

        if (!validateAndSetCategories(entry)) {
            errors.add(String.format("Linha %d: Uma ou mais categorias não foram encontradas: %s", 
                entry.getLineNumber(), String.join(", ", entry.getCategories())));
        }

        if (entry.getStartDate() == null) {
            errors.add(String.format("Linha %d: Data de início inválida", entry.getLineNumber()));
        }

        updateDefaultDescriptionIfNeeded(entry);

        return errors;
    }

    private boolean validateAndSetSquad(BatchLogEntry entry) {
        String squadName = entry.getSquadName();
        String bestMatch = findBestMatch(squadName, squadCache);
        
        if (bestMatch != null) {
            Long squadId = squadCache.get(bestMatch);
            entry.setSquadId(squadId);
            entry.setSquadName(bestMatch);
            return true;
        }
        
        return false;
    }

    private boolean validateAndSetUser(BatchLogEntry entry) {
        Long squadId = entry.getSquadId();
        String userName = entry.getPersonName();
        
        Map<String, Long> squadUsers = userCache.get(squadId);
        if (squadUsers != null) {
            String bestMatch = findBestUserMatch(userName, squadUsers);
            if (bestMatch != null) {
                Long userId = squadUsers.get(bestMatch);
                System.out.println("[DEBUG] UPDATING USER: '" + userName + "' -> '" + bestMatch + "' (ID: " + userId + ")");
                entry.setUserId(userId);
                entry.setPersonName(bestMatch);
                System.out.println("[DEBUG] AFTER UPDATE: entry.getPersonName() = '" + entry.getPersonName() + "'");
                return true;
            }
        }
        
        return false;
    }

    private boolean validateAndSetType(BatchLogEntry entry) {
        String typeName = entry.getLogType();
        String bestMatch = findBestMatch(typeName, typeCache);
        
        if (bestMatch != null) {
            Long typeId = typeCache.get(bestMatch);
            entry.setTypeId(typeId);
            entry.setLogType(bestMatch);
            return true;
        }
        
        return false;
    }

    private boolean validateAndSetCategories(BatchLogEntry entry) {
        List<Long> categoryIds = new ArrayList<>();
        List<String> correctedCategories = new ArrayList<>();
        
        for (String categoryName : entry.getCategories()) {
            String bestMatch = findBestMatch(categoryName, categoryCache);
            
            if (bestMatch != null) {
                Long categoryId = categoryCache.get(bestMatch);
                categoryIds.add(categoryId);
                correctedCategories.add(bestMatch);
            } else {
                return false;
            }
        }
        
        entry.setCategoryIds(categoryIds);
        entry.setCategories(correctedCategories);
        return true;
    }

    private void loadCacheData() {
        loadSquadsCache();
        loadTypesCache();
        loadCategoriesCache();
        loadUsersCache();
    }

    private void loadSquadsCache() {
        try {
            String squadsResponse = squadLogService.getSquads();
            System.out.println("[DEBUG] Squads Response: " + squadsResponse);
            
            JSONObject obj = new JSONObject(squadsResponse);
            JSONArray squadsArray = obj.optJSONArray("items");
            
            if (squadsArray != null) {
                System.out.println("[DEBUG] Found " + squadsArray.length() + " squads");
                for (int i = 0; i < squadsArray.length(); i++) {
                    JSONObject squad = squadsArray.getJSONObject(i);
                    String name = squad.getString("name");
                    Long id = squad.getLong("id");
                    squadCache.put(name, id);
                    System.out.println("[DEBUG] Loaded squad: " + name + " (ID: " + id + ")");
                }
            } else {
                System.out.println("[DEBUG] No 'items' array found in squads response");
            }
        } catch (RuntimeException e) {
            System.out.println("[DEBUG] Error loading squads: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.out.println("[DEBUG] Error loading squads: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadTypesCache() {
        try {
            String typesResponse = squadLogService.getSquadLogTypes();
            JSONArray typesArray = new JSONArray(typesResponse);
            
            for (int i = 0; i < typesArray.length(); i++) {
                JSONObject type = typesArray.getJSONObject(i);
                String name = type.getString("name");
                Long id = type.getLong("id");
                typeCache.put(name, id);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
        }
    }

    private void loadCategoriesCache() {
        try {
            String categoriesResponse = squadLogService.getSquadCategories();
            JSONArray categoriesArray = new JSONArray(categoriesResponse);
            
            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject category = categoriesArray.getJSONObject(i);
                String name = category.getString("name");
                Long id = category.getLong("id");
                categoryCache.put(name, id);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
        }
    }

    private void loadUsersCache() {
        try {
            String squadsResponse = squadLogService.getSquads();
            JSONObject obj = new JSONObject(squadsResponse);
            JSONArray squadsArray = obj.optJSONArray("items");
            
            if (squadsArray != null) {
                for (int i = 0; i < squadsArray.length(); i++) {
                    JSONObject squad = squadsArray.getJSONObject(i);
                    Long squadId = squad.getLong("id");
                    JSONArray userSquads = squad.optJSONArray("user_squads");
                    
                    Map<String, Long> squadUsers = new HashMap<>();
                    
                    if (userSquads != null) {
                        System.out.println("[DEBUG] Loading users for squad " + squadId + ": " + userSquads.length() + " users");
                        
                        for (int j = 0; j < userSquads.length(); j++) {
                            JSONObject userSquad = userSquads.getJSONObject(j);
                            JSONObject user = userSquad.getJSONObject("user");
                            
                            String firstName = user.optString("first_name", "");
                            String lastName = user.optString("last_name", "");
                            Long userId = user.getLong("id");
                            
                            if (!firstName.trim().isEmpty()) {
                                squadUsers.put(firstName, userId);
                                System.out.println("[DEBUG] Added user: " + firstName + " (ID: " + userId + ")");
                            }
                            if (!lastName.trim().isEmpty()) {
                                squadUsers.put(lastName, userId);
                                System.out.println("[DEBUG] Added user: " + lastName + " (ID: " + userId + ")");
                            }
                            if (!firstName.trim().isEmpty() && !lastName.trim().isEmpty()) {
                                String fullName = firstName + " " + lastName;
                                squadUsers.put(fullName, userId);
                                System.out.println("[DEBUG] Added user: " + fullName + " (ID: " + userId + ")");
                            }
                        }
                        
                        squadUsers.put("All team", 0L);
                        squadUsers.put("all team", 0L);
                        System.out.println("[DEBUG] Added 'All team' option");
                    }
                    
                    userCache.put(squadId, squadUsers);
                }
            }
        } catch (RuntimeException e) {
            System.out.println("[DEBUG] Error loading users: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.out.println("[DEBUG] Error loading users: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean isValidSquad(String squadName) {
        return squadCache.containsKey(squadName.toLowerCase().trim());
    }

    @Override
    public boolean isValidUser(String userName, Long squadId) {
        Map<String, Long> squadUsers = userCache.get(squadId);
        return squadUsers != null && squadUsers.containsKey(userName.toLowerCase().trim());
    }

    @Override
    public boolean isValidType(String typeName) {
        return typeCache.containsKey(typeName.toLowerCase().trim());
    }

    @Override
    public boolean isValidCategory(String categoryName) {
        return categoryCache.containsKey(categoryName.toLowerCase().trim());
    }

    private String normalizeText(String text) {
        if (text == null) return "";
        
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        normalized = normalized.toLowerCase().trim();
        normalized = normalized.replaceAll("\\s+", " ");
        
        return normalized;
    }

    private String findBestMatch(String input, Map<String, ?> cache) {
        String normalizedInput = normalizeText(input);
        
        String exactMatch = cache.keySet().stream()
                .filter(key -> normalizeText(key).equals(normalizedInput))
                .findFirst()
                .orElse(null);
        
        if (exactMatch != null) {
            return exactMatch;
        }
        
        return cache.keySet().stream()
                .filter(key -> {
                    String normalizedKey = normalizeText(key);
                    return normalizedKey.contains(normalizedInput) || normalizedInput.contains(normalizedKey);
                })
                .min((a, b) -> {
                    String normalizedA = normalizeText(a);
                    String normalizedB = normalizeText(b);
                    int distanceA = calculateLevenshteinDistance(normalizedInput, normalizedA);
                    int distanceB = calculateLevenshteinDistance(normalizedInput, normalizedB);
                    return Integer.compare(distanceA, distanceB);
                })
                .orElse(null);
    }

    private String findBestUserMatch(String input, Map<String, Long> userMap) {
        String normalizedInput = normalizeText(input);
        System.out.println("[DEBUG] Looking for user: '" + input + "' (normalized: '" + normalizedInput + "')");
        
        List<String> allMatches = userMap.keySet().stream()
                .filter(key -> {
                    String normalizedKey = normalizeText(key);
                    
                    if (normalizedKey.equals(normalizedInput)) {
                        return true;
                    }
                    
                    String[] keyParts = normalizedKey.split("\\s+");
                    for (String part : keyParts) {
                        if (part.equals(normalizedInput) || part.startsWith(normalizedInput)) {
                            return true;
                        }
                    }
                    
                    return false;
                })
                .collect(java.util.stream.Collectors.toList());
        
        if (!allMatches.isEmpty()) {
            String bestMatch = selectBestFullName(allMatches, userMap);
            System.out.println("[DEBUG] Found matches: " + allMatches + " -> Selected: " + bestMatch);
            return bestMatch;
        }
        
        String fuzzyMatch = userMap.keySet().stream()
                .filter(key -> {
                    String normalizedKey = normalizeText(key);
                    return normalizedKey.contains(normalizedInput) || normalizedInput.contains(normalizedKey);
                })
                .min((a, b) -> {
                    String normalizedA = normalizeText(a);
                    String normalizedB = normalizeText(b);
                    int distanceA = calculateLevenshteinDistance(normalizedInput, normalizedA);
                    int distanceB = calculateLevenshteinDistance(normalizedInput, normalizedB);
                    return Integer.compare(distanceA, distanceB);
                })
                .orElse(null);
        
        if (fuzzyMatch != null) {
            System.out.println("[DEBUG] Found fuzzy match: " + fuzzyMatch);
        } else {
            System.out.println("[DEBUG] No match found for: " + input);
            System.out.println("[DEBUG] Available users: " + userMap.keySet());
        }
        
        return fuzzyMatch;
    }

    private String selectBestFullName(List<String> matches, Map<String, Long> userMap) {
        if (matches.isEmpty()) return null;
        if (matches.size() == 1) return matches.get(0);
        
        Map<Long, List<String>> groupedByUserId = new java.util.HashMap<>();
        for (String match : matches) {
            Long userId = userMap.get(match);
            groupedByUserId.computeIfAbsent(userId, k -> new java.util.ArrayList<>()).add(match);
        }
        
        String bestMatch = null;
        int maxWords = 0;
        int maxLength = 0;
        
        for (List<String> userNames : groupedByUserId.values()) {
            String bestForUser = userNames.stream()
                    .sorted((a, b) -> {
                        int wordsA = a.split("\\s+").length;
                        int wordsB = b.split("\\s+").length;
                        if (wordsA != wordsB) {
                            return Integer.compare(wordsB, wordsA);
                        }
                        return Integer.compare(b.length(), a.length());
                    })
                    .findFirst()
                    .orElse(null);
            
            if (bestForUser != null) {
                int words = bestForUser.split("\\s+").length;
                int length = bestForUser.length();
                
                if (words > maxWords || (words == maxWords && length > maxLength)) {
                    bestMatch = bestForUser;
                    maxWords = words;
                    maxLength = length;
                }
            }
        }
        
        System.out.println("[DEBUG] Selected best full name from " + matches + " -> " + bestMatch);
        return bestMatch != null ? bestMatch : matches.get(0);
    }

    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }

    private void updateDefaultDescriptionIfNeeded(BatchLogEntry entry) {
        String description = entry.getDescription();
        
        if (description != null && description.matches("Log de .+ para .+")) {
            String newDescription = String.format("Log de %s para %s", entry.getLogType(), entry.getPersonName());
            entry.setDescription(newDescription);
            System.out.println("[DEBUG] Updated description: '" + description + "' -> '" + newDescription + "'");
        }
    }
}
