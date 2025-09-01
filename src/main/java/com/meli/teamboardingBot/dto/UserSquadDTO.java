package com.meli.teamboardingBot.dto;

import java.time.LocalDateTime;

public class UserSquadDTO {
    
    private Long userId;
    private Long squadId;
    private String roleId;
    private LocalDateTime addedOn;
    private Long buddyUserId;
    private String certificateAssetId;
    private String certificateCid;
    private String certificateState;
    private LocalDateTime newHireInOrientationModeUntil;
    
    public UserSquadDTO() {}
    
    public UserSquadDTO(Long userId, Long squadId, String roleId, LocalDateTime addedOn, Long buddyUserId, String certificateAssetId, String certificateCid, String certificateState, LocalDateTime newHireInOrientationModeUntil) {
        this.userId = userId;
        this.squadId = squadId;
        this.roleId = roleId;
        this.addedOn = addedOn;
        this.buddyUserId = buddyUserId;
        this.certificateAssetId = certificateAssetId;
        this.certificateCid = certificateCid;
        this.certificateState = certificateState;
        this.newHireInOrientationModeUntil = newHireInOrientationModeUntil;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getSquadId() {
        return squadId;
    }
    
    public void setSquadId(Long squadId) {
        this.squadId = squadId;
    }
    
    public String getRoleId() {
        return roleId;
    }
    
    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }
    
    public LocalDateTime getAddedOn() {
        return addedOn;
    }
    
    public void setAddedOn(LocalDateTime addedOn) {
        this.addedOn = addedOn;
    }
    
    public Long getBuddyUserId() {
        return buddyUserId;
    }
    
    public void setBuddyUserId(Long buddyUserId) {
        this.buddyUserId = buddyUserId;
    }
    
    public String getCertificateAssetId() {
        return certificateAssetId;
    }
    
    public void setCertificateAssetId(String certificateAssetId) {
        this.certificateAssetId = certificateAssetId;
    }
    
    public String getCertificateCid() {
        return certificateCid;
    }
    
    public void setCertificateCid(String certificateCid) {
        this.certificateCid = certificateCid;
    }
    
    public String getCertificateState() {
        return certificateState;
    }
    
    public void setCertificateState(String certificateState) {
        this.certificateState = certificateState;
    }
    
    public LocalDateTime getNewHireInOrientationModeUntil() {
        return newHireInOrientationModeUntil;
    }
    
    public void setNewHireInOrientationModeUntil(LocalDateTime newHireInOrientationModeUntil) {
        this.newHireInOrientationModeUntil = newHireInOrientationModeUntil;
    }
    
    @Override
    public String toString() {
        return "UserSquadDTO{" +
                "userId=" + userId +
                ", squadId=" + squadId +
                ", roleId='" + roleId + '\'' +
                ", addedOn=" + addedOn +
                ", buddyUserId=" + buddyUserId +
                ", certificateAssetId='" + certificateAssetId + '\'' +
                ", certificateCid='" + certificateCid + '\'' +
                ", certificateState='" + certificateState + '\'' +
                ", newHireInOrientationModeUntil=" + newHireInOrientationModeUntil +
                '}';
    }

}
