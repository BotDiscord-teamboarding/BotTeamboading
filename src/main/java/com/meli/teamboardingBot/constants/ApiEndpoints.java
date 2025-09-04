package com.meli.teamboardingBot.constants;
public final class ApiEndpoints {
    private ApiEndpoints() {}
    public static final String SQUAD_LIST = "/clients/squads?only_active_squads=true&limit=50&offset=0";
    public static final String SQUAD_LOG = "/clients/squad_logs";
    public static final String SQUAD_LOG_TYPES = "/clients/squad_log_types/all";
    public static final String SQUAD_CATEGORIES = "/clients/skill_categories";
    public static final String SQUAD_LOG_LIST_ALL = "/clients/squad_logs?offset=0&q=&client_id=67&area_id=67&project_id=35&squad_id=232&only_active_squads=true&limit=15";
    public static final String SQUAD_LOG_BY_ID = "/clients/squad_logs/";
}
