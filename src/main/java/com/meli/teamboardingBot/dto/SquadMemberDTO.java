package com.meli.teamboardingBot.dto;
import java.time.LocalDateTime;
public class SquadMemberDTO {
    private SquadUserDTO user;
    private String roleId;
    private RoleDTO role;
    private Long buddyUserId;
    private LocalDateTime newHireInOrientationModeUntil;
    private String certificateAssetId;
    private String certificateCid;
    private String certificateState;
    public SquadMemberDTO() {}
    public SquadUserDTO getUser() { return user; }
    public void setUser(SquadUserDTO user) { this.user = user; }
    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }
    public RoleDTO getRole() { return role; }
    public void setRole(RoleDTO role) { this.role = role; }
    public Long getBuddyUserId() { return buddyUserId; }
    public void setBuddyUserId(Long buddyUserId) { this.buddyUserId = buddyUserId; }
    public LocalDateTime getNewHireInOrientationModeUntil() { return newHireInOrientationModeUntil; }
    public void setNewHireInOrientationModeUntil(LocalDateTime newHireInOrientationModeUntil) { this.newHireInOrientationModeUntil = newHireInOrientationModeUntil; }
    public String getCertificateAssetId() { return certificateAssetId; }
    public void setCertificateAssetId(String certificateAssetId) { this.certificateAssetId = certificateAssetId; }
    public String getCertificateCid() { return certificateCid; }
    public void setCertificateCid(String certificateCid) { this.certificateCid = certificateCid; }
    public String getCertificateState() { return certificateState; }
    public void setCertificateState(String certificateState) { this.certificateState = certificateState; }
    @Override
    public String toString() {
        return "SquadMemberDTO{" +
                "user=" + user +
                ", roleId='" + roleId + '\'' +
                ", role=" + role +
                ", buddyUserId=" + buddyUserId +
                ", newHireInOrientationModeUntil=" + newHireInOrientationModeUntil +
                ", certificateAssetId='" + certificateAssetId + '\'' +
                ", certificateCid='" + certificateCid + '\'' +
                ", certificateState='" + certificateState + '\'' +
                '}';
    }
}
